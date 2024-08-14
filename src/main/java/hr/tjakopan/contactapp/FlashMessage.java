package hr.tjakopan.contactapp;

import io.vertx.mutiny.ext.web.RoutingContext;
import io.vertx.mutiny.ext.web.Session;
import org.jspecify.annotations.Nullable;

public final class FlashMessage {
  public static final String KEY = "flashMessage";

  private FlashMessage() {
  }

  public static void set(final RoutingContext ctx, final String message) {
    final Session session = ctx.session();
    if (session == null) {
      throw new IllegalStateException("Session is not available");
    }
    session.put(KEY, message);
  }

  public static @Nullable String getAndClear(final RoutingContext ctx) {
    final Session session = ctx.session();
    if (session == null) {
      throw new IllegalStateException("Session is not available");
    }
    return session.<@Nullable String>remove(KEY);
  }
}
