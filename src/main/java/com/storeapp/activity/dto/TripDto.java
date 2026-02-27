package com.storeapp.activity.dto;

import com.storeapp.activity.entity.TransportMode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Response DTO for Trip activities
 * Trips are travel activities with origin and destination (flights, trains, car trips, etc.)
 */
public class TripDto {

    public Long id;
    public Long groupId;
    public String name;
    public String description;

    // Date/time fields with Trip-semantic names
    public LocalDate departureDate;
    public LocalDate arrivalDate;
    public LocalTime departureTime;
    public LocalTime arrivalTime;

    // Activity type discriminator
    public String activityType = "TRIP";

    // Locations (embedded)
    public LocationDto origin;
    public LocationDto destination;

    // Trip-specific fields
    public TransportMode transportMode;
    public String bookingReference;

    // Common fields
    public Boolean isCompleted;
    public Integer displayOrder;
    public BigDecimal totalCost;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
    public Long createdBy;

    // Participant statistics
    public Long confirmedCount;
    public Long maybeCount;
    public Long declinedCount;

    // Relations (optional, loaded on demand)
    public List<ActivityParticipantDto> participants;
    public List<ActivityExpenseDto> expenses;

    // Helper methods
    public boolean isMultiDay() {
        return arrivalDate != null && !arrivalDate.equals(departureDate);
    }

    public int getDurationDays() {
        if (arrivalDate == null || departureDate == null) {
            return 1;
        }
        return (int) java.time.temporal.ChronoUnit.DAYS.between(departureDate, arrivalDate) + 1;
    }

    public String getRoute() {
        String originName = (origin != null) ? origin.getDisplayName() : "?";
        String destName = (destination != null) ? destination.getDisplayName() : "?";
        return originName + " → " + destName;
    }
}
