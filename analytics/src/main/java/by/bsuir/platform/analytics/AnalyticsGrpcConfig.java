package by.bsuir.platform.analytics;

import by.bsuir.platform.contracts.Rpc;
import io.grpc.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class AnalyticsGrpcConfig {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsGrpcConfig.class);

    @Bean(destroyMethod = "shutdown")
    Server analyticsGrpcServer(AnalyticsService analytics, @Value("${grpc.server.port:9089}") int port) {
        Server server = Rpc.start(port, new AnalyticsGrpcService(analytics));
        log.info("Analytics gRPC {}", server.getPort());
        return server;
    }
}
