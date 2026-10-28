package in.vit.workshop;

import in.vit.workshop.model.Workshop;
import in.vit.workshop.repo.BookingRepository;
import in.vit.workshop.repo.WorkshopRepository;
import in.vit.workshop.service.BookingService;
import in.vit.workshop.service.VitEventsCatalogService;
import in.vit.workshop.service.VitEventsFeedParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import java.time.LocalDateTime;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"portal.admin.username=test-admin", "portal.admin.password=test-admin-password"})
@AutoConfigureMockMvc
class PortalFlowIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired WorkshopRepository workshops;
    @Autowired BookingRepository bookings;
    @Autowired BookingService service;
    @Autowired VitEventsCatalogService vitCatalog;
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

    @Test void availabilitySnapshotExposesCurrentSeatCountsForLiveClients() throws Exception {
        mvc.perform(get("/api/workshops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test workshop"))
                .andExpect(jsonPath("$[0].availableSeats").value(1))
                .andExpect(jsonPath("$[0].bookedSeats").value(0));
    }

    @Test void attendeeCanBookFindAndCancelSeat() throws Exception {
        mvc.perform(post("/book").with(SecurityMockMvcRequestPostProcessors.csrf()).param("workshopId", workshop.getId().toString())
                        .param("name", "  Asha Rao ").param("email", " ASHA@example.com "))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("/?success=*"));
        mvc.perform(get("/").param("success", "Your seat is confirmed!"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Your seat is confirmed!")))
                .andExpect(content().string(containsString("Test workshop")));
        mvc.perform(get("/my-bookings").param("email", "asha@example.com"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Test workshop")))
                .andExpect(content().string(containsString("CONFIRMED")))
                .andExpect(content().string(containsString("Seat confirmed")));
        var booking = bookings.findAll().getFirst();
        mvc.perform(post("/cancel").with(SecurityMockMvcRequestPostProcessors.csrf()).param("bookingId", booking.getId().toString()).param("email", "asha@example.com"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("/my-bookings?success=*&email=*"));
        mvc.perform(get("/my-bookings").param("email", "asha@example.com"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("CANCELLED")))
                .andExpect(content().string(containsString("Booking cancelled")));
        org.junit.jupiter.api.Assertions.assertEquals(0, workshops.findById(workshop.getId()).orElseThrow().getBookedSeats());
    }

    @Test void fullWorkshopDoesNotAcceptSecondBooking() throws Exception {
        mvc.perform(post("/book").with(SecurityMockMvcRequestPostProcessors.csrf()).param("workshopId", workshop.getId().toString()).param("name", "Asha").param("email", "a@example.com"));
        mvc.perform(post("/book").with(SecurityMockMvcRequestPostProcessors.csrf()).param("workshopId", workshop.getId().toString()).param("name", "Bala").param("email", "b@example.com"))
                .andExpect(status().is3xxRedirection());
        org.junit.jupiter.api.Assertions.assertEquals(1, bookings.count());
        org.junit.jupiter.api.Assertions.assertEquals(1, workshops.findById(workshop.getId()).orElseThrow().getBookedSeats());
    }

    @Test void adminAreaIsSeparateAndRequiresAdminRole() throws Exception {
        mvc.perform(get("/admin/login")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Admin sign in")))
                .andExpect(content().string(containsString("Sign in to admin")));
        mvc.perform(get("/admin")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/admin/login"));
        mvc.perform(get("/admin").with(SecurityMockMvcRequestPostProcessors.user("test-admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Admin dashboard")))
                .andExpect(content().string(containsString("Booking register")));
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(content().string(containsString("Admin sign in")));
    }

    @Test void adminCanCancelBookingAndTheAttendeeGetsANotification() throws Exception {
        mvc.perform(post("/book").with(SecurityMockMvcRequestPostProcessors.csrf())
                .param("workshopId", workshop.getId().toString()).param("name", "Asha").param("email", "asha@example.com"));
        var booking = bookings.findAll().getFirst();
        mvc.perform(post("/admin/bookings/cancel").with(SecurityMockMvcRequestPostProcessors.csrf())
                        .with(SecurityMockMvcRequestPostProcessors.user("test-admin").roles("ADMIN"))
                        .param("bookingId", booking.getId().toString()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin"));
        mvc.perform(get("/my-bookings").param("email", "asha@example.com"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Booking cancelled")));
        assertEquals(0, workshops.findById(workshop.getId()).orElseThrow().getBookedSeats());
    }

    @Test void importedListingsCanBeBookedAndRefreshDoesNotResetPortalSeats() {
        var event = new VitEventsFeedParser.ImportedEvent("VIT workshop", "14th December 2026",
                java.time.LocalDate.of(2026, 12, 14), "https://vit.edu.in/workshop.pdf");
        vitCatalog.synchronize(java.util.List.of(event), 5);
        Workshop imported = workshops.findByExternalSourceKey(event.sourceKey()).orElseThrow();
        assertTrue(imported.isImported());
        service.book(imported.getId(), "Test Attendee", "test.attendee@example.com");

        var updatedEvent = new VitEventsFeedParser.ImportedEvent("Updated VIT workshop", "15th December 2026",
                java.time.LocalDate.of(2026, 12, 15), event.sourceUrl());
        vitCatalog.synchronize(java.util.List.of(updatedEvent), 99);

        Workshop refreshed = workshops.findById(imported.getId()).orElseThrow();
        assertEquals("Updated VIT workshop", refreshed.getTitle());
        assertEquals(5, refreshed.getCapacity());
        assertEquals(1, refreshed.getBookedSeats());
        assertEquals(4, refreshed.getAvailableSeats());
    }
}

