package by.bsuir.platform.booking;

import by.bsuir.platform.contracts.Api;
import by.bsuir.platform.contracts.Dates;
import by.bsuir.platform.contracts.ProtoMaps;
import by.bsuir.platform.contracts.Rpc;
import by.bsuir.platform.contracts.grpc.BookingApiGrpc;
import by.bsuir.platform.contracts.grpc.BookingList;
import by.bsuir.platform.contracts.grpc.BookingStats;
import by.bsuir.platform.contracts.grpc.BookingView;
import by.bsuir.platform.contracts.grpc.CancelBookingRequest;
import by.bsuir.platform.contracts.grpc.CreateBookingRequest;
import by.bsuir.platform.contracts.grpc.Empty;
import by.bsuir.platform.contracts.grpc.GetBookingRequest;
import by.bsuir.platform.contracts.grpc.ListBookingsRequest;
import by.bsuir.platform.contracts.grpc.PayBookingRequest;
import by.bsuir.platform.contracts.grpc.UserRequest;
import io.grpc.stub.StreamObserver;

/** gRPC-сервер бронирования. Сценарий выполняет BookingFlow. */
final class BookingGrpcService extends BookingApiGrpc.BookingApiImplBase {

    private final BookingService bookings;
    private final BookingFlow flow;

    BookingGrpcService(BookingService bookings, BookingFlow flow) {
        this.bookings = bookings;
        this.flow = flow;
    }

    @Override
    public void listMine(ListBookingsRequest request, StreamObserver<BookingList> observer) {
        Rpc.reply(observer, () -> ProtoMaps.bookings(bookings.listMine(
                request.getUserId(),
                request.getStatus().isBlank() ? null : request.getStatus(),
                request.getSort().isBlank() ? null : Api.BookingSort.valueOf(request.getSort())
        )));
    }

    @Override
    public void get(GetBookingRequest request, StreamObserver<BookingView> observer) {
        Rpc.reply(observer, () -> ProtoMaps.booking(bookings.get(request.getBookingId(), request.getUserId())));
    }

    @Override
    public void stats(UserRequest request, StreamObserver<BookingStats> observer) {
        Rpc.reply(observer, () -> ProtoMaps.stats(bookings.stats(request.getUserId())));
    }

    @Override
    public void create(CreateBookingRequest request, StreamObserver<BookingView> observer) {
        Rpc.reply(observer, () -> ProtoMaps.booking(flow.create(
                request.getUserId(),
                request.getRoomId(),
                Dates.parse(request.getCheckIn(), "checkIn"),
                Dates.parse(request.getCheckOut(), "checkOut"),
                request.getGuests()
        )));
    }

    @Override
    public void pay(PayBookingRequest request, StreamObserver<BookingView> observer) {
        Rpc.reply(observer, () -> ProtoMaps.booking(flow.pay(request.getUserId(), request.getBookingId(), request.getMethod())));
    }

    @Override
    public void cancel(CancelBookingRequest request, StreamObserver<BookingView> observer) {
        Rpc.reply(observer, () -> ProtoMaps.booking(flow.cancel(request.getActorId(), request.getStaff(), request.getBookingId())));
    }

    @Override
    public void sweep(Empty request, StreamObserver<Empty> observer) {
        Rpc.reply(observer, () -> {
            flow.sweep();
            return Empty.getDefaultInstance();
        });
    }
}
