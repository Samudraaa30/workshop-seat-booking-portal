package in.vit.workshop.service;

import java.net.URI;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

/** Parses the public VIT events announcement page (the page is not a dedicated calendar API). */
@Component
public class VitEventsFeedParser {
    private static final Pattern DAY_MONTH = Pattern.compile("(?i)(\\d{1,2})(?:st|nd|rd|th)?\\s+([A-Za-z]+)");
    private static final Pattern YEAR = Pattern.compile("\\b(20\\d{2})\\b");

    public List<ImportedEvent> parse(String html, LocalDate today) {
        var document = Jsoup.parse(html);
        List<ImportedEvent> events = new ArrayList<>();
        for (Element item : document.select("li.eventsitem")) {
            String title = item.select(".eventsinetitle, .visibletitle").first() == null
                    ? "" : item.select(".eventsinetitle, .visibletitle").first().text().trim();
            Element dateElement = item.select(".visibledate, .date").first();
            Element link = item.select("a.plusright[href]").first();
            if (dateElement == null || link == null || title.isBlank()) continue;
            String dateText = dateElement.text().trim();
            Optional<LocalDate> date = parseStartDate(dateText);
            String href = link.absUrl("href");
            if (href.isBlank()) href = link.attr("href");
            if (date.isEmpty() || date.get().isBefore(today) || !isVITUrl(href)) continue;
            events.add(new ImportedEvent(title, dateText, date.get(), href));
        }
        return List.copyOf(events);
    }

    private Optional<LocalDate> parseStartDate(String text) {
        Matcher dateMatcher = DAY_MONTH.matcher(text);
        Matcher yearMatcher = YEAR.matcher(text);
        if (!dateMatcher.find() || !yearMatcher.find()) return Optional.empty();
        try {
            int day = Integer.parseInt(dateMatcher.group(1));
            Month month = Month.valueOf(dateMatcher.group(2).toUpperCase(Locale.ROOT));
            int year = Integer.parseInt(yearMatcher.group(1));
            return Optional.of(LocalDate.of(year, month, day));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private boolean isVITUrl(String href) {
        try {
            String host = URI.create(href).getHost();
            return host != null && (host.equalsIgnoreCase("vit.edu.in") || host.toLowerCase(Locale.ROOT).endsWith(".vit.edu.in"));
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    public record ImportedEvent(String title, String dateText, LocalDate startsOn, String sourceUrl) {
        public String sourceKey() { return sourceUrl; }
    }
}
