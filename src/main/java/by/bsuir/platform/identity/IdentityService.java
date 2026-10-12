package by.bsuir.platform.identity;

import by.bsuir.platform.api.Contracts.IdentityPort;
import by.bsuir.platform.api.Contracts.UserView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

@Service
@Order(1)
public class IdentityService implements IdentityPort, ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(IdentityService.class);

    private final IdentityRepository repository;

    public IdentityService(IdentityRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserView me(long userId) {
        return repository.find(userId);
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Identity: своя схема identity, пользователей {}", repository.countUsers());
    }
}
