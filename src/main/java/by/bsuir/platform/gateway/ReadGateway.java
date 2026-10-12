package by.bsuir.platform.gateway;

import by.bsuir.platform.api.Contracts.AnalyticsPort;
import by.bsuir.platform.api.Contracts.BookingPort;
import by.bsuir.platform.api.Contracts.BookingView;
import by.bsuir.platform.api.Contracts.CatalogPort;
import by.bsuir.platform.api.Contracts.HotelView;
import by.bsuir.platform.api.Contracts.IdentityPort;
import by.bsuir.platform.api.Contracts.MultipliersView;
import by.bsuir.platform.api.Contracts.NotificationPort;
import by.bsuir.platform.api.Contracts.NotificationView;
import by.bsuir.platform.api.Contracts.OccupancyRevenueView;
import by.bsuir.platform.api.Contracts.PeriodInput;
import by.bsuir.platform.api.Contracts.PriceQuote;
import by.bsuir.platform.api.Contracts.PricingPort;
import by.bsuir.platform.api.Contracts.QuoteInput;
import by.bsuir.platform.api.Contracts.QuoteView;
import by.bsuir.platform.api.Contracts.RoomCard;
import by.bsuir.platform.api.Contracts.RoomOfferView;
import by.bsuir.platform.api.Contracts.SearchInput;
import by.bsuir.platform.api.Contracts.UserView;
import by.bsuir.platform.common.BadRequestException;
import by.bsuir.platform.common.CurrentUser;
import by.bsuir.platform.common.Dates;
import by.bsuir.platform.common.NotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ReadGateway {

    private final IdentityPort identity;
    private final CatalogPort catalog;
    private final PricingPort pricing;
    private final BookingPort booking;
    private final NotificationPort notification;
    private final AnalyticsPort analytics;

    public ReadGateway(
            IdentityPort identity,
            CatalogPort catalog,
            PricingPort pricing,
            BookingPort booking,
            NotificationPort notification,
            AnalyticsPort analytics
    ) {
        this.identity = identity;
        this.catalog = catalog;
        this.pricing = pricing;
        this.booking = booking;
        this.notification = notification;
        this.analytics = analytics;
    }

    public UserView me() {
        return identity.me(CurrentUser.require());
    }

    public HotelView hotel(long hotelId) {
        return catalog.hotel(hotelId).orElse(null);
    }

    public List<RoomOfferView> searchHotels(SearchInput input) {
        Stay stay = stay(input.checkIn(), input.checkOut(), input.guests());
        if (input.city() == null || input.city().isBlank()) {
            throw new BadRequestException("Укажите город");
        }
        return catalog.search(input.city().trim(), stay.checkIn(), stay.checkOut(), stay.guests())
                .stream()
                .map(room -> offer(room, stay))
                .toList();
    }

    public QuoteView quoteStay(QuoteInput input) {
        if (input.roomId() == null) {
            throw new BadRequestException("Укажите номер");
        }
        Stay stay = stay(input.checkIn(), input.checkOut(), input.guests());
        RoomCard room = catalog.getRoom(input.roomId(), stay.checkIn(), stay.checkOut())
                .orElseThrow(() -> new NotFoundException("Номер не найден"));
        if (stay.guests() > room.capacity()) {
            throw new BadRequestException("В номере недостаточно мест");
        }
        if (!catalog.isFree(room.roomId(), stay.checkIn(), stay.checkOut())) {
            throw new BadRequestException("Номер занят на эти даты");
        }
        PriceQuote price = pricing.calculate(
                room.hotelId(), room.basePrice(), room.occupancyRatio(),
                stay.checkIn(), stay.checkOut(), stay.guests()
        );
        return new QuoteView(
                room.roomId(),
                stay.checkIn().toString(),
                stay.checkOut().toString(),
                stay.guests(),
                room.basePrice().doubleValue(),
                price.total().doubleValue(),
                multipliers(price),
                price.explanation()
        );
    }

    public List<BookingView> myBookings() {
        return booking.listMine(CurrentUser.require());
    }

    public BookingView booking(long bookingId) {
        return booking.get(bookingId, CurrentUser.require()).orElse(null);
    }

    public List<NotificationView> myNotifications() {
        return notification.listMine(CurrentUser.require());
    }

    public OccupancyRevenueView occupancyAndRevenue(PeriodInput input) {
        if (input.hotelId() == null) {
            throw new BadRequestException("Укажите отель");
        }
        LocalDate from = Dates.parse(input.from(), "from");
        LocalDate to = Dates.parse(input.to(), "to");
        if (to.isBefore(from)) {
            throw new BadRequestException("Конец периода раньше начала");
        }
        if (catalog.hotel(input.hotelId()).isEmpty()) {
            throw new NotFoundException("Отель не найден");
        }
        return analytics.occupancyAndRevenue(input.hotelId(), from, to);
    }

    private RoomOfferView offer(RoomCard room, Stay stay) {
        PriceQuote price = pricing.calculate(
                room.hotelId(), room.basePrice(), room.occupancyRatio(),
                stay.checkIn(), stay.checkOut(), stay.guests()
        );
        return new RoomOfferView(
                room.roomId(),
                new HotelView(room.hotelId(), room.hotelName(), room.city(), room.address(), room.starRating()),
                room.roomType(),
                room.capacity(),
                room.basePrice().doubleValue(),
                price.nightly().doubleValue(),
                true
        );
    }

    private MultipliersView multipliers(PriceQuote price) {
        return new MultipliersView(
                price.occupancy().doubleValue(),
                price.season().doubleValue(),
                price.weekday().doubleValue(),
                price.demand().doubleValue(),
                price.guest().doubleValue()
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
