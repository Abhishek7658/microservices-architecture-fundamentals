package library_management.model;

import java.time.LocalDate;

/** Records one borrowing of a book by a member. */
public class IssueRecord extends Entity {
    private final String bookId;
    private final String memberId;
    private final LocalDate issueDate;
    private final LocalDate dueDate;
    private LocalDate returnDate;
    private double fine;

    public IssueRecord(String bookId, String memberId, LocalDate issueDate, LocalDate dueDate) {
        super("I");
        this.bookId = bookId;
        this.memberId = memberId;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
    }

    public String getBookId() { return bookId; }
    public String getMemberId() { return memberId; }
    public LocalDate getIssueDate() { return issueDate; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalDate getReturnDate() { return returnDate; }
    public double getFine() { return fine; }
    public boolean isReturned() { return returnDate != null; }

    public void markReturned(LocalDate date, double fine) {
        this.returnDate = date;
        this.fine = fine;
    }

    @Override
    public String toString() {
        return String.format("%s | book %s | member %s | issued %s | due %s | %s",
                id, bookId, memberId, issueDate, dueDate,
                isReturned() ? "returned " + returnDate + " fine " + fine : "NOT RETURNED");
    }
}
