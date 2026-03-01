package com.storeapp.activity.dto;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * DTO per il calendario delle attività
 * Mappato dalla view activity_calendar
 */
public class ActivityCalendarDto {

    public Long id;
    public Long groupId;
    public String title;
    public LocalDateTime start;
    public LocalDateTime end;
    public DayOfWeek dayOfWeek;
    public LocalDate activityDate;
    public LocalDate endDate; // null se attività single-day
    public String calendarStatus; // completed, confirmed, declined, pending
    public String description;
    public String locationName;
    public Double locationLat;
    public Double locationLng;
    public Boolean isCompleted;
    public Long confirmedCount;
    public Long maybeCount;
    public Long declinedCount;
    public Long totalMembers;
    public String creatorName;
    public String creatorAvatar;
}
