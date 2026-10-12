package by.bsuir.platform.notification;

import by.bsuir.platform.api.Contracts.NotificationView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
class NotificationRepository {

    private final JdbcTemplate jdbc;

    NotificationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    List<NotificationView> listMine(long userId) {
        return jdbc.query(
                """
                select id, kind, body, status, created_at
                from notification.notifications
                where user_id = ?
                order by id
                """,
                (rs, n) -> new NotificationView(
                        rs.getLong("id"),
                        rs.getString("kind"),
                        rs.getString("body"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at").toInstant().toString()
                ),
                userId
        );
    }

    int count() {
        Integer count = jdbc.queryForObject("select count(*) from notification.notifications", Integer.class);
        return count == null ? 0 : count;
    }
}
