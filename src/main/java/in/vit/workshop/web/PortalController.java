package in.vit.workshop.web;

import in.vit.workshop.service.BookingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PortalController {
    private final BookingService service;
    public PortalController(BookingService service) { this.service = service; }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("workshops", service.workshops());
        return "index";
    }

    @PostMapping("/book")
    public String book(@RequestParam Long workshopId,
                       @RequestParam String name,
                       @RequestParam String email,
                       RedirectAttributes redirect) {
        try {
            service.book(workshopId, name, email);
            redirect.addFlashAttribute("success", "Your seat is confirmed! Save the email you used to track or cancel your booking.");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/";
    }

    @GetMapping("/my-bookings")
    public String myBookings(@RequestParam(required = false) String email, Model model) {
        model.addAttribute("email", email == null ? "" : email);
        model.addAttribute("bookings", (email == null || email.isBlank()) ? java.util.List.of() : service.bookingsFor(email));
        return "bookings";
    }

    @PostMapping("/cancel")
    public String cancel(@RequestParam Long bookingId, @RequestParam String email, RedirectAttributes redirect) {
        try {
            service.cancel(bookingId, email);
            redirect.addFlashAttribute("success", "Booking cancelled.");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/my-bookings?email=" + java.net.URLEncoder.encode(email, java.nio.charset.StandardCharsets.UTF_8);
    }
}
