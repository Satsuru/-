package by.bsuir.platform.contracts;

import by.bsuir.platform.contracts.grpc.BookingStats;
import by.bsuir.platform.contracts.grpc.BookingView;
import by.bsuir.platform.contracts.grpc.DialogView;
import by.bsuir.platform.contracts.grpc.HotelView;
import by.bsuir.platform.contracts.grpc.MessageView;
import by.bsuir.platform.contracts.grpc.NotificationList;
import by.bsuir.platform.contracts.grpc.NotificationView;
import by.bsuir.platform.contracts.grpc.OccupancyRevenue;
import by.bsuir.platform.contracts.grpc.PriceQuote;
import by.bsuir.platform.contracts.grpc.RoomCard;
import by.bsuir.platform.contracts.grpc.RoomCardList;
import by.bsuir.platform.contracts.grpc.SnapshotView;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Перевод моделей платформы в protobuf и обратно. Правил бронирования здесь нет. */
public final class ProtoMaps {

    private ProtoMaps() {
    }

    public static HotelView hotel(Optional<Api.HotelView> hotel) {
        if (hotel.isEmpty()) {
            return HotelView.newBuilder().setFound(false).build();
        }
        Api.HotelView row = hotel.get();
        return HotelView.newBuilder()
                .setFound(true)
                .setId(row.id())
                .setName(text(row.name()))
                .setCity(text(row.city()))
                .setAddress(text(row.address()))
                .setStarRating(row.starRating() == null ? 0 : row.starRating())
                .build();
    }

    public static Optional<Api.HotelView> hotel(HotelView message) {
        if (!message.getFound()) {
            return Optional.empty();
        }
        return Optional.of(new Api.HotelView(message.getId(), message.getName(), message.getCity(), message.getAddress(), message.getStarRating()));
    }

    public static RoomCard room(Optional<Api.RoomCard> room) {
        if (room.isEmpty()) {
            return RoomCard.newBuilder().setFound(false).build();
        }
        Api.RoomCard row = room.get();
        return RoomCard.newBuilder()
                .setFound(true)
                .setRoomId(row.roomId())
                .setHotelId(row.hotelId())
                .setHotelName(text(row.hotelName()))
                .setCity(text(row.city()))
                .setAddress(text(row.address()))
                .setStarRating(row.starRating())
                .setRoomType(text(row.roomType()))
                .setCapacity(row.capacity())
                .setBasePrice(money(row.basePrice()))
                .setOccupancyRatio(money(row.occupancyRatio()))
                .build();
    }

    public static Optional<Api.RoomCard> room(RoomCard message) {
        if (!message.getFound()) {
            return Optional.empty();
        }
        return Optional.of(new Api.RoomCard(
                message.getRoomId(),
                message.getHotelId(),
                message.getHotelName(),
                message.getCity(),
                message.getAddress(),
                message.getStarRating(),
                message.getRoomType(),
                message.getCapacity(),
                money(message.getBasePrice()),
                money(message.getOccupancyRatio())
        ));
    }

    public static RoomCardList rooms(List<Api.RoomCard> rooms) {
        RoomCardList.Builder builder = RoomCardList.newBuilder();
        for (Api.RoomCard room : rooms) {
            builder.addRooms(room(Optional.of(room)));
        }
        return builder.build();
    }

    public static List<Api.RoomCard> rooms(RoomCardList message) {
        List<Api.RoomCard> rooms = new ArrayList<>();
        for (RoomCard room : message.getRoomsList()) {
            room(room).ifPresent(rooms::add);
        }
        return rooms;
    }

    public static PriceQuote quote(Api.PriceQuote quote) {
        return PriceQuote.newBuilder()
                .setNightly(money(quote.nightly()))
                .setTotal(money(quote.total()))
                .setOccupancy(money(quote.occupancy()))
                .setSeason(money(quote.season()))
                .setWeekday(money(quote.weekday()))
                .setDemand(money(quote.demand()))
                .setGuest(money(quote.guest()))
                .setExplanation(text(quote.explanation()))
                .build();
    }

    public static Api.PriceQuote quote(PriceQuote message) {
        return new Api.PriceQuote(
                money(message.getNightly()),
                money(message.getTotal()),
                money(message.getOccupancy()),
                money(message.getSeason()),
                money(message.getWeekday()),
                money(message.getDemand()),
                money(message.getGuest()),
                message.getExplanation()
        );
    }

    public static BookingView booking(Api.BookingView booking) {
        if (booking == null) {
            return BookingView.newBuilder().setFound(false).build();
        }
        BookingView.Builder builder = BookingView.newBuilder()
                .setFound(true)
                .setId(booking.id())
                .setRoomId(booking.roomId())
                .setHotelId(booking.hotelId())
                .setCheckIn(text(booking.checkIn()))
                .setCheckOut(text(booking.checkOut()))
                .setGuests(booking.guests() == null ? 0 : booking.guests())
                .setStatus(text(booking.status()))
                .setTotalPrice(booking.totalPrice() == null ? 0 : booking.totalPrice())
                .setHoldUntil(text(booking.holdUntil()));
        if (booking.snapshot() != null) {
            Api.SnapshotView snapshot = booking.snapshot();
            builder.setSnapshot(SnapshotView.newBuilder()
                    .setPresent(true)
                    .setBasePrice(snapshot.basePrice())
                    .setOccupancy(snapshot.occupancyMultiplier())
                    .setSeason(snapshot.seasonMultiplier())
                    .setWeekday(snapshot.weekdayMultiplier())
                    .setDemand(snapshot.demandMultiplier())
                    .setGuest(snapshot.guestMultiplier())
                    .setCalculatedPrice(snapshot.calculatedPrice())
                    .build());
        }
        return builder.build();
    }

    public static Optional<Api.BookingView> booking(BookingView message) {
        if (!message.getFound()) {
            return Optional.empty();
        }
        Api.SnapshotView snapshot = null;
        if (message.hasSnapshot() && message.getSnapshot().getPresent()) {
            SnapshotView raw = message.getSnapshot();
            snapshot = new Api.SnapshotView(raw.getBasePrice(), raw.getOccupancy(), raw.getSeason(), raw.getWeekday(),
                    raw.getDemand(), raw.getGuest(), raw.getCalculatedPrice());
        }
        String hold = message.getHoldUntil();
        return Optional.of(new Api.BookingView(
                message.getId(),
                message.getRoomId(),
                message.getHotelId(),
                message.getCheckIn(),
                message.getCheckOut(),
                message.getGuests(),
                message.getStatus(),
                message.getTotalPrice(),
                hold.isBlank() ? null : hold,
                snapshot
        ));
    }

    public static by.bsuir.platform.contracts.grpc.BookingList bookings(List<Api.BookingView> rows) {
        by.bsuir.platform.contracts.grpc.BookingList.Builder builder = by.bsuir.platform.contracts.grpc.BookingList.newBuilder();
        for (Api.BookingView row : rows) {
            builder.addBookings(booking(row));
        }
        return builder.build();
    }

    public static List<Api.BookingView> bookings(by.bsuir.platform.contracts.grpc.BookingList message) {
        List<Api.BookingView> rows = new ArrayList<>();
        for (BookingView row : message.getBookingsList()) {
            booking(row).ifPresent(rows::add);
        }
        return rows;
    }

    public static BookingStats stats(Api.BookingStats stats) {
        return BookingStats.newBuilder()
                .setTotal(stats.total())
                .setCreated(stats.created())
                .setPaid(stats.paid())
                .setCancelled(stats.cancelled())
                .setPaidSum(stats.paidSum() == null ? 0 : stats.paidSum())
                .build();
    }

    public static Api.BookingStats stats(BookingStats message) {
        return new Api.BookingStats(message.getTotal(), message.getCreated(), message.getPaid(), message.getCancelled(), message.getPaidSum());
    }

    public static NotificationList notifications(List<Api.NotificationView> rows) {
        NotificationList.Builder builder = NotificationList.newBuilder();
        for (Api.NotificationView row : rows) {
            builder.addItems(NotificationView.newBuilder()
                    .setId(row.id())
                    .setKind(text(row.kind()))
                    .setBody(text(row.body()))
                    .setStatus(text(row.status()))
                    .setCreatedAt(text(row.createdAt()))
                    .build());
        }
        return builder.build();
    }

    public static List<Api.NotificationView> notifications(NotificationList message) {
        List<Api.NotificationView> rows = new ArrayList<>();
        for (NotificationView row : message.getItemsList()) {
            rows.add(new Api.NotificationView(row.getId(), row.getKind(), row.getBody(), row.getStatus(), row.getCreatedAt()));
        }
        return rows;
    }

    public static DialogView dialog(Api.DialogView dialog) {
        DialogView.Builder builder = DialogView.newBuilder()
                .setId(dialog.id())
                .setStatus(text(dialog.status()));
        if (dialog.messages() != null) {
            for (Api.MessageView message : dialog.messages()) {
                builder.addMessages(MessageView.newBuilder()
                        .setId(message.id())
                        .setAuthor(text(message.author()))
                        .setBody(text(message.body()))
                        .build());
            }
        }
        return builder.build();
    }

    public static Api.DialogView dialog(DialogView message) {
        List<Api.MessageView> messages = new ArrayList<>();
        for (MessageView row : message.getMessagesList()) {
            messages.add(new Api.MessageView(row.getId(), row.getAuthor(), row.getBody()));
        }
        return new Api.DialogView(message.getId(), message.getStatus(), messages);
    }

    public static OccupancyRevenue occupancy(Api.OccupancyRevenueView view) {
        return OccupancyRevenue.newBuilder()
                .setHotelId(view.hotelId())
                .setOccupancy(view.occupancy() == null ? 0 : view.occupancy())
                .setRevenue(view.revenue() == null ? 0 : view.revenue())
                .setPeriodFrom(text(view.periodFrom()))
                .setPeriodTo(text(view.periodTo()))
                .build();
    }

    public static Api.OccupancyRevenueView occupancy(OccupancyRevenue message) {
        return new Api.OccupancyRevenueView(message.getHotelId(), message.getOccupancy(), message.getRevenue(),
                message.getPeriodFrom(), message.getPeriodTo());
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }

    private static String money(BigDecimal value) {
        return value == null ? "0" : value.toPlainString();
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value == null || value.isBlank() ? "0" : value);
    }
}
