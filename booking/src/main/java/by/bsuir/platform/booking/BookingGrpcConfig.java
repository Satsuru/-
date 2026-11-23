package by.bsuir.platform.booking;

import by.bsuir.platform.contracts.Rpc;
import io.grpc.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class BookingGrpcConfig {

    private static final Logger log = LoggerFactory.getLogger(BookingGrpcConfig.class);

    @Bean(destroyMethod = "shutdown")
    Server bookingGrpcServer(BookingService bookings, BookingFlow flow, @Value("${grpc.server.port:9084}") int port) {
        Server server = Rpc.start(port, new BookingGrpcService(bookings, flow));
        log.info("Booking gRPC {}", server.getPort());
        return server;
    }
}
