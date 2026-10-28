package in.vit.workshop.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "portal_notification", indexes = {
        @Index(name = "idx_portal_notification_recipient", columnList = "recipient_email,created_at")
})
public class PortalNotification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_email", nullable = false, length = 160)
    private String recipientEmail;

    @Column(nullable = false, length = 40)
    private String kind;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected PortalNotification() {}

    public PortalNotification(String recipientEmail, String kind, String title, String message) {
        this.recipientEmail = recipientEmail;
        this.kind = kind;
        this.title = title;
        this.message = message;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getRecipientEmail() { return recipientEmail; }
    public String getKind() { return kind; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
