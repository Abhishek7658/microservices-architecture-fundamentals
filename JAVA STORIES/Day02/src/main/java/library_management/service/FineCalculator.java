package library_management.service;

import java.time.LocalDate;

/** Strategy for computing late fines. */
public interface FineCalculator {
    double calculate(LocalDate dueDate, LocalDate returnDate);
}
