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
    public String home(@RequestParam(required = false) String success,
                       @RequestParam(required = false) String error,
                       Model model) {
        model.addAttribute("workshops", service.workshops());
        if (success != null && !success.isBlank()) model.addAttribute("success", success);
        if (error != null && !error.isBlank()) model.addAttribute("error", error);
        return "index";
    }

    @PostMapping("/book")
    public String book(@RequestParam Long workshopId,
                       @RequestParam String name,
                       @RequestParam String email,
                       RedirectAttributes redirect) {
        try {
            service.book(workshopId, name, email);
            redirect.addAttribute("success", "Your seat is confirmed! Save the email you used to track or cancel your booking.");
        } catch (RuntimeException e) {
            redirect.addAttribute("error", e.getMessage());
        }
        return "redirect:/";
    }

    @GetMapping("/my-bookings")
    public String myBookings(@RequestParam(required = false) String email,
                             @RequestParam(required = false) String success,
                             @RequestParam(required = false) String error,
                             Model model) {
        model.addAttribute("email", email == null ? "" : email);
        model.addAttribute("bookings", (email == null || email.isBlank()) ? java.util.List.of() : service.bookingsFor(email));
        if (success != null && !success.isBlank()) model.addAttribute("success", success);
        if (error != null && !error.isBlank()) model.addAttribute("error", error);
        return "bookings";
    }

    @PostMapping("/cancel")
    public String cancel(@RequestParam Long bookingId, @RequestParam String email, RedirectAttributes redirect) {
        try {
            service.cancel(bookingId, email);
            redirect.addAttribute("success", "Booking cancelled.");
        } catch (RuntimeException e) {
            redirect.addAttribute("error", e.getMessage());
        }
        redirect.addAttribute("email", email);
        return "redirect:/my-bookings";
    }
}

