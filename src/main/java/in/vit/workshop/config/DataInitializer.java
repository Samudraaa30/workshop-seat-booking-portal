package in.vit.workshop.config;

import in.vit.workshop.model.Workshop;
import in.vit.workshop.repo.WorkshopRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import java.time.LocalDateTime;

@Configuration
@Profile("!prod")
public class DataInitializer {
    @Bean
    CommandLineRunner seedWorkshops(WorkshopRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                repository.save(new Workshop("Build Your First AI App", "A hands-on introduction to building useful AI-powered applications.", LocalDateTime.now().plusDays(3).withHour(11).withMinute(0), 30));
                repository.save(new Workshop("Git & GitHub Bootcamp", "Learn branching, pull requests and collaborative development.", LocalDateTime.now().plusDays(5).withHour(14).withMinute(0), 24));
                repository.save(new Workshop("Cloud & DevOps Essentials", "Explore CI/CD, containers and deployment fundamentals.", LocalDateTime.now().plusDays(7).withHour(10).withMinute(30), 20));
            }
        };
    }
}
