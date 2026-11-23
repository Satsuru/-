package by.bsuir.platform.gateway;

import by.bsuir.platform.analytics.AnalyticsApplication;
import by.bsuir.platform.assistant.AssistantApplication;
import by.bsuir.platform.booking.BookingApplication;
import by.bsuir.platform.catalog.CatalogApplication;
import by.bsuir.platform.identity.IdentityApplication;
import by.bsuir.platform.notification.NotificationApplication;
import by.bsuir.platform.payment.PaymentApplication;
import by.bsuir.platform.pricing.PricingApplication;
import io.grpc.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.env.MapPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Поднимает Kafka и сервисы на свободных портах перед контекстом шлюза. */
final class RunningPlatform implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final List<ConfigurableApplicationContext> APPS = new ArrayList<>();
    private static final Map<String, Object> PORTS = new LinkedHashMap<>();
    private static JdbcTemplate bookingJdbc;

    static JdbcTemplate bookingJdbc() {
        if (bookingJdbc == null) {
            throw new IllegalStateException("База бронирования не запущена");
        }
        return bookingJdbc;
    }

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        try {
            start();
        } catch (RuntimeException ex) {
            stop();
            throw ex;
        }
        context.addApplicationListener((ContextClosedEvent event) -> stop());
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("platform-it", PORTS));
    }

    private static void start() {
        stop();
        ensureKafka();

        ConfigurableApplicationContext catalog = launch(CatalogApplication.class, "catalog");
        ConfigurableApplicationContext pricing = launch(PricingApplication.class, "pricing");
        ConfigurableApplicationContext payment = launch(PaymentApplication.class, "payment");
        String brokers = "--spring.kafka.bootstrap-servers=localhost:9092";
        String offset = "--spring.kafka.consumer.auto-offset-reset=latest";
        String runId = UUID.randomUUID().toString();
        ConfigurableApplicationContext notification = launch(NotificationApplication.class, "notification", brokers, offset,
                "--spring.kafka.consumer.group-id=notification-" + runId);
        ConfigurableApplicationContext analytics = launch(AnalyticsApplication.class, "analytics", brokers, offset,
                "--spring.kafka.consumer.group-id=analytics-" + runId);
        ConfigurableApplicationContext assistant = launch(AssistantApplication.class, "assistant", brokers, offset);
        ConfigurableApplicationContext booking = launch(BookingApplication.class, "booking", brokers, offset,
                "--services.catalog.port=" + port(catalog),
                "--services.pricing.port=" + port(pricing),
                "--services.payment.port=" + port(payment));
        ConfigurableApplicationContext identity = launch(IdentityApplication.class, "identity");
        bookingJdbc = booking.getBean("bookingJdbc", JdbcTemplate.class);

        PORTS.clear();
        PORTS.put("auth.service.url", "http://127.0.0.1:" + ((WebServerApplicationContext) identity).getWebServer().getPort());
        PORTS.put("services.catalog.port", Integer.toString(port(catalog)));
        PORTS.put("services.pricing.port", Integer.toString(port(pricing)));
        PORTS.put("services.booking.port", Integer.toString(port(booking)));
        PORTS.put("services.notification.port", Integer.toString(port(notification)));
        PORTS.put("services.assistant.port", Integer.toString(port(assistant)));
        PORTS.put("services.analytics.port", Integer.toString(port(analytics)));
        try {
            Thread.sleep(4000);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private static void ensureKafka() {
        if (portOpen(9092)) {
            return;
        }
        Path dir = Path.of("").toAbsolutePath();
        if (!Files.exists(dir.resolve("docker-compose.yml"))) {
            dir = dir.getParent();
        }
        try {
            ProcessBuilder builder = new ProcessBuilder("docker", "compose", "up", "-d")
                    .directory(dir.toFile())
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD);
            Path desktopSocket = Path.of(System.getProperty("user.home"), ".docker", "desktop", "docker.sock");
            if (builder.environment().get("DOCKER_HOST") == null && Files.exists(desktopSocket)) {
                builder.environment().put("DOCKER_HOST", "unix://" + desktopSocket);
            }
            Process process = builder.start();
            process.waitFor();
        } catch (Exception ex) {
            throw new IllegalStateException("Не удалось запустить Kafka. Из каталога platform выполните: docker compose up -d", ex);
        }
        for (int attempt = 0; attempt < 60 && !portOpen(9092); attempt++) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        if (!portOpen(9092)) {
            throw new IllegalStateException("Kafka не слушает порт 9092");
        }
    }

    private static boolean portOpen(int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", port), 300);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private static int port(ConfigurableApplicationContext context) {
        return context.getBean(Server.class).getPort();
    }

    private static ConfigurableApplicationContext launch(Class<?> application, String configName, String... extra) {
        SpringApplication spring = new SpringApplication(application);
        spring.setDefaultProperties(Map.of("spring.config.name", configName));
        List<String> args = new ArrayList<>();
        args.add("--server.port=0");
        args.add("--grpc.server.port=0");
        args.add("--spring.main.banner-mode=off");
        args.add("--logging.level.root=WARN");
        args.addAll(List.of(extra));
        ConfigurableApplicationContext context = spring.run(args.toArray(String[]::new));
        APPS.add(context);
        return context;
    }

    private static void stop() {
        for (ConfigurableApplicationContext app : APPS) {
            if (app.isActive()) {
                app.close();
            }
        }
        APPS.clear();
        bookingJdbc = null;
    }
}
