package com.storeapp.activity.mapper;

import com.storeapp.activity.dto.ActivityDto;
import com.storeapp.activity.entity.Activity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Base mapper for Activity hierarchy (Event / Trip)
 * 
 * TODO: This mapper needs to be updated for the new Event/Trip structure
 * For now, it's disabled to allow compilation of entities
 * Will be re-enabled after creating EventMapper and TripMapper
 */
@ApplicationScoped
public class ActivityMapper {

    @Inject
    protected ActivityParticipantMapper participantMapper;

    /**
     * Map any Activity (Event or Trip) to DTO
     * Dispatches to specific mapper based on runtime type
     */
    public ActivityDto toDto(Activity activity) {
        if (activity == null) {
            return null;
        }
        
        ActivityDto dto = new ActivityDto();
        
        // Common fields
        dto.id = activity.id;
        dto.groupId = activity.group != null ? activity.group.id : null;
        dto.activityType = activity.getActivityType();
        dto.name = activity.name;
        dto.description = activity.description;
        
        // Date/Time fields
        dto.startDate = activity.startDate;
        dto.endDate = activity.endDate;
        dto.startTime = activity.startTime;
        dto.endTime = activity.endTime;
        dto.scheduledDate = activity.startDate; // Backward compatibility
        
        // Status fields
        dto.isCompleted = activity.isCompleted;
        dto.displayOrder = activity.displayOrder;
        dto.createdAt = activity.createdAt;
        dto.updatedAt = activity.updatedAt;
        
        // Participant counts
        dto.confirmedCount = activity.getConfirmedCount();
        dto.maybeCount = activity.getMaybeCount();
        dto.declinedCount = activity.getDeclinedCount();
        
        // Type-specific fields - use instanceof to check runtime type
        if (activity instanceof com.storeapp.activity.entity.Event) {
            com.storeapp.activity.entity.Event event = (com.storeapp.activity.entity.Event) activity;
            dto.category = event.category != null ? event.category.name() : null;
            dto.location = event.location;
            dto.bookingUrl = event.bookingUrl;
            dto.bookingReference = event.bookingReference;
            dto.reservationTime = event.reservationTime;
        } else if (activity instanceof com.storeapp.activity.entity.Trip) {
            com.storeapp.activity.entity.Trip trip = (com.storeapp.activity.entity.Trip) activity;
            dto.transportMode = trip.transportMode != null ? trip.transportMode.name() : null;
            dto.origin = trip.origin;
            dto.destination = trip.destination;
            dto.departureTime = trip.getDepartureTime();
            dto.arrivalTime = trip.getArrivalTime();
            dto.bookingReference = trip.bookingReference;
        }
        
        return dto;
    }

    /**
     * Map list of activities (mixed Event/Trip)
     */
    public List<ActivityDto> toDtoList(List<Activity> activities) {
        if (activities == null) {
            return null;
        }
        return activities.stream()
            .map(this::toDto)
            .toList();
    }
}
