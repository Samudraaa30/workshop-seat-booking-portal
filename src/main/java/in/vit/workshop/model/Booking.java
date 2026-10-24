package in.vit.workshop.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "seat_booking")
public class Booking {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String attendeeName;
    private String attendeeEmail;
    @ManyToOne(optional = false) private Workshop workshop;
    private String status;
    private LocalDateTime createdAt;

    public Booking() {}
    public Booking(String name, String email, Workshop workshop) {
        this.attendeeName = name; this.attendeeEmail = email; this.workshop = workshop;
        this.status = "CONFIRMED"; this.createdAt = LocalDateTime.now();
    }
    public Long getId() { return id; }
    public String getAttendeeName() { return attendeeName; }
    public String getAttendeeEmail() { return attendeeEmail; }
    public Workshop getWorkshop() { return workshop; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setId(Long id) { this.id = id; }
    public void setAttendeeName(String name) { this.attendeeName = name; }
    public void setAttendeeEmail(String email) { this.attendeeEmail = email; }
    public void setWorkshop(Workshop workshop) { this.workshop = workshop; }
    public void setStatus(String status) { this.status = status; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
