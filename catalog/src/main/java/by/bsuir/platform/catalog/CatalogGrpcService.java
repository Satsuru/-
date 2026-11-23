package by.bsuir.platform.catalog;

import by.bsuir.platform.contracts.Dates;
import by.bsuir.platform.contracts.ProtoMaps;
import by.bsuir.platform.contracts.Rpc;
import by.bsuir.platform.contracts.grpc.BoolValue;
import by.bsuir.platform.contracts.grpc.CatalogApiGrpc;
import by.bsuir.platform.contracts.grpc.Count;
import by.bsuir.platform.contracts.grpc.Empty;
import by.bsuir.platform.contracts.grpc.HoldRequest;
import by.bsuir.platform.contracts.grpc.HotelView;
import by.bsuir.platform.contracts.grpc.Id;
import by.bsuir.platform.contracts.grpc.RoomCard;
import by.bsuir.platform.contracts.grpc.RoomCardList;
import by.bsuir.platform.contracts.grpc.RoomStayRequest;
import by.bsuir.platform.contracts.grpc.SearchRequest;
import io.grpc.stub.StreamObserver;

import java.time.LocalDate;

/** gRPC-сервер каталога. Решение о занятости номера остаётся в CatalogService. */
final class CatalogGrpcService extends CatalogApiGrpc.CatalogApiImplBase {

    private final CatalogService catalog;

    CatalogGrpcService(CatalogService catalog) {
        this.catalog = catalog;
    }

    @Override
    public void hotel(Id request, StreamObserver<HotelView> observer) {
        Rpc.reply(observer, () -> ProtoMaps.hotel(catalog.hotel(request.getId())));
    }

    @Override
    public void search(SearchRequest request, StreamObserver<RoomCardList> observer) {
        Rpc.reply(observer, () -> ProtoMaps.rooms(catalog.search(
                request.getCity(),
                date(request.getCheckIn(), "checkIn"),
                date(request.getCheckOut(), "checkOut"),
                request.getGuests(),
                request.getHasMinStars() ? request.getMinStars() : null
        )));
    }

    @Override
    public void getRoom(RoomStayRequest request, StreamObserver<RoomCard> observer) {
        Rpc.reply(observer, () -> ProtoMaps.room(catalog.getRoom(
                request.getRoomId(),
                date(request.getCheckIn(), "checkIn"),
                date(request.getCheckOut(), "checkOut")
        )));
    }

    @Override
    public void isFree(RoomStayRequest request, StreamObserver<BoolValue> observer) {
        Rpc.reply(observer, () -> BoolValue.newBuilder().setValue(catalog.isFree(
                request.getRoomId(),
                date(request.getCheckIn(), "checkIn"),
                date(request.getCheckOut(), "checkOut")
        )).build());
    }

    @Override
    public void activeRooms(Id request, StreamObserver<Count> observer) {
        Rpc.reply(observer, () -> Count.newBuilder().setValue(catalog.activeRooms(request.getId())).build());
    }

    @Override
    public void hold(HoldRequest request, StreamObserver<Empty> observer) {
        Rpc.reply(observer, () -> {
            catalog.hold(request.getRoomId(), date(request.getCheckIn(), "checkIn"), date(request.getCheckOut(), "checkOut"), request.getBookingId());
            return Empty.getDefaultInstance();
        });
    }

    @Override
    public void release(Id request, StreamObserver<Empty> observer) {
        Rpc.reply(observer, () -> {
            catalog.release(request.getId());
            return Empty.getDefaultInstance();
        });
    }

    @Override
    public void confirmSold(Id request, StreamObserver<Empty> observer) {
        Rpc.reply(observer, () -> {
            catalog.confirmSold(request.getId());
            return Empty.getDefaultInstance();
        });
    }

    private static LocalDate date(String raw, String field) {
        return Dates.parse(raw, field);
    }
}
