package by.bsuir.platform.notification;

import by.bsuir.platform.api.Contracts.NotificationPort;
import by.bsuir.platform.api.Contracts.NotificationView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Order(5)
public class NotificationService implements NotificationPort, ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<NotificationView> listMine(long userId) {
        return repository.listMine(userId);
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Notification: своя схема notification, писем {}", repository.count());
    }
}
