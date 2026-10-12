package by.bsuir.platform.assistant;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class AssistantRepository {

    private final JdbcTemplate jdbc;

    AssistantRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    int countDialogs() {
        Integer count = jdbc.queryForObject("select count(*) from assistant.dialogs", Integer.class);
        return count == null ? 0 : count;
    }
}
