package in.vit.workshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WorkshopSeatBookingApplication {
    public static void main(String[] args) {
        SpringApplication.run(WorkshopSeatBookingApplication.class, args);
    }
}
