package by.bsuir.platform.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

/**
 * Платёжный сервис читает только свою схему.
 * Создание платежа — мутация, в этой лабораторной её нет.
 */
@Service
@Order(7)
public class PaymentService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository repository;

    public PaymentService(PaymentRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Payment: своя схема payment, платежей {}", repository.count());
    }
}
