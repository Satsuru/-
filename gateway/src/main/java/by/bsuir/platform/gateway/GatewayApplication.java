package by.bsuir.platform.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        SqlInitializationAutoConfiguration.class
})
@ComponentScan(
        basePackages = {
                "by.bsuir.platform.gateway",
                "by.bsuir.platform.catalog",
                "by.bsuir.platform.pricing",
                "by.bsuir.platform.booking",
                "by.bsuir.platform.payment",
                "by.bsuir.platform.assistant",
                "by.bsuir.platform.notification",
                "by.bsuir.platform.analytics"
        },
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "by\\.bsuir\\.platform\\..*Application"
        )
)
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
