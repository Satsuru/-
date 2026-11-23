package by.bsuir.platform.pricing;

import by.bsuir.platform.contracts.Dates;
import by.bsuir.platform.contracts.ProtoMaps;
import by.bsuir.platform.contracts.Rpc;
import by.bsuir.platform.contracts.grpc.PriceQuote;
import by.bsuir.platform.contracts.grpc.PriceRequest;
import by.bsuir.platform.contracts.grpc.PricingApiGrpc;
import io.grpc.stub.StreamObserver;

import java.math.BigDecimal;

/** gRPC-сервер цены. Формулу считает PricingService. */
final class PricingGrpcService extends PricingApiGrpc.PricingApiImplBase {

    private final PricingService pricing;

    PricingGrpcService(PricingService pricing) {
        this.pricing = pricing;
    }

    @Override
    public void calculate(PriceRequest request, StreamObserver<PriceQuote> observer) {
        Rpc.reply(observer, () -> ProtoMaps.quote(pricing.calculate(
                request.getHotelId(),
                new BigDecimal(request.getBasePrice()),
                new BigDecimal(request.getOccupancyRatio()),
                Dates.parse(request.getCheckIn(), "checkIn"),
                Dates.parse(request.getCheckOut(), "checkOut"),
                request.getGuests()
        )));
    }
}
