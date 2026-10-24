package in.vit.workshop.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Entity
public class Workshop {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank private String title;
    @NotBlank @Column(length = 1000) private String description;
    private LocalDateTime startsAt;
    @Min(1) private int capacity;
    private int bookedSeats;

    public Workshop() {}
    public Workshop(String title, String description, LocalDateTime startsAt, int capacity) {
        this.title = title; this.description = description; this.startsAt = startsAt;
        this.capacity = capacity; this.bookedSeats = 0;
    }
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public LocalDateTime getStartsAt() { return startsAt; }
    public int getCapacity() { return capacity; }
    public int getBookedSeats() { return bookedSeats; }
    public int getAvailableSeats() { return Math.max(0, capacity - bookedSeats); }
    public void setId(Long id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setStartsAt(LocalDateTime startsAt) { this.startsAt = startsAt; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public void setBookedSeats(int bookedSeats) { this.bookedSeats = bookedSeats; }
}
