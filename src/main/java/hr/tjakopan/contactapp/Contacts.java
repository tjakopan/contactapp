package hr.tjakopan.contactapp;

import hr.tjakopan.contactapp.ValidationError.EmailValidationError.EmailIsNotUnique;
import hr.tjakopan.contactapp.ValidationError.EmailValidationError.EmailRequired;
import io.smallrye.mutiny.Uni;
import io.vertx.core.file.FileSystemException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.core.file.FileSystem;
import org.jspecify.annotations.Nullable;

import java.nio.file.NoSuchFileException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public final class Contacts {
  private final FileSystem fileSystem;
  private final Map<Integer, ValidContact> db;

  public Contacts(final Vertx vertx) {
    this.fileSystem = vertx.fileSystem();
    this.db = new ConcurrentHashMap<>();
  }

  public Uni<ValidContact> validate(final Contact contact) {
    final String email = contact.email();

    if (email == null || email.isEmpty()) {
      return Uni.createFrom().failure(new EmailRequired());
    }
    final boolean emailIsUnique = db.values()
      .stream()
      .noneMatch(c -> !Objects.equals(c.id(), contact.id()) && Objects.equals(c.email(), contact.email()));
    if (!emailIsUnique) {
      return Uni.createFrom().failure(new EmailIsNotUnique());
    }
    final ValidContact validContact =
      new ValidContact(contact.id(), contact.firstName(), contact.lastName(), contact.phone(), email);
    return Uni.createFrom().item(validContact);
  }

  public Uni<Void> save(final Contact contact) {
    return validate(contact)
      .onItem().transformToUni(c -> {
        final Integer id = c.id();
        if (id == null) {
          final int newId;
          if (db.isEmpty()) {
            newId = 1;
          } else {
            newId = db.keySet().stream().max(Integer::compareTo).get() + 1;
          }
          final ValidContact newContact =
            new ValidContact(newId, c.firstName(), c.lastName(), c.phone(), c.email());
          db.put(newId, newContact);
        } else {
          db.put(id, c);
        }
        return saveDb();
      });
  }

  public Uni<Void> delete(final ValidContact contact) {
    final Integer id = contact.id();
    if (id == null) {
      return Uni.createFrom().voidItem();
    }
    db.remove(id);
    return saveDb();
  }

  public Uni<Integer> count() {
    return Uni.createFrom().item(db.size())
      .onItem().delayIt().by(Duration.ofSeconds(2));
  }

  public Uni<List<ValidContact>> all() {
    return Uni.createFrom().item(db.values().stream().toList());
  }

  public Uni<List<ValidContact>> search(final String text) {
    final Predicate<@Nullable String> containsText = s -> s != null && s.contains(text);
    final Predicate<ValidContact> anyFieldContainsText = c -> containsText.test(c.firstName())
      || containsText.test(c.lastName()) || containsText.test(c.phone()) || containsText.test(c.email());
    return Uni.createFrom().item(db.values().stream().filter(anyFieldContainsText).toList());
  }

  public Uni<Void> loadDb() {
    final Predicate<Throwable> isNoSuchFileException =
      e -> (e instanceof FileSystemException) && (e.getCause() instanceof NoSuchFileException);
    return fileSystem
      .readFile("contacts.json")
      .onFailure(isNoSuchFileException).recoverWithItem(Buffer.buffer("[]"))
      .onItem().transform(buffer -> new JsonArray(buffer.getDelegate()))
      .onItem().invoke(jsonArray -> {
        db.clear();
        jsonArray.stream()
          .map(json -> (JsonObject) json)
          .map(ValidContact::fromJson)
          .forEach(contact -> {
            final Integer id = contact.id();
            if (id != null) {
              db.put(id, contact);
            }
          });
      })
      .replaceWithVoid();
  }

  private Uni<Void> saveDb() {
    final JsonArray jsonArray = new JsonArray();
    db.values().stream().map(ValidContact::toJson).forEach(jsonArray::add);
    return fileSystem.writeFile("contacts.json", Buffer.buffer(jsonArray.encodePrettily()))
      .replaceWithVoid();
  }

  public Uni<@Nullable ValidContact> find(final int id) {
    return Uni.createFrom().item(db.get(id));
  }
}
