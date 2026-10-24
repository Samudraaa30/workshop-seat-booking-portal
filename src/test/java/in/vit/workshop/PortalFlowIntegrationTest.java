package in.vit.workshop;

import in.vit.workshop.model.Workshop;
import in.vit.workshop.repo.BookingRepository;
import in.vit.workshop.repo.WorkshopRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PortalFlowIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired WorkshopRepository workshops;
    @Autowired BookingRepository bookings;
    private Workshop workshop;

    @BeforeEach void setUp() {
        bookings.deleteAll();
        workshops.deleteAll();
        workshop = workshops.save(new Workshop("Test workshop", "Hands-on testing", LocalDateTime.now().plusDays(2), 1));
    }

    @Test void homepageListsAvailableWorkshopAndHealthEndpointIsUp() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(content().string(containsString("Test workshop")));
        mvc.perform(get("/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }

    @Test void attendeeCanBookFindAndCancelSeat() throws Exception {
        mvc.perform(post("/book").param("workshopId", workshop.getId().toString())
                        .param("name", "  Asha Rao ").param("email", " ASHA@example.com "))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/"));
        mvc.perform(get("/my-bookings").param("email", "asha@example.com"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Test workshop")))
                .andExpect(content().string(containsString("CONFIRMED")));
        var booking = bookings.findAll().getFirst();
        mvc.perform(post("/cancel").param("bookingId", booking.getId().toString()).param("email", "asha@example.com"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/my-bookings").param("email", "asha@example.com"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("CANCELLED")));
        org.junit.jupiter.api.Assertions.assertEquals(0, workshops.findById(workshop.getId()).orElseThrow().getBookedSeats());
    }

    @Test void fullWorkshopDoesNotAcceptSecondBooking() throws Exception {
        mvc.perform(post("/book").param("workshopId", workshop.getId().toString()).param("name", "Asha").param("email", "a@example.com"));
        mvc.perform(post("/book").param("workshopId", workshop.getId().toString()).param("name", "Bala").param("email", "b@example.com"))
                .andExpect(status().is3xxRedirection());
        org.junit.jupiter.api.Assertions.assertEquals(1, bookings.count());
        org.junit.jupiter.api.Assertions.assertEquals(1, workshops.findById(workshop.getId()).orElseThrow().getBookedSeats());
    }
}
