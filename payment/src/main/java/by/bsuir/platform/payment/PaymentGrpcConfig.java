package by.bsuir.platform.payment;

import by.bsuir.platform.contracts.Rpc;
import io.grpc.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class PaymentGrpcConfig {

    private static final Logger log = LoggerFactory.getLogger(PaymentGrpcConfig.class);

    @Bean(destroyMethod = "shutdown")
    Server paymentGrpcServer(PaymentService payments, @Value("${grpc.server.port:9085}") int port) {
        Server server = Rpc.start(port, new PaymentGrpcService(payments));
        log.info("Payment gRPC {}", server.getPort());
        return server;
    }
}
