package com.storeapp.activity.mapper;

import com.storeapp.activity.dto.TripDto;
import com.storeapp.activity.dto.TripRequest;
import com.storeapp.activity.entity.Location;
import com.storeapp.activity.entity.Trip;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Mapper for Trip entity and DTOs
 * Converts between Trip entity and TripDto/TripRequest
 */
@ApplicationScoped
public class TripMapper {

    @Inject
    LocationMapper locationMapper;

    /**
     * Convert Trip entity to TripDto
     */
    public TripDto toDto(Trip entity) {
        if (entity == null) {
            return null;
        }

        TripDto dto = new TripDto();
        dto.id = entity.id;
        dto.groupId = entity.group != null ? entity.group.id : null;
        dto.name = entity.name;
        dto.description = entity.description;
        dto.departureDate = entity.startDate;
        dto.arrivalDate = entity.endDate;
        dto.departureTime = entity.startTime;
        dto.arrivalTime = entity.endTime;
        dto.activityType = "TRIP";

        // Locations
        dto.origin = locationMapper.toDto(entity.origin);
        dto.destination = locationMapper.toDto(entity.destination);

        // Trip-specific fields
        dto.transportMode = entity.transportMode;
        dto.bookingReference = entity.bookingReference;

        // Common fields
        dto.isCompleted = entity.isCompleted;
        dto.displayOrder = entity.displayOrder;
        dto.totalCost = entity.totalCost;
        dto.createdAt = entity.createdAt;
        dto.updatedAt = entity.updatedAt;
        dto.createdBy = entity.createdBy != null ? entity.createdBy.getId() : null;

        // Participant statistics
        dto.confirmedCount = entity.getConfirmedCount();

        return dto;
    }

    /**
     * Convert TripRequest to Trip entity
     */
    public Trip toEntity(TripRequest request) {
        if (request == null) {
            return null;
        }

        Trip entity = new Trip();
        updateEntityFromRequest(request, entity);
        return entity;
    }

    /**
     * Update Trip entity from TripRequest
     */
    public void updateEntityFromRequest(TripRequest request, Trip entity) {
        if (request == null || entity == null) {
            return;
        }

        entity.name = request.name;
        entity.description = request.description;
        entity.startDate = request.departureDate;
        entity.endDate = request.arrivalDate;
        entity.startTime = request.departureTime;
        entity.endTime = request.arrivalTime;

        // Origin location
        if (entity.origin == null) {
            entity.origin = new Location();
        }
        entity.origin.name = request.originName;
        entity.origin.address = request.originAddress;
        entity.origin.latitude = request.originLatitude;
        entity.origin.longitude = request.originLongitude;
        entity.origin.placeId = request.originPlaceId;
        entity.origin.metadata = request.originMetadata;

        // Destination location
        if (entity.destination == null) {
            entity.destination = new Location();
        }
        entity.destination.name = request.destinationName;
        entity.destination.address = request.destinationAddress;
        entity.destination.latitude = request.destinationLatitude;
        entity.destination.longitude = request.destinationLongitude;
        entity.destination.placeId = request.destinationPlaceId;
        entity.destination.metadata = request.destinationMetadata;

        // Trip-specific fields
        entity.transportMode = request.transportMode;
        entity.bookingReference = request.bookingReference;

        // Common fields
        entity.isCompleted = request.isCompleted;
        entity.displayOrder = request.displayOrder;
        entity.totalCost = request.totalCost != null ? request.totalCost : java.math.BigDecimal.ZERO;
    }
}
