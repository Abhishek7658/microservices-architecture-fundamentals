package library_management.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Charges a fixed amount for every day a book is late. */
public class PerDayFineCalculator implements FineCalculator {
    private final double ratePerDay;

    public PerDayFineCalculator(double ratePerDay) { this.ratePerDay = ratePerDay; }

    @Override
    public double calculate(LocalDate dueDate, LocalDate returnDate) {
        long late = ChronoUnit.DAYS.between(dueDate, returnDate);
        return late > 0 ? late * ratePerDay : 0.0;
    }
}
