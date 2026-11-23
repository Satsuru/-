package by.bsuir.platform.booking;

import by.bsuir.platform.contracts.Api;
import by.bsuir.platform.contracts.ProtoMaps;
import by.bsuir.platform.contracts.Rpc;
import by.bsuir.platform.contracts.grpc.CatalogApiGrpc;
import by.bsuir.platform.contracts.grpc.HoldRequest;
import by.bsuir.platform.contracts.grpc.Id;
import by.bsuir.platform.contracts.grpc.PaymentApiGrpc;
import by.bsuir.platform.contracts.grpc.PaymentRequest;
import by.bsuir.platform.contracts.grpc.PriceRequest;
import by.bsuir.platform.contracts.grpc.PricingApiGrpc;
import by.bsuir.platform.contracts.grpc.RoomStayRequest;
import io.grpc.ManagedChannel;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/** Исходящие gRPC-вызовы сервиса бронирования. */
@Component
public class BookingRemotes implements DisposableBean {

    private final ManagedChannel catalogChannel;
    private final ManagedChannel pricingChannel;
    private final ManagedChannel paymentChannel;
    private final CatalogApiGrpc.CatalogApiBlockingStub catalog;
    private final PricingApiGrpc.PricingApiBlockingStub pricing;
    private final PaymentApiGrpc.PaymentApiBlockingStub payment;

    public BookingRemotes(
            @Value("${services.catalog.host:127.0.0.1}") String catalogHost,
            @Value("${services.catalog.port:9082}") int catalogPort,
            @Value("${services.pricing.host:127.0.0.1}") String pricingHost,
            @Value("${services.pricing.port:9083}") int pricingPort,
            @Value("${services.payment.host:127.0.0.1}") String paymentHost,
            @Value("${services.payment.port:9085}") int paymentPort
    ) {
        this.catalogChannel = Rpc.channel(catalogHost, catalogPort);
        this.pricingChannel = Rpc.channel(pricingHost, pricingPort);
        this.paymentChannel = Rpc.channel(paymentHost, paymentPort);
        this.catalog = CatalogApiGrpc.newBlockingStub(catalogChannel);
        this.pricing = PricingApiGrpc.newBlockingStub(pricingChannel);
        this.payment = PaymentApiGrpc.newBlockingStub(paymentChannel);
    }

    Optional<Api.RoomCard> room(long roomId, LocalDate checkIn, LocalDate checkOut) {
        return Rpc.call(() -> ProtoMaps.room(Rpc.deadline(catalog).getRoom(stay(roomId, checkIn, checkOut))));
    }

    Api.PriceQuote price(long hotelId, BigDecimal basePrice, BigDecimal occupancy, LocalDate checkIn, LocalDate checkOut, int guests) {
        return Rpc.call(() -> ProtoMaps.quote(Rpc.deadline(pricing).calculate(PriceRequest.newBuilder()
                .setHotelId(hotelId)
                .setBasePrice(basePrice.toPlainString())
                .setOccupancyRatio(occupancy.toPlainString())
                .setCheckIn(checkIn.toString())
                .setCheckOut(checkOut.toString())
                .setGuests(guests)
                .build())));
    }

    void hold(long roomId, LocalDate checkIn, LocalDate checkOut, long bookingId) {
        Rpc.call(() -> Rpc.deadline(catalog).hold(HoldRequest.newBuilder()
                .setRoomId(roomId)
                .setCheckIn(checkIn.toString())
                .setCheckOut(checkOut.toString())
                .setBookingId(bookingId)
                .build()));
    }

    void release(long bookingId) {
        Rpc.call(() -> Rpc.deadline(catalog).release(Id.newBuilder().setId(bookingId).build()));
    }

    void confirmSold(long bookingId) {
        Rpc.call(() -> Rpc.deadline(catalog).confirmSold(Id.newBuilder().setId(bookingId).build()));
    }

    int activeRooms(long hotelId) {
        return Rpc.call(() -> Rpc.deadline(catalog).activeRooms(Id.newBuilder().setId(hotelId).build()).getValue());
    }

    void pay(long bookingId, long userId, BigDecimal amount, String method) {
        Rpc.call(() -> Rpc.deadline(payment).complete(PaymentRequest.newBuilder()
                .setBookingId(bookingId)
                .setUserId(userId)
                .setAmount(amount.toPlainString())
                .setMethod(method)
                .build()));
    }

    private static RoomStayRequest stay(long roomId, LocalDate checkIn, LocalDate checkOut) {
        return RoomStayRequest.newBuilder()
                .setRoomId(roomId)
                .setCheckIn(checkIn.toString())
                .setCheckOut(checkOut.toString())
                .build();
    }

    @Override
    public void destroy() {
        catalogChannel.shutdown();
        pricingChannel.shutdown();
        paymentChannel.shutdown();
    }
}
