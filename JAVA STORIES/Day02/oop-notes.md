# OOP Notes (Day 02)

- **Encapsulation** – model fields are private; state changes go through methods (`Book.checkOut()`).
- **Inheritance** – `Book`, `Member`, `IssueRecord` extend abstract `Entity` (shared id).
- **Abstraction / Interfaces** – `Repository<T>` and `FineCalculator` hide implementation details.
- **Polymorphism** – services depend on `Repository` / `FineCalculator`; swap `JsonRepository` or `PerDayFineCalculator` freely.
- **Composition** – `IssueService` is composed of `BookService`, `MemberService`, repositories and a `FineCalculator`.
- **Generics & Collections** – `JsonRepository<T extends Entity>`, `LinkedHashMap`, streams for search and reports.
- **Exceptions** – custom `LibraryException` hierarchy; app catches and prints friendly messages.
- **Separation of responsibilities** – app (UI) / service (rules) / repository (storage) / reports / exceptions / model.
