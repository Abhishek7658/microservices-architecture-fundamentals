package library_management.model;

import java.util.UUID;

/** Base class for every persisted object; provides a unique id. */
public abstract class Entity {
    protected String id;

    protected Entity(String prefix) {
        this.id = prefix + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    /** @return the unique id */
    public String getId() { return id; }
}
