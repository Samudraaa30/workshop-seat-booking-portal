package in.vit.workshop.service;

import in.vit.workshop.model.Workshop;
import in.vit.workshop.repo.WorkshopRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VitEventsCatalogService {
    private final WorkshopRepository workshops;
    private final ApplicationEventPublisher events;

    public VitEventsCatalogService(WorkshopRepository workshops, ApplicationEventPublisher events) {
        this.workshops = workshops;
        this.events = events;
    }

    @Transactional
    public int synchronize(List<VitEventsFeedParser.ImportedEvent> feed, int defaultCapacity) {
        int changed = 0;
        for (var event : feed) {
            Workshop workshop = workshops.findByExternalSourceKey(event.sourceKey()).orElseGet(() -> {
                Workshop created = new Workshop(event.title(),
                        "Official VIT announcement. Seat capacity and portal bookings are managed separately here.",
                        event.startsOn().atTime(LocalTime.MIDNIGHT), defaultCapacity);
                created.setExternalSourceKey(event.sourceKey());
                return created;
            });
            boolean isNew = workshop.getId() == null;
            boolean isChanged = isNew
                    || !event.title().equals(workshop.getTitle())
                    || !event.sourceUrl().equals(workshop.getSourceUrl())
                    || !event.dateText().equals(workshop.getSourceDateText())
                    || !event.startsOn().atTime(LocalTime.MIDNIGHT).equals(workshop.getStartsAt());
            if (isChanged) {
                workshop.setTitle(event.title());
                workshop.setSourceUrl(event.sourceUrl());
                workshop.setSourceDateText(event.dateText());
                workshop.setStartsAt(event.startsOn().atTime(LocalTime.MIDNIGHT));
                workshops.save(workshop);
                changed++;
            }
        }
        if (changed > 0) events.publishEvent(new WorkshopAvailabilityChangedEvent());
        return changed;
    }
}
