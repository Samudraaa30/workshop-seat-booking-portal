package in.vit.workshop.web;

import in.vit.workshop.model.Workshop;
import java.time.LocalDateTime;

public record WorkshopAvailability(
        Long id,
        String title,
        String description,
        LocalDateTime startsAt,
        int capacity,
        int bookedSeats,
        int availableSeats) {

    public static WorkshopAvailability from(Workshop workshop) {
        return new WorkshopAvailability(workshop.getId(), workshop.getTitle(), workshop.getDescription(),
                workshop.getStartsAt(), workshop.getCapacity(), workshop.getBookedSeats(), workshop.getAvailableSeats());
    }
}
