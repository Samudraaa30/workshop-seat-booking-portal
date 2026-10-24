package in.vit.workshop;

import in.vit.workshop.model.Workshop;
import in.vit.workshop.repo.BookingRepository;
import in.vit.workshop.repo.WorkshopRepository;
import in.vit.workshop.service.BookingService;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BookingServiceTest {
    private final WorkshopRepository workshops = mock(WorkshopRepository.class);
    private final BookingRepository bookings = mock(BookingRepository.class);
    private final BookingService service = new BookingService(workshops, bookings);

    @Test void rejectsInvalidContactDetailsBeforeWriting() {
        assertThrows(IllegalArgumentException.class, () -> service.book(1L, " ", "bad-email"));
        verifyNoInteractions(workshops, bookings);
    }

    @Test void rejectsUnknownWorkshop() {
        when(workshops.findByIdForUpdate(99L)).thenReturn(Optional.empty());
        assertEquals("Workshop not found.", assertThrows(IllegalArgumentException.class,
                () -> service.book(99L, "Asha", "asha@example.com")).getMessage());
        verify(bookings, never()).save(any());
    }

    @Test void rejectsFullWorkshop() {
        Workshop workshop = new Workshop("Full", "Description", LocalDateTime.now(), 1);
        workshop.setBookedSeats(1);
        when(workshops.findByIdForUpdate(1L)).thenReturn(Optional.of(workshop));
        assertThrows(IllegalStateException.class, () -> service.book(1L, "Asha", "asha@example.com"));
        verify(bookings, never()).save(any());
    }
}
