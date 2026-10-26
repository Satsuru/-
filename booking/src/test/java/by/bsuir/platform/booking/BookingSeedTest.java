package by.bsuir.platform.booking;

import by.bsuir.platform.contracts.Failures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class BookingSeedTest {

    @Autowired
    BookingService bookings;

    @Test
    void storedSumMatchesBaseDurationAndMultipliers() {
        var booking = bookings.get(1, 1);
        var snapshot = booking.snapshot();
        BigDecimal total = BigDecimal.valueOf(snapshot.basePrice())
                .multiply(BigDecimal.valueOf(snapshot.occupancyMultiplier()))
                .multiply(BigDecimal.valueOf(snapshot.seasonMultiplier()))
                .multiply(BigDecimal.valueOf(snapshot.weekdayMultiplier()))
                .multiply(BigDecimal.valueOf(snapshot.demandMultiplier()))
                .multiply(BigDecimal.valueOf(snapshot.guestMultiplier()))
                .multiply(BigDecimal.valueOf(2));
        assertEquals(0, total.setScale(2, java.math.RoundingMode.HALF_UP).compareTo(new BigDecimal("312.00")));
        assertEquals(312.0, booking.totalPrice());
        assertThrows(Failures.BadRequestException.class, () -> bookings.pay(1, 2));
    }
}
