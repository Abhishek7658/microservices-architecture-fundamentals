package library_management;

import library_management.exceptions.*;
import library_management.model.*;
import library_management.reports.ReportService;
import library_management.repository.JsonRepository;
import library_management.repository.Repository;
import library_management.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class LibraryTest {
    @TempDir Path dir;
    Repository<Book> bookRepo;
    Repository<Member> memberRepo;
    Repository<IssueRecord> issueRepo;
    BookService books;
    MemberService members;
    IssueService issues;
    ReportService reports;

    @BeforeEach
    void setUp() {
        bookRepo = new JsonRepository<>(dir.resolve("books.json"), Book.class);
        memberRepo = new JsonRepository<>(dir.resolve("members.json"), Member.class);
        issueRepo = new JsonRepository<>(dir.resolve("issues.json"), IssueRecord.class);
        books = new BookService(bookRepo);
        members = new MemberService(memberRepo);
        issues = new IssueService(books, members, bookRepo, issueRepo, new PerDayFineCalculator(2.0));
        reports = new ReportService(bookRepo, issueRepo);
    }

    @Test
    void issueAndReturnOnTimeHasNoFine() {
        Book b = books.add("Clean Code", "Robert Martin", 1);
        Member m = members.register("Abhi", "abhi@example.com", 6);
        IssueRecord r = issues.issueBook(b.getId(), m.getId());
        assertEquals(0, books.get(b.getId()).getAvailableCopies());
        issues.returnBook(r.getId());
        assertEquals(1, books.get(b.getId()).getAvailableCopies());
        assertEquals(0.0, issueRepo.findById(r.getId()).get().getFine());
    }

    @Test
    void lateReturnCalculatesFine() {
        Book b = books.add("Effective Java", "Joshua Bloch", 1);
        Member m = members.register("Abhi", "abhi@example.com", 6);
        LocalDate today = LocalDate.now();
        IssueRecord r = issues.issueBook(b.getId(), m.getId(), today);
        IssueRecord back = issues.returnBook(r.getId(), today.plusDays(IssueService.LOAN_DAYS + 3));
        assertEquals(6.0, back.getFine());
        assertTrue(reports.fineReport().contains("TOTAL FINES: 6.00"));
    }

    @Test
    void unavailableBookIsRejected() {
        Book b = books.add("DDIA", "Martin Kleppmann", 1);
        Member m1 = members.register("A", "a@x.com", 6);
        Member m2 = members.register("B", "b@x.com", 6);
        issues.issueBook(b.getId(), m1.getId());
        assertThrows(BookUnavailableException.class, () -> issues.issueBook(b.getId(), m2.getId()));
    }

    @Test
    void expiredMembershipIsRejected() {
        Book b = books.add("Java", "Oracle", 1);
        Member m = members.register("A", "a@x.com", 1);
        assertThrows(InvalidMembershipException.class,
                () -> issues.issueBook(b.getId(), m.getId(), LocalDate.now().plusMonths(2)));
    }

    @Test
    void validationAndNotFound() {
        assertThrows(ValidationException.class, () -> books.add(" ", "x", 1));
        assertThrows(ValidationException.class, () -> members.register("A", "bad-email", 1));
        assertThrows(BookNotFoundException.class, () -> books.get("nope"));
        assertThrows(MemberNotFoundException.class, () -> members.get("nope"));
    }

    @Test
    void searchAndPersistence() {
        books.add("Clean Code", "Robert Martin", 2);
        assertEquals(1, books.search("martin").size());
        Repository<Book> reloaded = new JsonRepository<>(dir.resolve("books.json"), Book.class);
        assertEquals(1, reloaded.findAll().size());
    }

    @Test
    void topBorrowersOrdersByCount() {
        Book b1 = books.add("A", "X", 5);
        Book b2 = books.add("B", "Y", 5);
        Member m1 = members.register("One", "1@x.com", 6);
        Member m2 = members.register("Two", "2@x.com", 6);
        issues.issueBook(b1.getId(), m1.getId());
        issues.issueBook(b2.getId(), m1.getId());
        issues.issueBook(b1.getId(), m2.getId());
        assertEquals(m1.getId(), reports.topBorrowers(1).get(0).getKey());
    }
}
