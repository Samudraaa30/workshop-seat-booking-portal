package in.vit.workshop;

import in.vit.workshop.service.VitEventsFeedParser;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VitEventsFeedParserTest {
    private final VitEventsFeedParser parser = new VitEventsFeedParser();

    @Test void importsFutureOfficialEventsWithRangeDateAndSourceLink() {
        String html = "<ul><li class='eventsitem'><a class='plusright' href='https://vit.edu.in/fdp.pdf'>"
                + "<h2 class='eventsinetitle'>AI and VR Workshop</h2><div class='visibledate'>14th December to 19th December 2026</div>"
                + "</a></li></ul>";
        var result = parser.parse(html, LocalDate.of(2026, 10, 25));
        assertEquals(1, result.size());
        assertEquals("AI and VR Workshop", result.getFirst().title());
        assertEquals(LocalDate.of(2026, 12, 14), result.getFirst().startsOn());
        assertEquals("https://vit.edu.in/fdp.pdf", result.getFirst().sourceUrl());
    }

    @Test void excludesPastAndNonVitLinksAndSkipsUnparseableDates() {
        String html = "<li class='eventsitem'><a class='plusright' href='https://vit.edu.in/a'><b class='visibletitle'>Past</b><span class='date'>1st January 2026</span></a></li>"
                + "<li class='eventsitem'><a class='plusright' href='https://evil.example/a'><b class='visibletitle'>Other</b><span class='date'>1st January 2027</span></a></li>"
                + "<li class='eventsitem'><a class='plusright' href='https://vit.edu.in/a'><b class='visibletitle'>Unknown date</b><span class='date'>Soon</span></a></li>";
        assertTrue(parser.parse(html, LocalDate.of(2026, 10, 25)).isEmpty());
    }
}
