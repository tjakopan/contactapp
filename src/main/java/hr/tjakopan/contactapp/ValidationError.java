package hr.tjakopan.contactapp;

public sealed class ValidationError extends Exception {
  private final String field;

  public ValidationError(final String field, final String message) {
    super(message);
    this.field = field;
  }

  public String getField() {
    return field;
  }

  @Override
  public String getMessage() {
    final String message = super.getMessage();
    if (message == null) {
      throw new IllegalStateException("Message is required");
    }
    return message;
  }

  public static sealed class EmailValidationError extends ValidationError {
    public EmailValidationError(final String message) {
      super("email", message);
    }

    public static final class EmailRequired extends EmailValidationError {
      public EmailRequired() {
        super("Email is required");
      }
    }

    public static final class EmailIsNotUnique extends EmailValidationError {
      public EmailIsNotUnique() {
        super("Email is not unique");
      }
    }
  }
}
