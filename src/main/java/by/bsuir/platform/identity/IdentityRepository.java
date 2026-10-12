package by.bsuir.platform.identity;

import by.bsuir.platform.api.Contracts.IdentityPort;
import by.bsuir.platform.api.Contracts.UserView;
import by.bsuir.platform.common.NotFoundException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
class IdentityRepository {

    private final JdbcTemplate jdbc;

    IdentityRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    UserView find(long userId) {
        List<UserView> rows = jdbc.query(
                "select id, email, full_name, phone from identity.users where id = ? and active = true",
                (rs, n) -> new UserView(
                        rs.getLong("id"),
                        rs.getString("email"),
                        rs.getString("full_name"),
                        rs.getString("phone"),
                        List.of()
                ),
                userId
        );
        if (rows.isEmpty()) {
            throw new NotFoundException("Пользователь не найден");
        }
        List<String> roles = jdbc.query(
                "select role_code from identity.user_roles where user_id = ? order by id",
                (rs, n) -> rs.getString("role_code"),
                userId
        );
        UserView user = rows.get(0);
        return new UserView(user.id(), user.email(), user.fullName(), user.phone(), roles);
    }

    int countUsers() {
        Integer count = jdbc.queryForObject("select count(*) from identity.users", Integer.class);
        return count == null ? 0 : count;
    }
}
