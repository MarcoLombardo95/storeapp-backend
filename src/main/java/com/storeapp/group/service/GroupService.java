package com.storeapp.group.service;

import com.storeapp.group.dto.*;
import com.storeapp.group.entity.Group;
import com.storeapp.group.entity.GroupMember;
import com.storeapp.group.entity.GroupRole;
import com.storeapp.group.exception.*;
import com.storeapp.group.mapper.GroupMapper;
import com.storeapp.group.mapper.GroupMemberMapper;
import com.storeapp.group.repository.GroupMemberRepository;
import com.storeapp.group.repository.GroupRepository;
import com.storeapp.activity.repository.ActivityParticipantRepository;
import com.storeapp.activity.repository.ActivityExpenseSplitRepository;
import com.storeapp.user.entity.User;
import com.storeapp.user.mapper.UserMapper;
import com.storeapp.user.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.util.List;

/**
 * Service per la gestione dei gruppi
 * Le conversioni Entity <-> DTO sono delegate ai Mapper
 */
@ApplicationScoped
public class GroupService {

    @Inject
    GroupRepository groupRepository;

    @Inject
    GroupMemberRepository groupMemberRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    ActivityParticipantRepository activityParticipantRepository;

    @Inject
    ActivityExpenseSplitRepository activityExpenseSplitRepository;

    @Inject
    GroupMapper groupMapper;

    @Inject
    GroupMemberMapper groupMemberMapper;

    @Inject
    UserMapper userMapper;

    private static final int MAX_MEMBERS_PER_GROUP = 50;

    /**
     * Crea un nuovo gruppo
     * Il creatore diventa automaticamente ADMIN del gruppo
     * Se la richiesta include membri, vengono aggiunti atomicamente nella stessa transazione
     * TRANSAZIONE ATOMICA: se un membro va in errore, rollback completo (gruppo non creato)
     */
    @Transactional
    public GroupDto createGroup(CreateGroupRequest request, Long userId) {
        // 1. VALIDAZIONI PRELIMINARI (prima di persistere qualsiasi cosa)
        User creator = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("Utente non trovato"));

        // Verifica limite membri PRIMA di iniziare
        if (request.members != null && request.members.size() + 1 > MAX_MEMBERS_PER_GROUP) {
            throw new InvalidOperationException(
                "Impossibile aggiungere " + request.members.size() + " membri. " +
                "Limite massimo: " + MAX_MEMBERS_PER_GROUP + " (già incluso il creatore)"
            );
        }

        // Valida e trova TUTTI gli utenti da aggiungere PRIMA di persistere il gruppo
        List<User> usersToAdd = new java.util.ArrayList<>();
        List<GroupRole> rolesToAssign = new java.util.ArrayList<>();

        if (request.members != null && !request.members.isEmpty()) {
            for (AddMemberRequest memberRequest : request.members) {
                // Trova l'utente da aggiungere
                User userToAdd;
                if (memberRequest.email != null && !memberRequest.email.isBlank()) {
                    userToAdd = userRepository.findByEmail(memberRequest.email)
                        .orElseThrow(() -> new NotFoundException("Utente non trovato: " + memberRequest.email));
                } else if (memberRequest.username != null && !memberRequest.username.isBlank()) {
                    userToAdd = userRepository.findByName(memberRequest.username)
                        .orElseThrow(() -> new NotFoundException("Utente non trovato: " + memberRequest.username));
                } else {
                    throw new NotFoundException("Email o username richiesti per aggiungere un membro");
                }

                // Verifica che non sia il creatore (verrà aggiunto automaticamente come ADMIN)
                if (!userToAdd.getId().equals(userId)) {
                    // Verifica duplicati nella lista
                    final Long userToAddId = userToAdd.getId();
                    if (usersToAdd.stream().anyMatch(u -> u.getId().equals(userToAddId))) {
                        throw new UserAlreadyMemberException(userToAdd.getEmail());
                    }
                    usersToAdd.add(userToAdd);
                    rolesToAssign.add(memberRequest.role != null ? memberRequest.role : GroupRole.MEMBER);
                }
            }
        }

        // 2. INIZIO PERSISTENZA (tutte le validazioni sono passate)
        // Crea e persiste il gruppo
        Group group = groupMapper.toEntity(request);
        group.createdBy = creator;
        groupRepository.persist(group);
        groupRepository.flush(); // Forza il flush per ottenere l'ID

        // 3. Aggiungi il creatore come ADMIN
        GroupMember creatorMember = new GroupMember();
        creatorMember.group = group;
        creatorMember.user = creator;
        creatorMember.role = GroupRole.ADMIN;
        groupMemberRepository.persist(creatorMember);

        // 4. Aggiungi i membri validati
        for (int i = 0; i < usersToAdd.size(); i++) {
            GroupMember member = new GroupMember();
            member.group = group;
            member.user = usersToAdd.get(i);
            member.role = rolesToAssign.get(i);
            groupMemberRepository.persist(member);
        }

        groupMemberRepository.flush();

        // 5. Ricarica il gruppo con i membri per avere i dati completi
        group = groupRepository.findByIdWithMembers(group.id)
            .orElseThrow(() -> new RuntimeException("Errore nel recupero del gruppo creato"));

        return groupMapper.toDtoWithMembers(group);
    }

    /**
     * Ottiene tutti i gruppi di un utente (creati o come membro)
     */
    public List<GroupDto> getUserGroups(Long userId) {
        List<Group> groups = groupRepository.findByMember(userId);
        return groupMapper.toDtoList(groups);
    }

    /**
     * Ottiene i dettagli di un gruppo con i membri
     */
    public GroupDto getGroupDetails(Long groupId, Long userId) {
        Group group = groupRepository.findByIdWithMembers(groupId)
            .orElseThrow(() -> new GroupNotFoundException(groupId));

        // Verifica che l'utente sia membro del gruppo
        if (!group.isMember(userId)) {
            throw InsufficientPermissionsException.adminRequired();
        }

        return groupMapper.toDtoWithMembers(group);
    }

    /**
     * Aggiorna le informazioni di un gruppo (solo ADMIN)
     */
    @Transactional
    public GroupDto updateGroup(Long groupId, UpdateGroupRequest request, Long userId) {
        Group group = groupRepository.findById(groupId);
        if (group == null) {
            throw new GroupNotFoundException(groupId);
        }

        // Solo gli admin possono modificare
        if (!group.isAdmin(userId)) {
            throw InsufficientPermissionsException.adminRequired();
        }

        // Usa mapper per aggiornare entity (partial update)
        groupMapper.updateEntityFromRequest(request, group);

        groupRepository.persist(group);
        return groupMapper.toDtoWithMembers(group);
    }

    /**
     * Elimina un gruppo (solo creatore)
     */
    @Transactional
    public void deleteGroup(Long groupId, Long userId) {
        Group group = groupRepository.findById(groupId);
        if (group == null) {
            throw new GroupNotFoundException(groupId);
        }

        // Solo il creatore può eliminare
        if (!group.isCreator(userId)) {
            throw InsufficientPermissionsException.creatorRequired();
        }

        groupRepository.delete(group);
    }

    /**
     * Ottiene la lista di utenti disponibili da aggiungere al gruppo
     * Esclude gli utenti già membri del gruppo
     * 
     * @param groupId ID del gruppo
     * @param userId ID dell'utente che richiede la lista
     * @param searchQuery Query di ricerca opzionale (cerca in email e nome, case-insensitive)
     * @return Lista di utenti disponibili, filtrati per la query se presente
     */
    public List<com.storeapp.user.dto.UserResponse> getAvailableUsers(Long groupId, Long userId, String searchQuery) {
        Group group = groupRepository.findByIdWithMembers(groupId)
            .orElseThrow(() -> new GroupNotFoundException(groupId));

        // Verifica che l'utente sia almeno membro del gruppo
        if (!group.isMember(userId)) {
            throw InsufficientPermissionsException.memberRequired();
        }

        // Ottieni gli ID dei membri attuali del gruppo da escludere
        List<Long> memberIds = group.members.stream()
            .map(m -> m.user.getId())
            .toList();

        // 🚀 Query ottimizzata: tutto a livello database!
        List<User> availableUsers;
        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            // Cerca utenti per nome/email ESCLUDENDO i membri (una sola query)
            availableUsers = userRepository.searchUsersExcluding(searchQuery.trim(), memberIds);
        } else {
            // Senza ricerca, restituisce tutti gli utenti NON membri (una sola query)
            availableUsers = userRepository.findAllExcluding(memberIds);
        }

        return availableUsers.stream()
            .map(userMapper::toUserResponse)
            .toList();
    }

    /**
     * Aggiunge un membro al gruppo (solo ADMIN)
     */
    @Transactional
    public GroupMemberDto addMember(Long groupId, AddMemberRequest request, Long userId) {
        Group group = groupRepository.findByIdWithMembers(groupId)
            .orElseThrow(() -> new GroupNotFoundException(groupId));

        // Solo gli admin possono aggiungere membri
        if (!group.isAdmin(userId)) {
            throw InsufficientPermissionsException.adminRequired();
        }

        // Verifica limite membri
        if (group.getMemberCount() >= MAX_MEMBERS_PER_GROUP) {
            throw InvalidOperationException.maxMembersReached();
        }

        // Trova l'utente da aggiungere
        User userToAdd = null;
        if (request.email != null && !request.email.isBlank()) {
            userToAdd = userRepository.findByEmail(request.email).orElse(null);
        } else if (request.username != null && !request.username.isBlank()) {
            userToAdd = userRepository.findByName(request.username).orElse(null);
        }

        if (userToAdd == null) {
            throw new NotFoundException("Utente non trovato");
        }

        // Verifica che non sia già membro
        if (groupMemberRepository.isMember(groupId, userToAdd.getId())) {
            throw new UserAlreadyMemberException(userToAdd.getEmail());
        }

        // Aggiungi il membro
        GroupMember member = new GroupMember();
        member.group = group;
        member.user = userToAdd;
        member.role = request.role != null ? request.role : GroupRole.MEMBER;
        groupMemberRepository.persist(member);

        return groupMemberMapper.toDto(member);
    }

    /**
     * Aggiunge più membri al gruppo in modo atomico (solo ADMIN)
     * Se anche solo uno fallisce, viene fatto rollback di tutti gli inserimenti
     */
    @Transactional
    public List<GroupMemberDto> addMembers(Long groupId, List<AddMemberRequest> requests, Long userId) {
        // Validazione gruppo e permessi
        Group group = groupRepository.findByIdWithMembers(groupId)
            .orElseThrow(() -> new GroupNotFoundException(groupId));

        // Solo gli admin possono aggiungere membri
        if (!group.isAdmin(userId)) {
            throw InsufficientPermissionsException.adminRequired();
        }

        // Verifica limite membri PRIMA di processare
        long currentMemberCount = group.getMemberCount();
        if (currentMemberCount + requests.size() > MAX_MEMBERS_PER_GROUP) {
            throw new InvalidOperationException(
                "Impossibile aggiungere " + requests.size() + " membri. " +
                "Limite massimo: " + MAX_MEMBERS_PER_GROUP + ", attuali: " + currentMemberCount
            );
        }

        // Lista per i membri aggiunti
        List<GroupMember> addedMembers = new java.util.ArrayList<>();

        // Processa ogni richiesta
        for (AddMemberRequest request : requests) {
            // Trova l'utente da aggiungere
            User userToAdd = null;
            if (request.email != null && !request.email.isBlank()) {
                userToAdd = userRepository.findByEmail(request.email).orElse(null);
            } else if (request.username != null && !request.username.isBlank()) {
                userToAdd = userRepository.findByName(request.username).orElse(null);
            }

            if (userToAdd == null) {
                // Rollback automatico grazie a @Transactional
                throw new NotFoundException("Utente non trovato: " + 
                    (request.email != null ? request.email : request.username));
            }

            // Verifica che non sia già membro
            if (groupMemberRepository.isMember(groupId, userToAdd.getId())) {
                // Rollback automatico grazie a @Transactional
                throw new UserAlreadyMemberException(userToAdd.getEmail());
            }

            // Aggiungi il membro
            GroupMember member = new GroupMember();
            member.group = group;
            member.user = userToAdd;
            member.role = request.role != null ? request.role : GroupRole.MEMBER;
            groupMemberRepository.persist(member);
            addedMembers.add(member);
        }

        // Converti tutti i membri in DTO
        return addedMembers.stream()
            .map(groupMemberMapper::toDto)
            .toList();
    }

    /**
     * Rimuove un membro dal gruppo (solo ADMIN)
     */
    @Transactional
    public void removeMember(Long groupId, Long memberId, Long userId) {
        Group group = groupRepository.findById(groupId);
        if (group == null) {
            throw new GroupNotFoundException(groupId);
        }

        // Solo gli admin possono rimuovere membri
        if (!group.isAdmin(userId)) {
            throw InsufficientPermissionsException.adminRequired();
        }

        GroupMember memberToRemove = groupMemberRepository.findById(memberId);
        if (memberToRemove == null || !memberToRemove.group.id.equals(groupId)) {
            throw new NotFoundException("Membro non trovato in questo gruppo");
        }

        // Non puoi rimuovere te stesso (usa leaveGroup)
        if (memberToRemove.user.getId().equals(userId)) {
            throw InvalidOperationException.cannotRemoveYourself();
        }

        // Se è un admin, verifica che non sia l'ultimo
        if (memberToRemove.role == GroupRole.ADMIN) {
            long adminCount = groupMemberRepository.countAdminsByGroup(groupId);
            if (adminCount <= 1) {
                throw InvalidOperationException.lastAdminCannotLeave();
            }
        }

        // Verifica che il membro non sia coinvolto in attività o spese
        long activityCount = activityParticipantRepository.count("groupMember.id", memberId);
        long expenseCount = activityExpenseSplitRepository.count("groupMember.id", memberId);
        if (activityCount > 0 || expenseCount > 0) {
            String memberName = memberToRemove.user.getName();
            throw InvalidOperationException.memberInvolvedInActivities(memberName, activityCount, expenseCount);
        }

        long deletedCount = groupMemberRepository.removeMemberById(memberId);
        
        if (deletedCount == 0) {
            throw new RuntimeException("Failed to delete member - no rows affected");
        }
    }

    /**
     * Verifica lo stato prima di abbandonare un gruppo
     */
    public LeaveGroupStatusDto checkLeaveGroupStatus(Long groupId, Long userId) {
        GroupMember membership = groupMemberRepository.findByGroupAndUser(groupId, userId)
            .orElseThrow(() -> new NotFoundException("Non sei membro di questo gruppo"));

        // Conta il numero totale di membri e admin
        long memberCount = groupMemberRepository.countByGroup(groupId);
        long adminCount = groupMemberRepository.countAdminsByGroup(groupId);

        // Se sei l'unico membro, il gruppo verrà eliminato
        if (memberCount == 1) {
            return LeaveGroupStatusDto.willDeleteGroup(memberCount);
        }

        // Se sei admin e l'ultimo admin, non puoi uscire
        if (membership.role == GroupRole.ADMIN && adminCount <= 1) {
            return LeaveGroupStatusDto.lastAdmin(memberCount, adminCount);
        }

        // Altrimenti puoi uscire normalmente
        return LeaveGroupStatusDto.canLeave();
    }

    /**
     * Abbandona un gruppo (self-remove)
     */
    @Transactional
    public void leaveGroup(Long groupId, Long userId) {
        GroupMember membership = groupMemberRepository.findByGroupAndUser(groupId, userId)
            .orElseThrow(() -> new NotFoundException("Non sei membro di questo gruppo"));

        // Conta il numero totale di membri
        long memberCount = groupMemberRepository.countByGroup(groupId);

        // Se sei l'unico membro, elimina il gruppo
        if (memberCount == 1) {
            Group group = groupRepository.findById(groupId);
            if (group != null) {
                groupRepository.delete(group);
            }
            return;
        }

        // Se sei admin, verifica che non sei l'ultimo
        if (membership.role == GroupRole.ADMIN) {
            long adminCount = groupMemberRepository.countAdminsByGroup(groupId);
            if (adminCount <= 1) {
                throw InvalidOperationException.lastAdminCannotLeave();
            }
        }

        groupMemberRepository.delete(membership);
    }

    /**
     * Cambia il ruolo di un membro (solo ADMIN)
     */
    @Transactional
    public GroupMemberDto updateMemberRole(Long groupId, Long memberId, UpdateMemberRoleRequest request, Long userId) {
        Group group = groupRepository.findById(groupId);
        if (group == null) {
            throw new GroupNotFoundException(groupId);
        }

        // Solo gli admin possono cambiare ruoli
        if (!group.isAdmin(userId)) {
            throw InsufficientPermissionsException.adminRequired();
        }

        GroupMember member = groupMemberRepository.findById(memberId);
        if (member == null || !member.group.id.equals(groupId)) {
            throw new NotFoundException("Membro non trovato in questo gruppo");
        }

        // Se stai retrocedendo un admin, verifica che non sia l'ultimo
        if (member.role == GroupRole.ADMIN && request.role == GroupRole.MEMBER) {
            long adminCount = groupMemberRepository.countAdminsByGroup(groupId);
            if (adminCount <= 1) {
                throw InvalidOperationException.lastAdminCannotLeave();
            }
        }

        member.role = request.role;
        groupMemberRepository.persist(member);

        return groupMemberMapper.toDto(member);
    }

    /**
     * Ottiene tutti i membri di un gruppo
     */
    public List<GroupMemberDto> getGroupMembers(Long groupId, Long userId) {
        Group group = groupRepository.findById(groupId);
        if (group == null) {
            throw new GroupNotFoundException(groupId);
        }

        // Verifica che l'utente sia membro
        if (!group.isMember(userId)) {
            throw InsufficientPermissionsException.adminRequired();
        }

        List<GroupMember> members = groupMemberRepository.findByGroup(groupId);
        return groupMemberMapper.toDtoList(members);
    }
}
