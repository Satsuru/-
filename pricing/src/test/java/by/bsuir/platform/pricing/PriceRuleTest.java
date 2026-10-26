package by.bsuir.platform.pricing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class PriceRuleTest {

    @Autowired
    PricingService pricing;

    @Test
    void juneStayWithHalfOccupancyIs312() {
        var quote = pricing.calculate(1, new BigDecimal("100.00"), new BigDecimal("0.500"),
                LocalDate.parse("2026-06-10"), LocalDate.parse("2026-06-12"), 2);
        assertEquals(0, quote.total().compareTo(new BigDecimal("312.00")));
        assertEquals(0, quote.occupancy().compareTo(new BigDecimal("1.300")));
        assertEquals(0, quote.demand().compareTo(new BigDecimal("1.000")));
    }

    @Test
    void earlyJuneQuoteStays264() {
        var quote = pricing.calculate(1, new BigDecimal("100.00"), new BigDecimal("0.250"),
                LocalDate.parse("2026-06-01"), LocalDate.parse("2026-06-03"), 2);
        assertEquals(0, quote.total().compareTo(new BigDecimal("264.00")));
        assertEquals(0, quote.demand().compareTo(new BigDecimal("1.100")));
    }
}
