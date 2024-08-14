package hr.tjakopan.contactapp;

import io.vertx.core.json.JsonObject;
import org.jspecify.annotations.Nullable;

public record ValidContact(@Nullable Integer id, @Nullable String firstName, @Nullable String lastName,
                           @Nullable String phone, String email) {
  public ValidContact(@Nullable String firstName, @Nullable String lastName, @Nullable String phone, String email) {
    this(null, firstName, lastName, phone, email);
  }

  public JsonObject toJson() {
    final JsonObject json = new JsonObject();
    if (id != null) {
      json.put("id", id);
    }
    if (firstName != null) {
      json.put("firstName", firstName);
    }
    if (lastName != null) {
      json.put("lastName", lastName);
    }
    if (phone != null) {
      json.put("phone", phone);
    }
    json.put("email", email);
    return json;
  }

  public static ValidContact fromJson(final JsonObject json) {
    return new ValidContact(json.getInteger("id"), json.getString("firstName"), json.getString("lastName"),
      json.getString("phone"), json.getString("email"));
  }

  public Contact toContact() {
    return new Contact(id, firstName, lastName, phone, email);
  }
}
