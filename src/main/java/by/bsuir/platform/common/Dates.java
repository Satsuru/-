package by.bsuir.platform.common;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public final class Dates {

    private Dates() {
    }

    public static LocalDate parse(String raw, String field) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException("Заполните поле " + field);
        }
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException ex) {
            throw new BadRequestException("Поле " + field + " должно быть датой ГГГГ-ММ-ДД");
        }
    }

    public static void requireStay(LocalDate checkIn, LocalDate checkOut, int guests) {
        if (!checkOut.isAfter(checkIn)) {
            throw new BadRequestException("Дата выезда должна быть позже даты заезда");
        }
        if (guests < 1) {
            throw new BadRequestException("Гостей должно быть хотя бы один");
        }
    }
}
