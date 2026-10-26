package by.bsuir.platform.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
class HoldSeedTest {

    @Autowired
    CatalogService catalog;

    @Test
    void booking1HoldsBothNightsAndBlocksThem() {
        assertEquals(2, catalog.heldNights(1));
        assertFalse(catalog.isFree(1, LocalDate.parse("2026-06-10"), LocalDate.parse("2026-06-12")));
    }
}
