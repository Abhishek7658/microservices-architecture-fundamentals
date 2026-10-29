package library_management.app;

import library_management.exceptions.LibraryException;
import library_management.model.Book;
import library_management.model.IssueRecord;
import library_management.model.Member;
import library_management.reports.ReportService;
import library_management.repository.JsonRepository;
import library_management.repository.Repository;
import library_management.service.*;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Scanner;

/** Console entry point. Run with: mvn exec:java */
public class LibraryApp {
    public static void main(String[] args) {
        Path data = Path.of("data");
        Repository<Book> bookRepo = new JsonRepository<>(data.resolve("books.json"), Book.class);
        Repository<Member> memberRepo = new JsonRepository<>(data.resolve("members.json"), Member.class);
        Repository<IssueRecord> issueRepo = new JsonRepository<>(data.resolve("issues.json"), IssueRecord.class);

        BookService bookService = new BookService(bookRepo);
        MemberService memberService = new MemberService(memberRepo);
        IssueService issueService = new IssueService(bookService, memberService, bookRepo, issueRepo,
                new PerDayFineCalculator(2.0));
        ReportService reports = new ReportService(bookRepo, issueRepo);

        Scanner in = new Scanner(System.in);
        while (true) {
            System.out.println("""

                    ===== LIBRARY MANAGEMENT =====
                    1. Add book          6. Register member     11. Issue book
                    2. Update book       7. Update member       12. Return book
                    3. Delete book       8. Delete member       13. Available books
                    4. Search books      9. List members        14. Borrowed books
                    5. List books       10. Validate member     15. Top borrowers
                                                                16. Fine report
                    0. Exit""");
            System.out.print("Choice: ");
            String choice = in.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> System.out.println("Added: " + bookService.add(ask(in, "Title"), ask(in, "Author"), Integer.parseInt(ask(in, "Copies"))));
                    case "2" -> System.out.println("Updated: " + bookService.update(ask(in, "Book id"), ask(in, "Title"), ask(in, "Author"), Integer.parseInt(ask(in, "Total copies"))));
                    case "3" -> { bookService.delete(ask(in, "Book id")); System.out.println("Deleted."); }
                    case "4" -> bookService.search(ask(in, "Keyword")).forEach(System.out::println);
                    case "5" -> bookService.all().forEach(System.out::println);
                    case "6" -> System.out.println("Registered: " + memberService.register(ask(in, "Name"), ask(in, "Email"), Integer.parseInt(ask(in, "Membership months"))));
                    case "7" -> System.out.println("Updated: " + memberService.update(ask(in, "Member id"), ask(in, "Name"), ask(in, "Email")));
                    case "8" -> { memberService.delete(ask(in, "Member id")); System.out.println("Deleted."); }
                    case "9" -> memberService.all().forEach(System.out::println);
                    case "10" -> { memberService.validate(ask(in, "Member id"), LocalDate.now()); System.out.println("Membership is valid."); }
                    case "11" -> System.out.println("Issued: " + issueService.issueBook(ask(in, "Book id"), ask(in, "Member id")));
                    case "12" -> System.out.println("Returned: " + issueService.returnBook(ask(in, "Issue id")));
                    case "13" -> reports.availableBooks().forEach(System.out::println);
                    case "14" -> reports.borrowedBooks().forEach(System.out::println);
                    case "15" -> reports.topBorrowers(5).forEach(e -> System.out.println(e.getKey() + " -> " + e.getValue() + " books"));
                    case "16" -> System.out.println(reports.fineReport());
                    case "0" -> { System.out.println("Bye!"); return; }
                    default -> System.out.println("Invalid choice.");
                }
            } catch (LibraryException e) {
                System.out.println("Error: " + e.getMessage());
            } catch (NumberFormatException e) {
                System.out.println("Error: please enter a valid number.");
            }
        }
    }

    private static String ask(Scanner in, String label) {
        System.out.print(label + ": ");
        return in.nextLine();
    }
}
