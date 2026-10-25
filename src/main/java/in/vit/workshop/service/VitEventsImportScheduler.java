package in.vit.workshop.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "portal.vit-import.enabled", havingValue = "true")
public class VitEventsImportScheduler {
    private static final Logger log = LoggerFactory.getLogger(VitEventsImportScheduler.class);
    private static final String FEED_URL = "https://vit.edu.in/wp-json/wp/v2/pages?slug=news-update";
    private final RestClient client;
    private final ObjectMapper json;
    private final VitEventsFeedParser parser;
    private final VitEventsCatalogService catalog;
    private final int capacity;

    public VitEventsImportScheduler(ObjectMapper json, VitEventsFeedParser parser, VitEventsCatalogService catalog,
                                    @Value("${portal.vit-import.default-capacity:30}") int capacity) {
        this.client = RestClient.builder().build();
        this.json = json;
        this.parser = parser;
        this.catalog = catalog;
        this.capacity = Math.max(1, capacity);
    }

    @Scheduled(initialDelay = 10_000, fixedDelayString = "${portal.vit-import.interval-ms:3600000}")
    public void synchronize() {
        try {
            String body = client.get().uri(FEED_URL).retrieve().body(String.class);
            JsonNode pages = json.readTree(body);
            String html = pages.isArray() && !pages.isEmpty()
                    ? pages.get(0).path("content").path("rendered").asText("") : "";
            int imported = catalog.synchronize(parser.parse(html, LocalDate.now()), capacity);
            log.info("VIT announcements sync completed: {} upcoming listings processed", imported);
        } catch (Exception exception) {
            log.warn("VIT announcements sync failed; existing listings remain available: {}", exception.getMessage());
        }
    }
}
