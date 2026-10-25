package in.vit.workshop.service;

/** Published inside a booking transaction and consumed only after it commits. */
public record WorkshopAvailabilityChangedEvent() {}
