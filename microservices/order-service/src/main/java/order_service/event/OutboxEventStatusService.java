
package order_service.event;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxEventStatusService {

    private final OutboxEventRepository repository;

    public OutboxEventStatusService(OutboxEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void markPublished(Long id) {
        repository.findById(id).ifPresent(event -> {
            event.markPublished();
            repository.save(event);
        });
    }
}
