package by.bsuir.platform.analytics;

import by.bsuir.platform.contracts.Dates;
import by.bsuir.platform.contracts.ProtoMaps;
import by.bsuir.platform.contracts.Rpc;
import by.bsuir.platform.contracts.grpc.AnalyticsApiGrpc;
import by.bsuir.platform.contracts.grpc.OccupancyRevenue;
import by.bsuir.platform.contracts.grpc.PeriodRequest;
import io.grpc.stub.StreamObserver;

/** gRPC для чтения сводки. Новые факты приходят из Kafka. */
final class AnalyticsGrpcService extends AnalyticsApiGrpc.AnalyticsApiImplBase {

    private final AnalyticsService analytics;

    AnalyticsGrpcService(AnalyticsService analytics) {
        this.analytics = analytics;
    }

    @Override
    public void occupancyAndRevenue(PeriodRequest request, StreamObserver<OccupancyRevenue> observer) {
        Rpc.reply(observer, () -> ProtoMaps.occupancy(analytics.occupancyAndRevenue(
                request.getHotelId(),
                Dates.parse(request.getFrom(), "from"),
                Dates.parse(request.getTo(), "to")
        )));
    }
}
