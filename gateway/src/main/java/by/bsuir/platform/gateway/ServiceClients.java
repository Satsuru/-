package by.bsuir.platform.gateway;

import by.bsuir.platform.analytics.AnalyticsService;
import by.bsuir.platform.assistant.AssistantService;
import by.bsuir.platform.booking.BookingService;
import by.bsuir.platform.catalog.CatalogService;
import by.bsuir.platform.contracts.Api;
import by.bsuir.platform.contracts.Clients;
import by.bsuir.platform.notification.NotificationService;
import by.bsuir.platform.payment.PaymentService;
import by.bsuir.platform.pricing.PricingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Замена будущего gRPC: интерфейс клиента вызывает класс сервиса и его базу.
 * Сервис сам интерфейс клиента не реализует.
 */
@Configuration
public class ServiceClients {

    @Bean
    public Clients.CatalogClient catalogClient(CatalogService catalog) {
        return new Clients.CatalogClient() {
            @Override
            public Optional<Api.HotelView> hotel(long hotelId) {
                return catalog.hotel(hotelId);
            }

            @Override
            public List<Api.RoomCard> search(String city, LocalDate checkIn, LocalDate checkOut, int guests, Double minStars) {
                return catalog.search(city, checkIn, checkOut, guests, minStars);
            }

            @Override
            public Optional<Api.RoomCard> getRoom(long roomId, LocalDate checkIn, LocalDate checkOut) {
                return catalog.getRoom(roomId, checkIn, checkOut);
            }

            @Override
            public boolean isFree(long roomId, LocalDate checkIn, LocalDate checkOut) {
                return catalog.isFree(roomId, checkIn, checkOut);
            }

            @Override
            public int activeRooms(long hotelId) {
                return catalog.activeRooms(hotelId);
            }

            @Override
            public void hold(long roomId, LocalDate checkIn, LocalDate checkOut, long bookingId) {
                catalog.hold(roomId, checkIn, checkOut, bookingId);
            }

            @Override
            public void release(long bookingId) {
                catalog.release(bookingId);
            }

            @Override
            public void confirmSold(long bookingId) {
                catalog.confirmSold(bookingId);
            }
        };
    }

    @Bean
    public Clients.PricingClient pricingClient(PricingService pricing) {
        return pricing::calculate;
    }

    @Bean
    public Clients.BookingClient bookingClient(BookingService booking) {
        return new Clients.BookingClient() {
            @Override
            public List<Api.BookingView> listMine(long userId, String status, Api.BookingSort sort) {
                return booking.listMine(userId, status, sort);
            }

            @Override
            public Optional<Api.BookingView> get(long bookingId, long userId) {
                return Optional.ofNullable(booking.get(bookingId, userId));
            }

            @Override
            public Api.BookingStats stats(long userId) {
                return booking.stats(userId);
            }

            @Override
            public Api.BookingView create(long userId, long roomId, long hotelId, LocalDate checkIn, LocalDate checkOut,
                                          int guests, BigDecimal basePrice, Api.PriceQuote quote) {
                return booking.create(userId, roomId, hotelId, checkIn, checkOut, guests, basePrice, quote);
            }

            @Override
            public Api.BookingView pay(long userId, long bookingId) {
                return booking.pay(userId, bookingId);
            }

            @Override
            public Api.BookingView cancel(long actorId, boolean staff, long bookingId) {
                return booking.cancel(actorId, staff, bookingId);
            }

            @Override
            public void abort(long bookingId) {
                booking.abort(bookingId);
            }

            @Override
            public List<Long> expireOverdue() {
                return booking.expireOverdue();
            }
        };
    }

    @Bean
    public Clients.PaymentClient paymentClient(PaymentService payment) {
        return (bookingId, userId, amount, method) -> payment.complete(bookingId, userId, amount, method);
    }

    @Bean
    public Clients.NotificationClient notificationClient(NotificationService notification) {
        return new Clients.NotificationClient() {
            @Override
            public List<Api.NotificationView> listMine(long userId) {
                return notification.listMine(userId);
            }

            @Override
            public void add(long userId, String kind, String body) {
                notification.add(userId, kind, body);
            }
        };
    }

    @Bean
    public Clients.AssistantClient assistantClient(AssistantService assistant) {
        return assistant::ask;
    }

    @Bean
    public Clients.AnalyticsClient analyticsClient(AnalyticsService analytics) {
        return new Clients.AnalyticsClient() {
            @Override
            public Api.OccupancyRevenueView occupancyAndRevenue(long hotelId, LocalDate from, LocalDate to) {
                return analytics.occupancyAndRevenue(hotelId, from, to);
            }

            @Override
            public void addSale(long hotelId, String status, BigDecimal amount, LocalDate day) {
                analytics.addSale(hotelId, status, amount, day);
            }

            @Override
            public void recordSoldNights(long hotelId, LocalDate checkIn, LocalDate checkOut, int totalRooms) {
                analytics.recordSoldNights(hotelId, checkIn, checkOut, totalRooms);
            }
        };
    }
}
