package in.vit.workshop.service;

import in.vit.workshop.model.Booking;
import in.vit.workshop.model.Workshop;
import in.vit.workshop.repo.BookingRepository;
import in.vit.workshop.repo.WorkshopRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import java.util.List;

@Service
public class BookingService {
    private final WorkshopRepository workshops;
    private final BookingRepository bookings;
    private final ApplicationEventPublisher events;
    private final NotificationService notifications;
    public BookingService(WorkshopRepository workshops, BookingRepository bookings,
                          ApplicationEventPublisher events, NotificationService notifications) {
        this.workshops = workshops; this.bookings = bookings; this.events = events;
        this.notifications = notifications;
    }
    public List<Workshop> workshops() { return workshops.findAll(); }
    public List<Booking> bookingsFor(String email) {
        if (email == null || email.isBlank()) return List.of();
        return bookings.findByAttendeeEmailIgnoreCaseOrderByCreatedAtDesc(email.trim());
    }
    @Transactional
    public Booking book(Long workshopId, String name, String email) {
        if (name == null || name.isBlank() || name.trim().length() > 100)
            throw new IllegalArgumentException("Enter a name of 1 to 100 characters.");
        if (email == null || email.isBlank() || email.trim().length() > 160 ||
                !email.trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
            throw new IllegalArgumentException("Enter a valid email address (up to 160 characters).");
        Workshop workshop = workshops.findByIdForUpdate(workshopId)
            .orElseThrow(() -> new IllegalArgumentException("Workshop not found."));
        if (workshop.getAvailableSeats() < 1) throw new IllegalStateException("Sorry, this workshop is full.");
        workshop.setBookedSeats(workshop.getBookedSeats() + 1);
        workshops.save(workshop);
        Booking booking = bookings.save(new Booking(name.trim(), email.trim().toLowerCase(), workshop));
        notifications.bookingConfirmed(booking);
        publishAvailabilityChanged();
        return booking;
    }
    @Transactional
    public void cancel(Long bookingId, String email) {
        if (email == null || email.isBlank())
            throw new IllegalArgumentException("Enter the email used for this booking.");
        Booking booking = bookings.findByIdForUpdate(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("Booking not found."));
        if (!booking.getAttendeeEmail().equalsIgnoreCase(email.trim()))
            throw new IllegalArgumentException("Enter the email used for this booking.");
        cancelConfirmedBooking(booking, false);
    }

    @Transactional
    public void cancelByAdmin(Long bookingId) {
        Booking booking = bookings.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found."));
        cancelConfirmedBooking(booking, true);
    }

    private void cancelConfirmedBooking(Booking booking, boolean byAdmin) {
        if ("CANCELLED".equals(booking.getStatus())) return;
        booking.setStatus("CANCELLED");
        Workshop workshop = booking.getWorkshop();
        workshop.setBookedSeats(Math.max(0, workshop.getBookedSeats() - 1));
        workshops.save(workshop);
        bookings.save(booking);
        notifications.bookingCancelled(booking, byAdmin);
        publishAvailabilityChanged();
    }

    private void publishAvailabilityChanged() {
        events.publishEvent(new WorkshopAvailabilityChangedEvent());
    }
}
