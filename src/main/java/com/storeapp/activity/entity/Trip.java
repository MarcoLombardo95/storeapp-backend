package com.storeapp.activity.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

/**
 * Trip - Travel activity with origin and destination
 * 
 * Examples:
 * - Flight from Milan to Paris
 * - Train journey from Rome to Florence
 * - Bus trip
 * - Car drive
 * - Ferry ride
 * 
 * Uses embedded Location for both origin and destination
 * Location provider configured globally in application.properties
 */
@Entity
@DiscriminatorValue("TRIP")
public class Trip extends Activity {

    // Origin location with trip_ prefix for SINGLE_TABLE
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "name", column = @Column(name = "trip_origin_name")),
        @AttributeOverride(name = "address", column = @Column(name = "trip_origin_address")),
        @AttributeOverride(name = "latitude", column = @Column(name = "trip_origin_latitude")),
        @AttributeOverride(name = "longitude", column = @Column(name = "trip_origin_longitude")),
        @AttributeOverride(name = "placeId", column = @Column(name = "trip_origin_place_id")),
        @AttributeOverride(name = "metadata", column = @Column(name = "trip_origin_metadata"))
    })
    public Location origin;

    // Destination location with trip_ prefix for SINGLE_TABLE
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "name", column = @Column(name = "trip_destination_name")),
        @AttributeOverride(name = "address", column = @Column(name = "trip_destination_address")),
        @AttributeOverride(name = "latitude", column = @Column(name = "trip_destination_latitude")),
        @AttributeOverride(name = "longitude", column = @Column(name = "trip_destination_longitude")),
        @AttributeOverride(name = "placeId", column = @Column(name = "trip_destination_place_id")),
        @AttributeOverride(name = "metadata", column = @Column(name = "trip_destination_metadata"))
    })
    public Location destination;

    // Trip-specific fields with trip_ prefix for SINGLE_TABLE
    @Enumerated(EnumType.STRING)
    @Column(name = "trip_transport_mode", nullable = false, length = 50)
    public TransportMode transportMode = TransportMode.OTHER;

    @Size(max = 255)
    @Column(name = "trip_booking_reference", length = 255)
    public String bookingReference;

    // Constructors
    public Trip() {
        this.origin = new Location();
        this.destination = new Location();
    }

    public Trip(String name, Location origin, Location destination, TransportMode transportMode) {
        this.name = name;
        this.origin = origin != null ? origin : new Location();
        this.destination = destination != null ? destination : new Location();
        this.transportMode = transportMode != null ? transportMode : TransportMode.OTHER;
    }

    @Override
    public String getActivityType() {
        return "TRIP";
    }

    /**
     * Get route description (origin → destination)
     */
    public String getRoute() {
        String originName = origin != null ? origin.getDisplayName() : "Unknown";
        String destName = destination != null ? destination.getDisplayName() : "Unknown";
        return String.format("%s → %s", originName, destName);
    }

    /**
     * Check if trip has valid origin coordinates
     */
    public boolean hasValidOrigin() {
        return origin != null && origin.hasCoordinates();
    }

    /**
     * Check if trip has valid destination coordinates
     */
    public boolean hasValidDestination() {
        return destination != null && destination.hasCoordinates();
    }

    /**
     * Check if trip has both valid locations
     */
    public boolean hasValidRoute() {
        return hasValidOrigin() && hasValidDestination();
    }

    // Semantic aliases for Trip (departure/arrival instead of start/end)
    // These map to the inherited startDate/endDate/startTime/endTime fields
    
    public java.time.LocalDate getDepartureDate() { return this.startDate; }
    public void setDepartureDate(java.time.LocalDate d) { this.startDate = d; }

    public java.time.LocalDate getArrivalDate() { return this.endDate; }
    public void setArrivalDate(java.time.LocalDate d) { this.endDate = d; }

    public java.time.LocalTime getDepartureTime() { return this.startTime; }
    public void setDepartureTime(java.time.LocalTime t) { this.startTime = t; }

    public java.time.LocalTime getArrivalTime() { return this.endTime; }
    public void setArrivalTime(java.time.LocalTime t) { this.endTime = t; }

    @Override
    public String toString() {
        return String.format("Trip{id=%d, name='%s', mode=%s, route='%s'}", 
            id, name, transportMode, getRoute());
    }
}
