package hr.tjakopan.contactapp;

import org.jspecify.annotations.Nullable;

public record Contact(@Nullable Integer id, @Nullable String firstName, @Nullable String lastName,
                      @Nullable String phone, @Nullable String email) {
  public Contact(@Nullable String firstName, @Nullable String lastName, @Nullable String phone,
                 @Nullable String email) {
    this(null, firstName, lastName, phone, email);
  }

  public Contact() {
    this(null, null, null, null, null);
  }
}
