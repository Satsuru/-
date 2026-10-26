package by.bsuir.platform.notification;

import by.bsuir.platform.contracts.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final JdbcTemplate jdbc;

    public NotificationService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Api.NotificationView> listMine(long userId) {
        return jdbc.query(
                "select id, kind, body, status, created_at from notification.notifications where user_id = ? order by id",
                (rs, n) -> new Api.NotificationView(rs.getLong("id"), rs.getString("kind"), rs.getString("body"),
                        rs.getString("status"), rs.getTimestamp("created_at").toInstant().toString()),
                userId
        );
    }

    public void add(long userId, String kind, String body) {
        Long id = jdbc.queryForObject("select coalesce(max(id), 0) + 1 from notification.notifications", Long.class);
        jdbc.update("""
                insert into notification.notifications (id, user_id, kind, body, status, created_at)
                values (?, ?, ?, ?, 'NEW', current_timestamp)
                """, id, userId, kind, body);
    }

    @Override
    public void run(ApplicationArguments args) {
        Integer count = jdbc.queryForObject("select count(*) from notification.notifications", Integer.class);
        log.info("Notification: своя схема notification, писем {}", count);
    }
}
