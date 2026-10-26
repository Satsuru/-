package by.bsuir.platform.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PaymentService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private final JdbcTemplate jdbc;

    public PaymentService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long complete(long bookingId, long userId, BigDecimal amount, String method) {
        Long id = jdbc.queryForObject("select coalesce(max(id), 0) + 1 from payment.payments", Long.class);
        jdbc.update("""
                insert into payment.payments (id, booking_id, user_id, amount, method, status, provider_ref)
                values (?, ?, ?, ?, ?, 'COMPLETED', ?)
                """, id, bookingId, userId, amount, method, "pay-" + id);
        return id;
    }

    @Override
    public void run(ApplicationArguments args) {
        Integer count = jdbc.queryForObject("select count(*) from payment.payments", Integer.class);
        log.info("Payment: своя схема payment, платежей {}", count);
    }
}
