package by.bsuir.platform.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;

import java.util.Map;

@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        SqlInitializationAutoConfiguration.class
})
public class NotificationApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(NotificationApplication.class);
        application.setDefaultProperties(Map.of("spring.config.name", "notification"));
        application.run(args);
    }
}
