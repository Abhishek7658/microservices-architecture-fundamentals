package library_management.model;

/** A book with a number of physical copies. */
public class Book extends Entity {
    private String title;
    private String author;
    private int totalCopies;
    private int availableCopies;

    public Book(String title, String author, int totalCopies) {
        super("B");
        this.title = title;
        this.author = author;
        this.totalCopies = totalCopies;
        this.availableCopies = totalCopies;
    }

    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public int getTotalCopies() { return totalCopies; }
    public int getAvailableCopies() { return availableCopies; }
    public boolean isAvailable() { return availableCopies > 0; }

    public void setTitle(String title) { this.title = title; }
    public void setAuthor(String author) { this.author = author; }

    /** Changes total copies, keeping the number currently borrowed unchanged. */
    public void setTotalCopies(int newTotal) {
        int borrowed = totalCopies - availableCopies;
        this.totalCopies = newTotal;
        this.availableCopies = newTotal - borrowed;
    }

    public void checkOut() { availableCopies--; }
    public void checkIn() { availableCopies++; }

    @Override
    public String toString() {
        return String.format("%s | %s by %s | %d/%d available", id, title, author, availableCopies, totalCopies);
    }
}
