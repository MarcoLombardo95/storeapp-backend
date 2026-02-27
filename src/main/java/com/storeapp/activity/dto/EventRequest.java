package com.storeapp.activity.dto;

import com.storeapp.activity.entity.EventCategory;
import com.storeapp.activity.validation.ValidDateTimeRange;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * Request DTO for creating/updating Event activities
 * Events are single-location activities (restaurants, museums, hotels, etc.)
 */
@ValidDateTimeRange
public class EventRequest {

    @NotBlank(message = "Il nome è obbligatorio")
    @Size(max = 255, message = "Il nome non può superare 255 caratteri")
    public String name;

    public String description;

    @NotNull(message = "La data di inizio è obbligatoria")
    public LocalDate startDate;

    public LocalDate endDate;

    public LocalTime startTime;

    public LocalTime endTime;

    // Location (embedded)
    @Size(max = 500, message = "Il nome della location non può superare 500 caratteri")
    public String locationName;

    @Size(max = 500, message = "L'indirizzo non può superare 500 caratteri")
    public String locationAddress;

    public BigDecimal locationLatitude;

    public BigDecimal locationLongitude;

    @Size(max = 500, message = "Il place ID non può superare 500 caratteri")
    public String locationPlaceId;

    public Map<String, Object> locationMetadata;

    // Event-specific fields
    @NotNull(message = "La categoria è obbligatoria")
    public EventCategory category = EventCategory.OTHER;

    @Size(max = 1000, message = "L'URL di prenotazione non può superare 1000 caratteri")
    public String bookingUrl;

    @Size(max = 255, message = "Il riferimento prenotazione non può superare 255 caratteri")
    public String bookingReference;

    public LocalTime reservationTime;

    // Common fields
    public Boolean isCompleted = false;

    public Integer displayOrder = 0;

    public BigDecimal totalCost;

    // Participants - IDs of GroupMembers participating in this activity (at least one required)
    @NotEmpty(message = "Almeno un partecipante è obbligatorio")
    public List<Long> participantIds;
}
