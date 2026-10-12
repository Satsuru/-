package by.bsuir.platform.payment;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class PaymentRepository {

    private final JdbcTemplate jdbc;

    PaymentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    int count() {
        Integer count = jdbc.queryForObject("select count(*) from payment.payments", Integer.class);
        return count == null ? 0 : count;
    }
}
