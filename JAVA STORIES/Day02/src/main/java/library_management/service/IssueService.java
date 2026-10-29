package library_management.service;

import library_management.exceptions.BookUnavailableException;
import library_management.exceptions.IssueNotFoundException;
import library_management.exceptions.ValidationException;
import library_management.model.Book;
import library_management.model.IssueRecord;
import library_management.repository.Repository;

import java.time.LocalDate;
import java.util.List;

/** Issue and return books, and calculate fines. */
public class IssueService {
    public static final int LOAN_DAYS = 14;

    private final BookService bookService;
    private final MemberService memberService;
    private final Repository<Book> books;
    private final Repository<IssueRecord> issues;
    private final FineCalculator fineCalculator;

    public IssueService(BookService bookService, MemberService memberService, Repository<Book> books,
                        Repository<IssueRecord> issues, FineCalculator fineCalculator) {
        this.bookService = bookService;
        this.memberService = memberService;
        this.books = books;
        this.issues = issues;
        this.fineCalculator = fineCalculator;
    }

    public IssueRecord issueBook(String bookId, String memberId) {
        return issueBook(bookId, memberId, LocalDate.now());
    }

    public IssueRecord issueBook(String bookId, String memberId, LocalDate today) {
        memberService.validate(memberId, today);
        Book book = bookService.get(bookId);
        if (!book.isAvailable()) throw new BookUnavailableException("No copies left of '" + book.getTitle() + "'");
        boolean alreadyHas = issues.findAll().stream()
                .anyMatch(i -> !i.isReturned() && i.getBookId().equals(bookId) && i.getMemberId().equals(memberId));
        if (alreadyHas) throw new ValidationException("Member already has this book");

        book.checkOut();
        books.save(book);
        IssueRecord rec = new IssueRecord(bookId, memberId, today, today.plusDays(LOAN_DAYS));
        issues.save(rec);
        return rec;
    }

    public IssueRecord returnBook(String issueId) {
        return returnBook(issueId, LocalDate.now());
    }

    public IssueRecord returnBook(String issueId, LocalDate returnDate) {
        IssueRecord rec = issues.findById(issueId)
                .orElseThrow(() -> new IssueNotFoundException("No issue with id " + issueId));
        if (rec.isReturned()) throw new IssueNotFoundException("Issue " + issueId + " is already returned");
        rec.markReturned(returnDate, fineCalculator.calculate(rec.getDueDate(), returnDate));
        issues.save(rec);
        Book book = bookService.get(rec.getBookId());
        book.checkIn();
        books.save(book);
        return rec;
    }

    public List<IssueRecord> all() { return issues.findAll(); }
}
