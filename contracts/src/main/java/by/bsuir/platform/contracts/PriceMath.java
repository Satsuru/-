package by.bsuir.platform.contracts;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

public final class PriceMath {

    private PriceMath() {
    }

    public record Band(BigDecimal min, BigDecimal max, BigDecimal multiplier) {
    }

    public record Rules(
            List<Band> occupancy,
            Map<Integer, BigDecimal> seasonByMonth,
            Map<Integer, BigDecimal> weekday,
            int windowDays,
            int viewThreshold,
            BigDecimal demandMultiplier,
            Map<LocalDate, Integer> views,
            int includedGuests,
            BigDecimal extraPerGuest
    ) {
    }

    public static Api.PriceQuote quote(
            BigDecimal base,
            BigDecimal occupancyRatio,
            LocalDate checkIn,
            LocalDate checkOut,
            int guests,
            Rules rules
    ) {
        BigDecimal occupancy = scale(band(rules.occupancy(), occupancyRatio));
        BigDecimal demand = scale(demand(rules, checkIn));
        BigDecimal guest = scale(guest(rules, guests));
        BigDecimal season = BigDecimal.ONE.setScale(3, RoundingMode.HALF_UP);
        BigDecimal weekday = BigDecimal.ONE.setScale(3, RoundingMode.HALF_UP);
        BigDecimal total = BigDecimal.ZERO;
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        for (LocalDate day = checkIn; day.isBefore(checkOut); day = day.plusDays(1)) {
            season = scale(rules.seasonByMonth().getOrDefault(day.getMonthValue(), BigDecimal.ONE));
            weekday = scale(rules.weekday().getOrDefault(day.getDayOfWeek().getValue(), BigDecimal.ONE));
            BigDecimal multiplier = occupancy.multiply(season).multiply(weekday).multiply(demand).multiply(guest);
            total = total.add(base.multiply(multiplier));
        }
        total = total.setScale(2, RoundingMode.HALF_UP);
        BigDecimal nightly = nights == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : total.divide(BigDecimal.valueOf(nights), 2, RoundingMode.HALF_UP);
        String explanation = "Ночей: " + nights
                + ". Загрузка " + occupancy
                + ", сезон " + season
                + ", день недели " + weekday
                + ", спрос " + demand
                + ", гости " + guest
                + ". Итог " + total + ".";
        return new Api.PriceQuote(nightly, total, occupancy, season, weekday, demand, guest, explanation);
    }

    private static BigDecimal band(List<Band> bands, BigDecimal ratio) {
        for (Band band : bands) {
            if (band.min().compareTo(ratio) <= 0 && band.max().compareTo(ratio) > 0) {
                return band.multiplier();
            }
        }
        return BigDecimal.ONE;
    }

    private static BigDecimal demand(Rules rules, LocalDate checkIn) {
        LocalDate from = checkIn.minusDays(rules.windowDays());
        int seen = 0;
        for (Map.Entry<LocalDate, Integer> entry : rules.views().entrySet()) {
            LocalDate day = entry.getKey();
            if (!day.isBefore(from) && day.isBefore(checkIn)) {
                seen += entry.getValue();
            }
        }
        return seen >= rules.viewThreshold() ? rules.demandMultiplier() : BigDecimal.ONE;
    }

    private static BigDecimal guest(Rules rules, int guests) {
        int extra = Math.max(0, guests - rules.includedGuests());
        return BigDecimal.ONE.add(rules.extraPerGuest().multiply(BigDecimal.valueOf(extra)));
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(3, RoundingMode.HALF_UP);
    }
}
