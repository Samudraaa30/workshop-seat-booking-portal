package in.vit.workshop.web;

import in.vit.workshop.service.BookingService;
import in.vit.workshop.service.WorkshopAvailabilityChangedEvent;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Controller
@RequestMapping("/api/workshops")
public class WorkshopRealtimeController {
    private final BookingService service;
    private final CopyOnWriteArrayList<SseEmitter> clients = new CopyOnWriteArrayList<>();

    public WorkshopRealtimeController(BookingService service) {
        this.service = service;
    }

    @GetMapping
    @ResponseBody
    public List<WorkshopAvailability> snapshot() {
        return service.workshops().stream().map(WorkshopAvailability::from).toList();
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        clients.add(emitter);
        emitter.onCompletion(() -> clients.remove(emitter));
        emitter.onTimeout(() -> clients.remove(emitter));
        emitter.onError(error -> clients.remove(emitter));
        sendAvailability(emitter);
        return emitter;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAvailabilityChanged(WorkshopAvailabilityChangedEvent ignored) {
        broadcastAvailability();
    }

    /** Keep long-lived browser EventSource connections open through idle periods and proxies. */
    @Scheduled(fixedRate = 15_000)
    public void sendHeartbeat() {
        clients.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().comment("keep-alive"));
            } catch (IOException | IllegalStateException exception) {
                clients.remove(emitter);
            }
        });
    }

    private void broadcastAvailability() {
        List<WorkshopAvailability> current = snapshot();
        clients.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().name("availability").data(current));
            } catch (IOException | IllegalStateException exception) {
                clients.remove(emitter);
            }
        });
    }

    private void sendAvailability(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().name("availability").data(snapshot()));
        } catch (IOException | IllegalStateException exception) {
            clients.remove(emitter);
        }
    }
}
