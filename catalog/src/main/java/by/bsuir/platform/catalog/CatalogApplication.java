package by.bsuir.platform.catalog;

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
public class CatalogApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(CatalogApplication.class);
        application.setDefaultProperties(Map.of("spring.config.name", "catalog"));
        application.run(args);
    }
}
