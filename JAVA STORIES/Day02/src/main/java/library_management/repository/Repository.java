package library_management.repository;

import library_management.model.Entity;
import java.util.List;
import java.util.Optional;

/** Generic storage abstraction. */
public interface Repository<T extends Entity> {
    void save(T entity);
    Optional<T> findById(String id);
    List<T> findAll();
    boolean deleteById(String id);
}
