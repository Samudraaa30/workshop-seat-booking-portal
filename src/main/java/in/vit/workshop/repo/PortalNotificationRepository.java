package in.vit.workshop.repo;

import in.vit.workshop.model.PortalNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PortalNotificationRepository extends JpaRepository<PortalNotification, Long> {
    List<PortalNotification> findTop50ByRecipientEmailIgnoreCaseOrderByCreatedAtDesc(String recipientEmail);
}
