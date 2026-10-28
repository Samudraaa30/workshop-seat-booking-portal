package in.vit.workshop.service;

import in.vit.workshop.model.Booking;
import in.vit.workshop.model.PortalNotification;
import in.vit.workshop.repo.PortalNotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class NotificationService {
    public static final String ADMIN_RECIPIENT = "admin";
    private final PortalNotificationRepository notifications;

    public NotificationService(PortalNotificationRepository notifications) {
        this.notifications = notifications;
    }

    @Transactional
    public void bookingConfirmed(Booking booking) {
        String workshop = booking.getWorkshop().getTitle();
        notifications.save(new PortalNotification(booking.getAttendeeEmail(), "BOOKING_CONFIRMED",
                "Seat confirmed", "Your seat for “" + workshop + "” is confirmed."));
        notifications.save(new PortalNotification(ADMIN_RECIPIENT, "NEW_BOOKING", "New booking",
                booking.getAttendeeName() + " booked “" + workshop + "”."));
    }

    @Transactional
    public void bookingCancelled(Booking booking, boolean byAdmin) {
        String workshop = booking.getWorkshop().getTitle();
        notifications.save(new PortalNotification(booking.getAttendeeEmail(), "BOOKING_CANCELLED",
                "Booking cancelled", "Your booking for “" + workshop + "” was cancelled."));
        notifications.save(new PortalNotification(ADMIN_RECIPIENT, "BOOKING_CANCELLED",
                byAdmin ? "Booking cancelled by admin" : "Attendee cancelled a booking",
                "Booking #" + booking.getId() + " for “" + workshop + "” was cancelled."));
    }

    @Transactional(readOnly = true)
    public List<PortalNotification> forAttendee(String email) {
        if (email == null || email.isBlank()) return List.of();
        return notifications.findTop50ByRecipientEmailIgnoreCaseOrderByCreatedAtDesc(email.trim());
    }

    @Transactional(readOnly = true)
    public List<PortalNotification> forAdmin() {
        return notifications.findTop50ByRecipientEmailIgnoreCaseOrderByCreatedAtDesc(ADMIN_RECIPIENT);
    }
}
