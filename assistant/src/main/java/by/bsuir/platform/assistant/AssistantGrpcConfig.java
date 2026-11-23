package by.bsuir.platform.assistant;

import by.bsuir.platform.contracts.Rpc;
import io.grpc.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class AssistantGrpcConfig {

    private static final Logger log = LoggerFactory.getLogger(AssistantGrpcConfig.class);

    @Bean(destroyMethod = "shutdown")
    Server assistantGrpcServer(AssistantService assistant, @Value("${grpc.server.port:9086}") int port) {
        Server server = Rpc.start(port, new AssistantGrpcService(assistant));
        log.info("Assistant gRPC {}", server.getPort());
        return server;
    }
}
