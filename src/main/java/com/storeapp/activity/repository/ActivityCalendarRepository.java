package com.storeapp.activity.repository;

import com.storeapp.activity.dto.ActivityCalendarDto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Repository for querying activity_calendar view
 */
@ApplicationScoped
public class ActivityCalendarRepository {

    @Inject
    EntityManager entityManager;

    /**
     * Get calendar view for a date range (filtered by user participation)
     */
    @SuppressWarnings("unchecked")
    public List<ActivityCalendarDto> findByGroupAndDateRange(Long groupId, LocalDate startDate, LocalDate endDate, Long userId) {
        String sql = """
            SELECT DISTINCT
                ac.id, ac.group_id, ac.title, ac.description,
                ac.start_time, ac.end_time,
                ac.day_of_week, ac.activity_date, ac.end_date,
                ac.location_name, ac.location_lat, ac.location_lng,
                ac.is_completed, ac.calendar_status,
                ac.confirmed_count, ac.maybe_count, ac.declined_count, ac.total_members,
                ac.creator_name, ac.creator_avatar
            FROM activity_calendar ac
            INNER JOIN activity_participants ap ON ac.id = ap.activity_id
            INNER JOIN group_members gm ON ap.group_member_id = gm.id
            WHERE ac.group_id = :groupId
            AND ac.activity_date BETWEEN :startDate AND :endDate
            AND gm.user_id = :userId
            ORDER BY ac.activity_date, ac.start_time
        """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("groupId", groupId);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        query.setParameter("userId", userId);

        List<Object[]> results = query.getResultList();

        return results.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get all activities for a group (no date filtering, filtered by user participation)
     */
    @SuppressWarnings("unchecked")
    public List<ActivityCalendarDto> findByGroup(Long groupId, Long userId) {
        String sql = """
            SELECT DISTINCT
                ac.id, ac.group_id, ac.title, ac.description,
                ac.start_time, ac.end_time,
                ac.day_of_week, ac.activity_date, ac.end_date,
                ac.location_name, ac.location_lat, ac.location_lng,
                ac.is_completed, ac.calendar_status,
                ac.confirmed_count, ac.maybe_count, ac.declined_count, ac.total_members,
                ac.creator_name, ac.creator_avatar
            FROM activity_calendar ac
            INNER JOIN activity_participants ap ON ac.id = ap.activity_id
            INNER JOIN group_members gm ON ap.group_member_id = gm.id
            WHERE ac.group_id = :groupId
            AND gm.user_id = :userId
            ORDER BY ac.activity_date, ac.start_time
        """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("groupId", groupId);
        query.setParameter("userId", userId);

        List<Object[]> results = query.getResultList();

        return results.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Map database result to ActivityCalendarDto
     */
    private ActivityCalendarDto mapToDto(Object[] row) {
        ActivityCalendarDto dto = new ActivityCalendarDto();
        
        int i = 0;
        dto.id = ((Number) row[i++]).longValue();
        dto.groupId = ((Number) row[i++]).longValue();  // CHANGED: no more itinerary_id
        dto.title = (String) row[i++];
        dto.description = (String) row[i++];
        
        // start and end are LocalDateTime in DTO
        dto.start = row[i++] != null ? ((java.sql.Time) row[i-1]).toLocalTime().atDate(LocalDate.now()) : null;
        dto.end = row[i++] != null ? ((java.sql.Time) row[i-1]).toLocalTime().atDate(LocalDate.now()) : null;
        
        // day_of_week is a number (1-7) from EXTRACT(ISODOW)
        Object dayOfWeekObj = row[i++];
        if (dayOfWeekObj != null) {
            int dayOfWeekNum = ((Number) dayOfWeekObj).intValue();
            dto.dayOfWeek = DayOfWeek.of(dayOfWeekNum);
        }
        
        dto.activityDate = row[i++] != null ? ((java.sql.Date) row[i-1]).toLocalDate() : null;
        dto.endDate = row[i++] != null ? ((java.sql.Date) row[i-1]).toLocalDate() : null;
        
        dto.locationName = (String) row[i++];
        dto.locationLat = row[i++] != null ? ((Number) row[i-1]).doubleValue() : null;
        dto.locationLng = row[i++] != null ? ((Number) row[i-1]).doubleValue() : null;
        
        dto.isCompleted = (Boolean) row[i++];
        dto.calendarStatus = (String) row[i++];
        
        dto.confirmedCount = row[i++] != null ? ((Number) row[i-1]).longValue() : 0L;
        dto.maybeCount = row[i++] != null ? ((Number) row[i-1]).longValue() : 0L;
        dto.declinedCount = row[i++] != null ? ((Number) row[i-1]).longValue() : 0L;
        dto.totalMembers = row[i++] != null ? ((Number) row[i-1]).longValue() : 0L;
        
        dto.creatorName = (String) row[i++];
        dto.creatorAvatar = (String) row[i++];
        
        return dto;
    }
}
