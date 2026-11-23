package by.bsuir.platform.notification;

import by.bsuir.platform.contracts.Rpc;
import io.grpc.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class NotificationGrpcConfig {

    private static final Logger log = LoggerFactory.getLogger(NotificationGrpcConfig.class);

    @Bean(destroyMethod = "shutdown")
    Server notificationGrpcServer(NotificationService notifications, @Value("${grpc.server.port:9087}") int port) {
        Server server = Rpc.start(port, new NotificationGrpcService(notifications));
        log.info("Notification gRPC {}", server.getPort());
        return server;
    }
}
