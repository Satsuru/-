package by.bsuir.platform.assistant;

import by.bsuir.platform.contracts.Failures;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AssistantService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AssistantService.class);
    private final JdbcTemplate jdbc;

    public AssistantService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long ask(long userId, Long dialogId, String guestText, String answer, boolean escalated) {
        long id;
        if (dialogId == null) {
            id = next("assistant.dialogs");
            jdbc.update("insert into assistant.dialogs (id, user_id, status) values (?, ?, ?)",
                    id, userId, escalated ? "ESCALATED" : "OPEN");
        } else {
            Integer own = jdbc.queryForObject(
                    "select count(*) from assistant.dialogs where id = ? and user_id = ?",
                    Integer.class, dialogId, userId
            );
            if (own == null || own == 0) {
                throw new Failures.NotFoundException("Диалог не найден");
            }
            id = dialogId;
            if (escalated) {
                jdbc.update("update assistant.dialogs set status = 'ESCALATED' where id = ?", id);
            }
        }
        add(id, "GUEST", guestText);
        add(id, "ASSISTANT", answer);
        return id;
    }

    private void add(long dialogId, String author, String body) {
        jdbc.update("insert into assistant.messages (id, dialog_id, author, body) values (?, ?, ?, ?)",
                next("assistant.messages"), dialogId, author, body);
    }

    private long next(String table) {
        Long id = jdbc.queryForObject("select coalesce(max(id), 0) + 1 from " + table, Long.class);
        return id == null ? 1 : id;
    }

    @Override
    public void run(ApplicationArguments args) {
        Integer count = jdbc.queryForObject("select count(*) from assistant.dialogs", Integer.class);
        log.info("Assistant: своя схема assistant, диалогов {}", count);
    }
}
