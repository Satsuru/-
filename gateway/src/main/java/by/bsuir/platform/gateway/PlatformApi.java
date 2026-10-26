package by.bsuir.platform.gateway;

import by.bsuir.platform.contracts.Api;
import by.bsuir.platform.contracts.Clients;
import by.bsuir.platform.contracts.Dates;
import by.bsuir.platform.contracts.Failures;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class PlatformApi {

    private final Clients.IdentityClient identity;
    private final Clients.CatalogClient catalog;
    private final Clients.PricingClient pricing;
    private final Clients.BookingClient booking;
    private final Clients.PaymentClient payment;
    private final Clients.NotificationClient notification;
    private final Clients.AssistantClient assistant;
    private final Clients.AnalyticsClient analytics;

    public PlatformApi(
            Clients.IdentityClient identity,
            Clients.CatalogClient catalog,
            Clients.PricingClient pricing,
            Clients.BookingClient booking,
            Clients.PaymentClient payment,
            Clients.NotificationClient notification,
            Clients.AssistantClient assistant,
            Clients.AnalyticsClient analytics
    ) {
        this.identity = identity;
        this.catalog = catalog;
        this.pricing = pricing;
        this.booking = booking;
        this.payment = payment;
        this.notification = notification;
        this.assistant = assistant;
        this.analytics = analytics;
    }

    public Api.UserView me() {
        return identity.me(CurrentUser.require());
    }

    public Api.AuthPayload register(Api.RegisterInput input) {
        return identity.register(input.email(), input.password(), input.fullName(), input.phone());
    }

    public Api.AuthPayload login(Api.LoginInput input) {
        return identity.login(input.email(), input.password());
    }

    public Api.HotelView hotel(long hotelId) {
        return catalog.hotel(hotelId).orElse(null);
    }

    public List<Api.RoomOfferView> searchHotels(Api.SearchInput input) {
        Stay stay = stay(input.checkIn(), input.checkOut(), input.guests());
        if (input.city() == null || input.city().isBlank()) {
            throw new Failures.BadRequestException("Укажите город");
        }
        List<Api.RoomOfferView> offers = new ArrayList<>();
        for (Api.RoomCard room : catalog.search(input.city().trim(), stay.checkIn(), stay.checkOut(), stay.guests(), input.minStars())) {
            offers.add(offer(room, stay));
        }
        Comparator<Api.RoomOfferView> order = Comparator.comparing(Api.RoomOfferView::nightlyPrice);
        if (input.sort() == Api.OfferSort.PRICE_DESC) {
            order = order.reversed();
        } else if (input.sort() == Api.OfferSort.RATING_DESC) {
            order = Comparator.comparing((Api.RoomOfferView row) -> row.hotel().starRating()).reversed()
                    .thenComparing(Api.RoomOfferView::nightlyPrice);
        }
        offers.sort(order);
        return offers;
    }

    public Api.QuoteView quoteStay(Api.QuoteInput input) {
        if (input.roomId() == null) {
            throw new Failures.BadRequestException("Укажите номер");
        }
        Stay stay = stay(input.checkIn(), input.checkOut(), input.guests());
        Api.RoomCard room = catalog.getRoom(input.roomId(), stay.checkIn(), stay.checkOut())
                .orElseThrow(() -> new Failures.NotFoundException("Номер не найден"));
        if (stay.guests() > room.capacity()) {
            throw new Failures.BadRequestException("В номере недостаточно мест");
        }
        if (!catalog.isFree(room.roomId(), stay.checkIn(), stay.checkOut())) {
            throw new Failures.BadRequestException("Номер занят на эти даты");
        }
        Api.PriceQuote price = pricing.calculate(room.hotelId(), room.basePrice(), room.occupancyRatio(),
                stay.checkIn(), stay.checkOut(), stay.guests());
        return quote(room, stay, price);
    }

    public Api.BookingView createBooking(Api.CreateBookingInput input) {
        long userId = CurrentUser.require();
        if (input.roomId() == null) {
            throw new Failures.BadRequestException("Укажите номер");
        }
        Stay stay = stay(input.checkIn(), input.checkOut(), input.guests());
        Api.RoomCard room = catalog.getRoom(input.roomId(), stay.checkIn(), stay.checkOut())
                .orElseThrow(() -> new Failures.NotFoundException("Номер не найден"));
        if (stay.guests() > room.capacity()) {
            throw new Failures.BadRequestException("В номере недостаточно мест");
        }
        if (!catalog.isFree(room.roomId(), stay.checkIn(), stay.checkOut())) {
            throw new Failures.BadRequestException("Номер занят на эти даты");
        }
        Api.PriceQuote price = pricing.calculate(room.hotelId(), room.basePrice(), room.occupancyRatio(),
                stay.checkIn(), stay.checkOut(), stay.guests());
        Api.BookingView created = booking.create(userId, room.roomId(), room.hotelId(), stay.checkIn(), stay.checkOut(), stay.guests(), price);
        catalog.hold(room.roomId(), stay.checkIn(), stay.checkOut(), created.id());
        notification.add(userId, "BOOKING_CREATED", "Бронь " + created.id() + " создана, номер удерживается до оплаты");
        analytics.addSale(room.hotelId(), "CREATED", price.total(), stay.checkIn());
        return created;
    }

    public Api.BookingView payBooking(Api.PayInput input) {
        long userId = CurrentUser.require();
        if (input.bookingId() == null) {
            throw new Failures.BadRequestException("Укажите бронь");
        }
        if (input.method() == null || input.method().isBlank()) {
            throw new Failures.BadRequestException("Укажите способ оплаты");
        }
        Api.BookingView paid = booking.pay(userId, input.bookingId());
        payment.complete(paid.id(), userId, java.math.BigDecimal.valueOf(paid.totalPrice()), input.method());
        catalog.confirmSold(paid.id());
        notification.add(userId, "PAYMENT_COMPLETED", "Бронь " + paid.id() + " оплачена");
        analytics.addSale(paid.hotelId(), "PAID", java.math.BigDecimal.valueOf(paid.totalPrice()), LocalDate.parse(paid.checkIn()));
        return paid;
    }

    public Api.BookingView cancelBooking(Api.CancelInput input) {
        long userId = CurrentUser.require();
        if (input.bookingId() == null) {
            throw new Failures.BadRequestException("Укажите бронь");
        }
        Api.BookingView cancelled = booking.cancel(userId, input.bookingId());
        catalog.release(cancelled.id());
        notification.add(userId, "BOOKING_CANCELLED", "Бронь " + cancelled.id() + " отменена, номер снова свободен");
        return cancelled;
    }

    public Api.DialogView askAssistant(Api.AskInput input) {
        long userId = CurrentUser.require();
        if (input.text() == null || input.text().isBlank()) {
            throw new Failures.BadRequestException("Напишите вопрос");
        }
        boolean escalated = input.text().toLowerCase(java.util.Locale.ROOT).contains("сотрудник");
        String answer = escalated
                ? "Передала вопрос сотруднику."
                : "По правилам отеля ночь считается как база × загрузка × сезон × день недели × спрос × гости. Броней у вас: "
                + booking.stats(userId).total() + ".";
        Api.DialogView dialog = assistant.ask(userId, input.dialogId(), input.text().trim(), answer, escalated);
        if (escalated) {
            notification.add(2, "ASSISTANT_ESCALATED", "Гость ждёт сотрудника, диалог " + dialog.id());
        }
        return dialog;
    }

    public List<Api.BookingView> myBookings(Api.BookingFilter filter) {
        String status = filter == null ? null : filter.status();
        Api.BookingSort sort = filter == null ? null : filter.sort();
        return booking.listMine(CurrentUser.require(), status, sort);
    }

    public Api.BookingView booking(long bookingId) {
        return booking.get(bookingId, CurrentUser.require()).orElse(null);
    }

    public Api.BookingStats myBookingStats() {
        return booking.stats(CurrentUser.require());
    }

    public List<Api.NotificationView> myNotifications() {
        return notification.listMine(CurrentUser.require());
    }

    public Api.OccupancyRevenueView occupancyAndRevenue(Api.PeriodInput input) {
        if (input.hotelId() == null) {
            throw new Failures.BadRequestException("Укажите отель");
        }
        LocalDate from = Dates.parse(input.from(), "from");
        LocalDate to = Dates.parse(input.to(), "to");
        if (to.isBefore(from)) {
            throw new Failures.BadRequestException("Конец периода раньше начала");
        }
        if (catalog.hotel(input.hotelId()).isEmpty()) {
            throw new Failures.NotFoundException("Отель не найден");
        }
        return analytics.occupancyAndRevenue(input.hotelId(), from, to);
    }

    private Api.RoomOfferView offer(Api.RoomCard room, Stay stay) {
        Api.PriceQuote price = pricing.calculate(room.hotelId(), room.basePrice(), room.occupancyRatio(),
                stay.checkIn(), stay.checkOut(), stay.guests());
        return new Api.RoomOfferView(
                room.roomId(),
                new Api.HotelView(room.hotelId(), room.hotelName(), room.city(), room.address(), room.starRating()),
                room.roomType(),
                room.capacity(),
                room.basePrice().doubleValue(),
                price.nightly().doubleValue(),
                true
        );
    }

    private Api.QuoteView quote(Api.RoomCard room, Stay stay, Api.PriceQuote price) {
        return new Api.QuoteView(
                room.roomId(),
                stay.checkIn().toString(),
                stay.checkOut().toString(),
                stay.guests(),
                room.basePrice().doubleValue(),
                price.total().doubleValue(),
                new Api.MultipliersView(price.occupancy().doubleValue(), price.season().doubleValue(),
                        price.weekday().doubleValue(), price.demand().doubleValue(), price.guest().doubleValue()),
                price.explanation()
        );
    }

    private Stay stay(String checkIn, String checkOut, Integer guests) {
        int count = guests == null ? 0 : guests;
        LocalDate in = Dates.parse(checkIn, "checkIn");
        LocalDate out = Dates.parse(checkOut, "checkOut");
        Dates.requireStay(in, out, count);
        return new Stay(in, out, count);
    }

    private record Stay(LocalDate checkIn, LocalDate checkOut, int guests) {
    }
}
