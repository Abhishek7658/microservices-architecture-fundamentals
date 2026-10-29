package library_management.reports;

import library_management.model.Book;
import library_management.model.IssueRecord;
import library_management.repository.Repository;

import java.util.*;
import java.util.stream.Collectors;

/** Read-only reports built from books and issue records. */
public class ReportService {
    private final Repository<Book> books;
    private final Repository<IssueRecord> issues;

    public ReportService(Repository<Book> books, Repository<IssueRecord> issues) {
        this.books = books;
        this.issues = issues;
    }

    public List<Book> availableBooks() {
        return books.findAll().stream().filter(Book::isAvailable).toList();
    }

    /** Records of books that are currently borrowed. */
    public List<IssueRecord> borrowedBooks() {
        return issues.findAll().stream().filter(i -> !i.isReturned()).toList();
    }

    /** Member ids with the number of books borrowed, highest first. */
    public List<Map.Entry<String, Long>> topBorrowers(int limit) {
        return issues.findAll().stream()
                .collect(Collectors.groupingBy(IssueRecord::getMemberId, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit).toList();
    }

    public String fineReport() {
        List<IssueRecord> fined = issues.findAll().stream().filter(i -> i.getFine() > 0).toList();
        if (fined.isEmpty()) return "No fines collected.";
        StringBuilder sb = new StringBuilder();
        double total = 0;
        for (IssueRecord r : fined) {
            sb.append(String.format("%s | member %s | book %s | fine %.2f%n", r.getId(), r.getMemberId(), r.getBookId(), r.getFine()));
            total += r.getFine();
        }
        return sb.append(String.format("TOTAL FINES: %.2f", total)).toString();
    }
}
