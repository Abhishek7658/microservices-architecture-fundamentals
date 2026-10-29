package library_management.repository;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import library_management.exceptions.LibraryException;
import library_management.model.Entity;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;

/** Repository that keeps entities in memory and persists them to a JSON file using Gson. */
public class JsonRepository<T extends Entity> implements Repository<T> {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(LocalDate.class,
                    (JsonSerializer<LocalDate>) (src, t, c) -> new JsonPrimitive(src.toString()))
            .registerTypeAdapter(LocalDate.class,
                    (JsonDeserializer<LocalDate>) (json, t, c) -> LocalDate.parse(json.getAsString()))
            .create();

    private final Path file;
    private final Class<T> type;
    private final Map<String, T> store = new LinkedHashMap<>();

    public JsonRepository(Path file, Class<T> type) {
        this.file = file;
        this.type = type;
        load();
    }

    private void load() {
        if (!Files.exists(file)) return;
        Type listType = TypeToken.getParameterized(List.class, type).getType();
        try (Reader r = Files.newBufferedReader(file)) {
            List<T> items = GSON.fromJson(r, listType);
            if (items != null) items.forEach(i -> store.put(i.getId(), i));
        } catch (IOException | JsonParseException e) {
            throw new LibraryException("Could not read " + file, e);
        }
    }

    private void persist() {
        try {
            if (file.getParent() != null) Files.createDirectories(file.getParent());
            try (Writer w = Files.newBufferedWriter(file)) {
                GSON.toJson(new ArrayList<>(store.values()), w);
            }
        } catch (IOException e) {
            throw new LibraryException("Could not write " + file, e);
        }
    }

    @Override public void save(T e) { store.put(e.getId(), e); persist(); }
    @Override public Optional<T> findById(String id) { return Optional.ofNullable(store.get(id)); }
    @Override public List<T> findAll() { return new ArrayList<>(store.values()); }
    @Override public boolean deleteById(String id) {
        boolean removed = store.remove(id) != null;
        if (removed) persist();
        return removed;
    }
}
