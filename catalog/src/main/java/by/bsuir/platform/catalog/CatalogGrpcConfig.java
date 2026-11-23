package by.bsuir.platform.catalog;

import by.bsuir.platform.contracts.Rpc;
import io.grpc.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Поднимает сетевой gRPC-сервер каталога рядом с его базой. */
@Configuration
class CatalogGrpcConfig {

    private static final Logger log = LoggerFactory.getLogger(CatalogGrpcConfig.class);

    @Bean(destroyMethod = "shutdown")
    Server catalogGrpcServer(CatalogService catalog, @Value("${grpc.server.port:9082}") int port) {
        Server server = Rpc.start(port, new CatalogGrpcService(catalog));
        log.info("Catalog gRPC {}", server.getPort());
        return server;
    }
}
