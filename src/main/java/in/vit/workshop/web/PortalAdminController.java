package in.vit.workshop.web;

import in.vit.workshop.repo.BookingRepository;
import in.vit.workshop.service.BookingService;
import in.vit.workshop.service.NotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PortalAdminController {
    private final BookingService bookings;
    private final BookingRepository bookingRepository;
    private final NotificationService notifications;
    private final boolean adminConfigured;

    public PortalAdminController(BookingService bookings,
                                 BookingRepository bookingRepository,
                                 NotificationService notifications,
                                 @Value("${portal.admin.username:}") String username,
                                 @Value("${portal.admin.password:}") String password) {
        this.bookings = bookings;
        this.bookingRepository = bookingRepository;
        this.notifications = notifications;
        this.adminConfigured = !username.isBlank() && !password.isBlank();
    }

    @GetMapping("/admin/login")
    public String login(Model model) {
        model.addAttribute("adminConfigured", adminConfigured);
        return "admin-login";
    }

    @GetMapping("/admin")
    public String dashboard(Model model) {
        model.addAttribute("workshops", bookings.workshops());
        model.addAttribute("bookings", bookingRepository.findAllByOrderByCreatedAtDesc());
        model.addAttribute("confirmedCount", bookingRepository.countByStatus("CONFIRMED"));
        model.addAttribute("cancelledCount", bookingRepository.countByStatus("CANCELLED"));
        model.addAttribute("notifications", notifications.forAdmin());
        return "admin";
    }

    @PostMapping("/admin/bookings/cancel")
    public String cancelBooking(Long bookingId, RedirectAttributes redirect) {
        try {
            bookings.cancelByAdmin(bookingId);
            redirect.addFlashAttribute("success", "Booking cancelled and its seat released.");
        } catch (RuntimeException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admin";
    }
}
