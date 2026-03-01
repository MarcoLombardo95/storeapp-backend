package com.storeapp.activity.service;

import com.storeapp.group.entity.Group;
import com.storeapp.group.entity.GroupMember;
import com.storeapp.group.repository.GroupRepository;
import com.storeapp.group.repository.GroupMemberRepository;
import com.storeapp.activity.dto.*;
import com.storeapp.activity.entity.*;
import com.storeapp.activity.exception.ActivityNotFoundException;
import com.storeapp.activity.exception.InvalidExpenseSplitException;
import com.storeapp.activity.mapper.*;
import com.storeapp.activity.repository.ActivityRepository;
import com.storeapp.activity.repository.ActivityParticipantRepository;
import com.storeapp.activity.repository.ActivityExpenseRepository;
import com.storeapp.activity.repository.ActivityExpenseSplitRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.util.List;

@ApplicationScoped
public class ActivityService {

    @Inject
    ActivityRepository activityRepository;

    @Inject
    GroupRepository groupRepository;

    @Inject
    ActivityParticipantRepository participantRepository;

    @Inject
    ActivityExpenseRepository expenseRepository;

    @Inject
    ActivityExpenseSplitRepository expenseSplitRepository;

    @Inject
    GroupMemberRepository groupMemberRepository;

    @Inject
    ActivityMapper activityMapper;

    @Inject
    ActivityParticipantMapper participantMapper;

    @Inject
    ActivityExpenseMapper expenseMapper;

    @Inject
    ActivityExpenseSplitMapper expenseSplitMapper;

    @Inject
    EventMapper eventMapper;

    @Inject
    TripMapper tripMapper;

    /**
     * Create an Event activity
     */
    @Transactional
    public EventDto createEvent(Long groupId, EventRequest request, Long userId) {
        Group group = groupRepository.findByIdWithMembers(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found: " + groupId));

        if (!group.isMember(userId)) {
            throw new RuntimeException("User is not a member of this group");
        }

        Event event = eventMapper.toEntity(request);
        event.group = group;
        activityRepository.persist(event);
        
        // Add participants if provided
        if (request.participantIds != null && !request.participantIds.isEmpty()) {
            addParticipantsToActivity(event, request.participantIds, group);
        }
        
        return (EventDto) toTypedDto(event, true);
    }

    /**
     * Create a Trip activity
     */
    @Transactional
    public TripDto createTrip(Long groupId, TripRequest request, Long userId) {
        Group group = groupRepository.findByIdWithMembers(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found: " + groupId));

        if (!group.isMember(userId)) {
            throw new RuntimeException("User is not a member of this group");
        }

        Trip trip = tripMapper.toEntity(request);
        trip.group = group;
        activityRepository.persist(trip);
        
        // Add participants if provided
        if (request.participantIds != null && !request.participantIds.isEmpty()) {
            addParticipantsToActivity(trip, request.participantIds, group);
        }
        
        return (TripDto) toTypedDto(trip, true);
    }

    /**
     * @deprecated Use createEvent() or createTrip() instead
     */
    @Deprecated
    @Transactional
    public ActivityDto createActivity(Long groupId, ActivityRequest request, Long userId) {
        throw new UnsupportedOperationException(
            "This method is deprecated. Use createEvent() or createTrip() instead."
        );
    }

    @SuppressWarnings("deprecation")
    public ActivityDto getActivity(Long activityId, Long userId) {
        Activity activity = activityRepository.findByIdOptional(activityId)
                .orElseThrow(() -> new ActivityNotFoundException(activityId));

        if (!activity.group.isMember(userId)) {
            throw new RuntimeException("User is not a member of this group");
        }

        return activityMapper.toDto(activity);
    }

    /**
     * Returns EventDto or TripDto (with timezone and all type-specific fields)
     * depending on the concrete type. Use this instead of getActivity() whenever
     * type-specific fields (timezone, etc.) are needed.
     */
    public Object getTypedActivity(Long activityId, Long userId) {
        Activity activity = activityRepository.findByIdOptional(activityId)
                .orElseThrow(() -> new ActivityNotFoundException(activityId));

        if (!activity.group.isMember(userId)) {
            throw new RuntimeException("User is not a member of this group");
        }

        return toTypedDto(activity, true);
    }

    public Object getActivityWithDetails(Long activityId, Long userId) {
        Activity activity = activityRepository.findByIdOptional(activityId)
                .orElseThrow(() -> new ActivityNotFoundException(activityId));

        if (!activity.group.isMember(userId)) {
            throw new RuntimeException("User is not a member of this group");
        }

        List<ActivityExpense> expenses = expenseRepository.findByActivityId(activityId);

        if (activity instanceof Event event) {
            EventDto dto = (EventDto) toTypedDto(event, true);
            dto.expenses = expenseMapper.toDtoList(expenses);
            return dto;
        } else if (activity instanceof Trip trip) {
            TripDto dto = (TripDto) toTypedDto(trip, true);
            dto.expenses = expenseMapper.toDtoList(expenses);
            return dto;
        }

        // fallback — should never happen
        return activityMapper.toDto(activity);
    }

    public List<Object> getActivitiesByGroup(Long groupId, Long userId) {
        Group group = groupRepository.findByIdWithMembers(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found: " + groupId));

        if (!group.isMember(userId)) {
            throw new RuntimeException("User is not a member of this group");
        }

        return activityRepository.findByGroupId(groupId).stream()
                .map(a -> toTypedDto(a, false))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Update an Event activity
     */
    @Transactional
    public EventDto updateEvent(Long activityId, EventRequest request, Long userId) {
        Activity activity = activityRepository.findByIdOptional(activityId)
                .orElseThrow(() -> new ActivityNotFoundException(activityId));

        if (!activity.group.isMember(userId)) {
            throw new RuntimeException("User is not a member of this group");
        }

        if (!(activity instanceof Event event)) {
            throw new IllegalArgumentException("Activity " + activityId + " is not an Event");
        }

        // Update Event-specific fields using mapper
        Event updatedEvent = eventMapper.toEntity(request);
        
        // Copy fields (excluding id and group)
        event.name = updatedEvent.name;
        event.description = updatedEvent.description;
        event.startDate = updatedEvent.startDate;
        event.endDate = updatedEvent.endDate;
        event.startTime = updatedEvent.startTime;
        event.endTime = updatedEvent.endTime;
        event.location = updatedEvent.location;
        event.category = updatedEvent.category;
        event.bookingUrl = updatedEvent.bookingUrl;
        event.bookingReference = updatedEvent.bookingReference;
        event.reservationTime = updatedEvent.reservationTime;
        event.startTimezone = updatedEvent.startTimezone;
        event.endTimezone = updatedEvent.endTimezone;
        event.isCompleted = updatedEvent.isCompleted;
        event.displayOrder = updatedEvent.displayOrder;

        // Aggiorna i partecipanti se specificati nella richiesta
        if (request.participantIds != null) {
            updateActivityParticipants(event, request.participantIds, event.group);
        }

        return (EventDto) toTypedDto(event, true);
    }

    /**
     * Update a Trip activity
     */
    @Transactional
    public TripDto updateTrip(Long activityId, TripRequest request, Long userId) {
        Activity activity = activityRepository.findByIdOptional(activityId)
                .orElseThrow(() -> new ActivityNotFoundException(activityId));

        if (!activity.group.isMember(userId)) {
            throw new RuntimeException("User is not a member of this group");
        }

        if (!(activity instanceof Trip trip)) {
            throw new IllegalArgumentException("Activity " + activityId + " is not a Trip");
        }

        // Update Trip-specific fields using mapper
        Trip updatedTrip = tripMapper.toEntity(request);
        
        // Copy fields (excluding id and group)
        trip.name = updatedTrip.name;
        trip.description = updatedTrip.description;
        trip.startDate = updatedTrip.startDate;
        trip.endDate = updatedTrip.endDate;
        trip.startTime = updatedTrip.startTime;
        trip.endTime = updatedTrip.endTime;
        trip.origin = updatedTrip.origin;
        trip.destination = updatedTrip.destination;
        trip.transportMode = updatedTrip.transportMode;
        trip.bookingReference = updatedTrip.bookingReference;
        trip.startTimezone = updatedTrip.startTimezone;
        trip.endTimezone = updatedTrip.endTimezone;
        trip.isCompleted = updatedTrip.isCompleted;
        trip.displayOrder = updatedTrip.displayOrder;

        // Aggiorna i partecipanti se specificati nella richiesta
        if (request.participantIds != null) {
            updateActivityParticipants(trip, request.participantIds, trip.group);
        }

        return (TripDto) toTypedDto(trip, true);
    }

    /**
     * @deprecated Use updateEvent() or updateTrip() instead
     */
    @Deprecated
    @Transactional
    public ActivityDto updateActivity(Long activityId, ActivityRequest request, Long userId) {
        Activity activity = activityRepository.findByIdOptional(activityId)
                .orElseThrow(() -> new ActivityNotFoundException(activityId));

        if (!activity.group.isMember(userId)) {
            throw new RuntimeException("User is not a member of this group");
        }

        activity.name = request.name;
        activity.description = request.description;
        activity.startDate = request.scheduledDate;
        activity.startTime = request.startTime;
        activity.endTime = request.endTime;

        return activityMapper.toDto(activity);
    }

    @Transactional
    public void deleteActivity(Long activityId, Long userId) {
        Activity activity = activityRepository.findByIdOptional(activityId)
                .orElseThrow(() -> new ActivityNotFoundException(activityId));

        if (!activity.group.isAdmin(userId)) {
            throw new RuntimeException("Only group admins can delete activities");
        }

        activityRepository.delete(activity);
    }

    @Transactional
    public Object toggleActivityCompletion(Long activityId, Long userId) {
        Activity activity = activityRepository.findByIdOptional(activityId)
                .orElseThrow(() -> new ActivityNotFoundException(activityId));

        if (!activity.group.isMember(userId)) {
            throw new RuntimeException("User is not a member of this group");
        }

        activity.isCompleted = !activity.isCompleted;
        return toTypedDto(activity, false);
    }

    @Transactional
    public void reorderActivities(Long groupId, List<Long> activityIds, Long userId) {
        Group group = groupRepository.findByIdWithMembers(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found: " + groupId));

        if (!group.isMember(userId)) {
            throw new RuntimeException("User is not a member of this group");
        }

        for (int i = 0; i < activityIds.size(); i++) {
            Long activityId = activityIds.get(i);
            Activity activity = activityRepository.findByIdOptional(activityId)
                    .orElseThrow(() -> new ActivityNotFoundException(activityId));

            if (!activity.group.id.equals(groupId)) {
                throw new RuntimeException("Activity does not belong to this group");
            }

            activity.displayOrder = i;
        }
    }

    @Transactional
    public ActivityParticipantDto addParticipant(Long activityId, ActivityParticipantRequest request, Long userId) {
        Activity activity = activityRepository.findByIdOptional(activityId)
                .orElseThrow(() -> new ActivityNotFoundException(activityId));

        if (!activity.group.isMember(userId)) {
            throw new RuntimeException("User is not a member of this group");
        }

        GroupMember member = groupMemberRepository.findByIdOptional(request.groupMemberId)
                .orElseThrow(() -> new RuntimeException("Group member not found"));

        if (!member.group.id.equals(activity.group.id)) {
            throw new RuntimeException("Member does not belong to this group");
        }

        ActivityParticipant participant = new ActivityParticipant();
        participant.activity = activity;
        participant.groupMember = member;
        participant.status = request.status != null ? request.status : ParticipantStatus.CONFIRMED;
        participant.notes = request.notes;

        participantRepository.persist(participant);
        return participantMapper.toDto(participant);
    }

    @Transactional
    public ActivityParticipantDto updateParticipantStatus(Long participantId, ParticipantStatus status, String notes, Long userId) {
        ActivityParticipant participant = participantRepository.findByIdOptional(participantId)
                .orElseThrow(() -> new RuntimeException("Participant not found"));

        if (!participant.activity.group.isMember(userId)) {
            throw new RuntimeException("User is not a member of this group");
        }

        participant.status = status;
        if (notes != null) {
            participant.notes = notes;
        }

        return participantMapper.toDto(participant);
    }

    @Transactional
    public void removeParticipant(Long participantId, Long userId) {
        ActivityParticipant participant = participantRepository.findByIdOptional(participantId)
                .orElseThrow(() -> new RuntimeException("Participant not found"));

        if (!participant.activity.group.isMember(userId)) {
            throw new RuntimeException("User is not a member of this group");
        }

        long expenseCount = expenseRepository.count("activity.id", participant.activity.id);
        if (expenseCount > 0) {
            throw new jakarta.ws.rs.BadRequestException(
                "Cannot remove participant: activity has " + expenseCount + " expense(s). Remove all expenses first.");
        }

        participantRepository.delete(participant);
    }

    public List<ActivityParticipantDto> getParticipantsByActivity(Long activityId, Long userId) {
        Activity activity = activityRepository.findByIdOptional(activityId)
                .orElseThrow(() -> new ActivityNotFoundException(activityId));

        if (!activity.group.isMember(userId)) {
            throw new RuntimeException("User is not a member of this group");
        }

        List<ActivityParticipant> participants = participantRepository.findByActivityId(activityId);
        return participantMapper.toDtoList(participants);
    }

    @Transactional
    public ActivityExpenseDto addExpense(Long activityId, ActivityExpenseRequest request, Long userId) {
        Activity activity = activityRepository.findByIdOptional(activityId)
                .orElseThrow(() -> new ActivityNotFoundException(activityId));

        if (!activity.group.isMember(userId)) {
            throw new RuntimeException("User is not a member of this group");
        }

        // Calcola l'importo totale dalla somma dei paganti
        BigDecimal totalAmount = request.payers.stream()
                .map(p -> p.paidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Total amount must be positive");
        }

        // Primo pagante come riferimento per paid_by (retrocompatibilità DB)
        GroupMember primaryPayer = groupMemberRepository.findByIdOptional(request.payers.get(0).groupMemberId)
                .orElseThrow(() -> new RuntimeException("Payer not found"));

        if (!primaryPayer.group.id.equals(activity.group.id)) {
            throw new RuntimeException("Payer is not a member of this group");
        }

        ActivityExpense expense = new ActivityExpense();
        expense.activity = activity;
        expense.description = request.description;
        expense.amount = totalAmount;
        expense.currency = request.currency != null ? request.currency : "EUR";
        expense.paidBy = primaryPayer;

        expenseRepository.persist(expense);

        if (request.splits != null && !request.splits.isEmpty()) {
            // Valida che la somma degli splits corrisponda al totale
            BigDecimal totalSplits = request.splits.stream()
                    .map(s -> s.amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (totalSplits.compareTo(totalAmount) != 0) {
                throw new InvalidExpenseSplitException("Splits must sum to total expense amount");
            }

            // Mappa dei paganti per accesso rapido
            java.util.Map<Long, BigDecimal> payersMap = request.payers.stream()
                    .collect(java.util.stream.Collectors.toMap(
                            p -> p.groupMemberId,
                            p -> p.paidAmount
                    ));

            for (ActivityExpenseRequest.ExpenseSplitRequest splitRequest : request.splits) {
                GroupMember member = groupMemberRepository.findByIdOptional(splitRequest.groupMemberId)
                        .orElseThrow(() -> new RuntimeException("Member not found in split"));

                ActivityExpenseSplit split = new ActivityExpenseSplit();
                split.expense = expense;
                split.groupMember = member;
                split.amount = splitRequest.amount;
                split.isPayer = payersMap.containsKey(splitRequest.groupMemberId);
                split.paidAmount = split.isPayer
                        ? payersMap.get(splitRequest.groupMemberId)
                        : BigDecimal.ZERO;

                expenseSplitRepository.persist(split);
            }
        }

        // Aggiorna totalCost dell'attività
        BigDecimal newTotal = expenseRepository.getTotalByActivityId(activityId);
        activity.totalCost = newTotal != null ? newTotal : BigDecimal.ZERO;
        activityRepository.persist(activity);

        expenseRepository.getEntityManager().flush();

        ActivityExpense savedExpense = expenseRepository.findByIdOptional(expense.id)
                .orElseThrow(() -> new RuntimeException("Failed to retrieve saved expense"));

        return expenseMapper.toDto(savedExpense);
    }

    public List<ActivityExpenseDto> getExpensesByActivity(Long activityId, Long userId) {
        Activity activity = activityRepository.findByIdOptional(activityId)
                .orElseThrow(() -> new ActivityNotFoundException(activityId));

        if (!activity.group.isMember(userId)) {
            throw new RuntimeException("User is not a member of this group");
        }

        List<ActivityExpense> expenses = expenseRepository.findByActivityId(activityId);
        return expenseMapper.toDtoList(expenses);
    }

    @Transactional
    public void deleteExpense(Long expenseId, Long userId) {
        ActivityExpense expense = expenseRepository.findByIdOptional(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        if (!expense.activity.group.isMember(userId)) {
            throw new RuntimeException("User is not a member of this group");
        }

        Activity activity = expense.activity;
        expenseRepository.delete(expense);
        expenseRepository.getEntityManager().flush();

        // Ricalcola totalCost dopo la cancellazione
        BigDecimal newTotal = expenseRepository.getTotalByActivityId(activity.id);
        activity.totalCost = newTotal != null ? newTotal : BigDecimal.ZERO;
        activityRepository.persist(activity);
    }

    /**
     * Aggiorna i partecipanti di un'attività esistente.
     * Rimuove i partecipanti non più presenti e aggiunge i nuovi.
     * Grazie a orphanRemoval=true sulla collection, i rimossi vengono
     * automaticamente eliminati dal DB al flush della transazione.
     */
    private void updateActivityParticipants(Activity activity, java.util.List<Long> newParticipantIds, Group group) {
        // Blocca la modifica se l'attività ha già delle spese registrate
        long expenseCount = expenseRepository.count("activity.id", activity.id);
        if (expenseCount > 0) {
            throw new jakarta.ws.rs.BadRequestException(
                "Cannot modify participants: activity has " + expenseCount + " expense(s). Remove all expenses first.");
        }

        // Rimuovi dalla collection i partecipanti non presenti nella nuova lista
        // (orphanRemoval=true li cancella automaticamente dal DB)
        activity.participants.removeIf(existing ->
                !newParticipantIds.contains(existing.groupMember.id));

        // Aggiungi i nuovi partecipanti non ancora presenti
        java.util.Set<Long> existingMemberIds = activity.participants.stream()
                .map(p -> p.groupMember.id)
                .collect(java.util.stream.Collectors.toSet());

        for (Long memberId : newParticipantIds) {
            if (!existingMemberIds.contains(memberId)) {
                GroupMember groupMember = group.members.stream()
                        .filter(m -> m.id.equals(memberId))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                                "GroupMember " + memberId + " not found in group " + group.id));

                com.storeapp.activity.entity.ActivityParticipant participant =
                        new com.storeapp.activity.entity.ActivityParticipant();
                participant.activity = activity;
                participant.groupMember = groupMember;
                participant.status = com.storeapp.activity.entity.ParticipantStatus.CONFIRMED;
                participant.balance = java.math.BigDecimal.ZERO;
                participant.createdAt = java.time.LocalDateTime.now();
                participant.updatedAt = java.time.LocalDateTime.now();

                activity.participants.add(participant);
            }
        }
    }

    /**
     * Helper method to add participants to an activity
     */
    private void addParticipantsToActivity(Activity activity, java.util.List<Long> participantIds, Group group) {
        if (participantIds == null || participantIds.isEmpty()) {
            return;
        }

        for (Long memberId : participantIds) {
            // Find the GroupMember
            com.storeapp.group.entity.GroupMember groupMember = group.members.stream()
                    .filter(m -> m.id.equals(memberId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "GroupMember " + memberId + " not found in group " + group.id));

            // Create participant
            com.storeapp.activity.entity.ActivityParticipant participant = 
                    new com.storeapp.activity.entity.ActivityParticipant();
            participant.activity = activity;
            participant.groupMember = groupMember;
            participant.status = com.storeapp.activity.entity.ParticipantStatus.CONFIRMED;
            participant.balance = java.math.BigDecimal.ZERO;
            participant.createdAt = java.time.LocalDateTime.now();
            participant.updatedAt = java.time.LocalDateTime.now();

            // Add to activity's participants set
            activity.participants.add(participant);
        }
    }

    /**
     * Dispatch pattern: returns EventDto or TripDto based on the concrete entity type.
     * This is the single place where we decide which typed mapper to use.
     *
     * @param activity       the activity entity (must be Event or Trip)
     * @param withParticipants  if true, loads and attaches the participants list
     * @return EventDto or TripDto (never the deprecated ActivityDto)
     */
    private Object toTypedDto(Activity activity, boolean withParticipants) {
        if (activity instanceof Event event) {
            EventDto dto = eventMapper.toDto(event);
            if (withParticipants) {
                dto.participants = participantMapper.toDtoList(
                        participantRepository.findByActivityId(activity.id));
            }
            return dto;
        } else if (activity instanceof Trip trip) {
            TripDto dto = tripMapper.toDto(trip);
            if (withParticipants) {
                dto.participants = participantMapper.toDtoList(
                        participantRepository.findByActivityId(activity.id));
            }
            return dto;
        }
        // fallback — should never happen with current schema
        return activityMapper.toDto(activity);
    }
}