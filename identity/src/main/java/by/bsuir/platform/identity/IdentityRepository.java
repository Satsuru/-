package by.bsuir.platform.identity;

import by.bsuir.platform.contracts.Api;
import by.bsuir.platform.contracts.Failures;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
class IdentityRepository {

    private final JdbcTemplate jdbc;

    IdentityRepository(@Qualifier("identityJdbc") JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    record Credential(long id, String hash) {
    }

    Api.UserView find(long userId) {
        List<Api.UserView> rows = jdbc.query(
                "select id, email, full_name, phone from identity.users where id = ? and active = true",
                (rs, n) -> new Api.UserView(rs.getLong("id"), rs.getString("email"), rs.getString("full_name"), rs.getString("phone"), List.of()),
                userId
        );
        if (rows.isEmpty()) {
            throw new Failures.NotFoundException("Пользователь не найден");
        }
        List<String> roles = jdbc.query(
                "select role_code from identity.user_roles where user_id = ? order by id",
                (rs, n) -> rs.getString("role_code"),
                userId
        );
        Api.UserView user = rows.get(0);
        return new Api.UserView(user.id(), user.email(), user.fullName(), user.phone(), roles);
    }

    Credential credential(String email) {
        List<Credential> rows = jdbc.query(
                "select id, password_hash from identity.users where lower(email) = lower(?) and active = true",
                (rs, n) -> new Credential(rs.getLong("id"), rs.getString("password_hash")),
                email
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    void hashPlainPasswords(java.util.function.Function<String, String> encode) {
        List<Credential> rows = jdbc.query(
                "select id, password_hash from identity.users",
                (rs, n) -> new Credential(rs.getLong("id"), rs.getString("password_hash"))
        );
        for (Credential row : rows) {
            if (row.hash() != null && !row.hash().startsWith("$2")) {
                jdbc.update("update identity.users set password_hash = ? where id = ?", encode.apply(row.hash()), row.id());
            }
        }
    }

    boolean emailTaken(String email) {
        Integer count = jdbc.queryForObject(
                "select count(*) from identity.users where lower(email) = lower(?)",
                Integer.class,
                email
        );
        return count != null && count > 0;
    }

    long insertUser(String email, String password, String fullName, String phone) {
        Long id = jdbc.queryForObject("select coalesce(max(id), 0) + 1 from identity.users", Long.class);
        jdbc.update("insert into identity.users (id, email, password_hash, full_name, phone, active) values (?, ?, ?, ?, ?, true)",
                id, email, password, fullName, phone);
        Long roleId = jdbc.queryForObject("select coalesce(max(id), 0) + 1 from identity.user_roles", Long.class);
        jdbc.update("insert into identity.user_roles (id, user_id, role_code) values (?, ?, 'GUEST')", roleId, id);
        return id;
    }

    int countUsers() {
        Integer count = jdbc.queryForObject("select count(*) from identity.users", Integer.class);
        return count == null ? 0 : count;
    }
}
