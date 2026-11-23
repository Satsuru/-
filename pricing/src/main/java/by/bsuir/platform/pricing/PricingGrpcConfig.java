package by.bsuir.platform.pricing;

import by.bsuir.platform.contracts.Rpc;
import io.grpc.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class PricingGrpcConfig {

    private static final Logger log = LoggerFactory.getLogger(PricingGrpcConfig.class);

    @Bean(destroyMethod = "shutdown")
    Server pricingGrpcServer(PricingService pricing, @Value("${grpc.server.port:9083}") int port) {
        Server server = Rpc.start(port, new PricingGrpcService(pricing));
        log.info("Pricing gRPC {}", server.getPort());
        return server;
    }
}
