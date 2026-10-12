package by.bsuir.platform.assistant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

/**
 * Помощник хранит только диалоги. Отели, цены и брони к себе не записывает.
 * Вопрос гостя — мутация, в этой лабораторной её нет.
 */
@Service
@Order(8)
public class AssistantService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AssistantService.class);

    private final AssistantRepository repository;

    public AssistantService(AssistantRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Assistant: своя схема assistant, диалогов {}", repository.countDialogs());
    }
}
