package library_management.service;

import library_management.exceptions.BookNotFoundException;
import library_management.exceptions.ValidationException;
import library_management.model.Book;
import library_management.repository.Repository;

import java.util.List;

/** Add, update, delete and search books. */
public class BookService {
    private final Repository<Book> books;

    public BookService(Repository<Book> books) { this.books = books; }

    public Book add(String title, String author, int copies) {
        Book b = new Book(Validators.text("Title", title), Validators.text("Author", author),
                Validators.positive("Copies", copies));
        books.save(b);
        return b;
    }

    public Book get(String id) {
        return books.findById(id).orElseThrow(() -> new BookNotFoundException("No book with id " + id));
    }

    public Book update(String id, String title, String author, int totalCopies) {
        Book b = get(id);
        int borrowed = b.getTotalCopies() - b.getAvailableCopies();
        if (Validators.positive("Copies", totalCopies) < borrowed)
            throw new ValidationException("Cannot set copies below currently borrowed (" + borrowed + ")");
        b.setTitle(Validators.text("Title", title));
        b.setAuthor(Validators.text("Author", author));
        b.setTotalCopies(totalCopies);
        books.save(b);
        return b;
    }

    public void delete(String id) {
        Book b = get(id);
        if (b.getAvailableCopies() != b.getTotalCopies())
            throw new ValidationException("Cannot delete a book that is currently borrowed");
        books.deleteById(id);
    }

    /** Case-insensitive search on title or author. */
    public List<Book> search(String keyword) {
        String k = Validators.text("Keyword", keyword).toLowerCase();
        return books.findAll().stream()
                .filter(b -> b.getTitle().toLowerCase().contains(k) || b.getAuthor().toLowerCase().contains(k))
                .toList();
    }

    public List<Book> all() { return books.findAll(); }
}
