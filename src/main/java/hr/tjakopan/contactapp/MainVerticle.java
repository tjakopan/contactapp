package hr.tjakopan.contactapp;

import gg.jte.CodeResolver;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.Utf8ByteOutput;
import gg.jte.resolve.DirectoryCodeResolver;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpHeaders;
import io.vertx.mutiny.core.MultiMap;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.core.http.HttpServerRequest;
import io.vertx.mutiny.ext.web.Router;
import io.vertx.mutiny.ext.web.RoutingContext;
import io.vertx.mutiny.ext.web.common.WebEnvironment;
import io.vertx.mutiny.ext.web.handler.BodyHandler;
import io.vertx.mutiny.ext.web.handler.SessionHandler;
import io.vertx.mutiny.ext.web.handler.StaticHandler;
import io.vertx.mutiny.ext.web.sstore.LocalSessionStore;
import io.vertx.mutiny.ext.web.sstore.SessionStore;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class MainVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);
  private static final Consumer<Void> NOOP = v -> {
  };
  private static final Consumer<Throwable> FAILED_RESPONSE = e -> LOGGER.error("Failed to send response", e);

  @Override
  public Uni<Void> asyncStart() {
    final TemplateEngine templateEngine = createTemplateEngine();
    final Contacts contacts = new Contacts(vertx);

    final Router router = Router.router(vertx);

    final SessionStore sessionStore = LocalSessionStore.create(vertx);
    final SessionHandler sessionHandler = SessionHandler.create(sessionStore);
    router.route().handler(sessionHandler);

    final BodyHandler bodyHandler = BodyHandler.create();
    router.post().handler(bodyHandler);

    final StaticHandler staticHandler = StaticHandler.create();
    router.route("/static/*").handler(staticHandler);

    router.get("/").handler(this::index);
    router.get("/contacts").handler(ctx -> contacts(templateEngine, contacts, ctx));
    router.get("/contacts/new").handler(ctx -> contactsNewGet(templateEngine, ctx));
    router.get("/contacts/:contactId").handler(ctx -> contactsView(templateEngine, contacts, ctx));
    router.post("/contacts/new").handler(ctx -> contactsNew(templateEngine, contacts, ctx));
    router.get("/contacts/:contactId/edit").handler(ctx -> contactsEditGet(templateEngine, contacts, ctx));
    router.post("/contacts/:contactId/edit").handler(ctx -> contactsEditPost(templateEngine, contacts, ctx));
    router.post("/contacts/:contactId/delete").handler(ctx -> contactsDelete(contacts, ctx));

    return contacts.loadDb()
      .onItem().transformToUni(v -> vertx.createHttpServer().requestHandler(router).listen(8888))
      .replaceWithVoid();
  }

  private TemplateEngine createTemplateEngine() {
    final TemplateEngine templateEngine;
    if (WebEnvironment.development()) {
      // Enables hot reload in local dev environment, requires environment variable VERTXWEB_ENVIRONMENT=dev or
      // system property vertxweb.environment=dev.
      final CodeResolver codeResolver = new DirectoryCodeResolver(Path.of("src/main/jte"));
      templateEngine = TemplateEngine.create(codeResolver, Path.of("target/jte-classes"), ContentType.Html);
    } else {
      templateEngine = TemplateEngine.createPrecompiled(ContentType.Html);
    }
    templateEngine.setBinaryStaticContent(true);
    return templateEngine;
  }

  private void index(final RoutingContext ctx) {
    ctx.redirect("/contacts").subscribe().with(NOOP, FAILED_RESPONSE);
  }

  private void contacts(final TemplateEngine templateEngine, final Contacts contacts, final RoutingContext ctx) {
    final String search = getRequestParam(ctx.request(), "q");
    final Uni<List<ValidContact>> contactsUni = search != null ? contacts.search(search) : contacts.all();
    contactsUni.subscribe().with(list -> {
      final Map<String, Object> params =
        search != null ? Map.of("q", search, "contacts", list) : Map.of("contacts", list);
      renderTemplate(templateEngine, ctx, "index.jte", params);
    }, ctx::fail);
  }

  private void contactsNewGet(final TemplateEngine templateEngine, final RoutingContext ctx) {
    renderTemplate(templateEngine, ctx, "new.jte");
  }

  private void contactsNew(final TemplateEngine templateEngine, final Contacts contacts, final RoutingContext ctx) {
    final MultiMap form = ctx.request().formAttributes();
    final Contact c = new Contact(getFormAttribute(form, "first_name"), getFormAttribute(form, "last_name"),
      getFormAttribute(form, "phone"), getFormAttribute(form, "email"));
    contacts.save(c)
      .subscribe().with(v -> {
        FlashMessage.set(ctx, "Created new contact!");
        ctx.redirect("/contacts").subscribe().with(NOOP, FAILED_RESPONSE);
      }, e -> {
        if (e instanceof ValidationError ve) {
          renderTemplate(templateEngine, ctx, "new.jte",
            Map.of("contact", c, "errors", Map.of(ve.getField(), ve.getMessage())));
        } else {
          ctx.fail(e);
        }
      });
  }

  private void contactsView(final TemplateEngine templateEngine, final Contacts contacts, final RoutingContext ctx) {
    final int contactId = Integer.parseInt(ctx.pathParam("contactId"));
    contacts.find(contactId)
      .subscribe().with(c -> {
        if (c == null) {
          ctx.fail(404);
        } else {
          renderTemplate(templateEngine, ctx, "show.jte", Map.of("contact", c));
        }
      }, ctx::fail);
  }

  private void contactsEditGet(
    final TemplateEngine templateEngine, final Contacts contacts, final RoutingContext ctx) {
    final int contactId = Integer.parseInt(ctx.pathParam("contactId"));
    contacts.find(contactId)
      .subscribe().with(c -> {
        if (c == null) {
          ctx.fail(404);
        } else {
          renderTemplate(templateEngine, ctx, "edit.jte", Map.of("contact", c.toContact()));
        }
      }, ctx::fail);
  }

  private void contactsEditPost(
    final TemplateEngine templateEngine, final Contacts contacts, final RoutingContext ctx) {
    final int contactId = Integer.parseInt(ctx.pathParam("contactId"));
    final MultiMap form = ctx.request().formAttributes();
    final Contact c = new Contact(contactId, getFormAttribute(form, "first_name"),
      getFormAttribute(form, "last_name"), getFormAttribute(form, "phone"),
      getFormAttribute(form, "email"));
    contacts.save(c)
      .subscribe().with(v -> {
        FlashMessage.set(ctx, "Updated contact!");
        ctx.redirect("/contacts/" + contactId).subscribe().with(NOOP, FAILED_RESPONSE);
      }, e -> {
        if (e instanceof ValidationError ve) {
          renderTemplate(templateEngine, ctx, "edit.jte",
            Map.of("contact", c, "errors", Map.of(ve.getField(), ve.getMessage())));
        } else {
          ctx.fail(e);
        }
      });
  }

  private void contactsDelete(final Contacts contacts, final RoutingContext ctx) {
    final int contactId = Integer.parseInt(ctx.pathParam("contactId"));
    contacts.find(contactId)
      .subscribe().with(c -> {
        if (c == null) {
          ctx.fail(404);
        } else {
          contacts.delete(c)
            .subscribe().with(v -> {
              FlashMessage.set(ctx, "Deleted contact!");
              ctx.redirect("/contacts")
                .subscribe()
                .with(NOOP, FAILED_RESPONSE);
            }, ctx::fail);
        }
      }, ctx::fail);
  }

  private void renderTemplate(final TemplateEngine templateEngine, final RoutingContext ctx, final String template) {
    renderTemplate(templateEngine, ctx, template, Map.of());
  }

  private void renderTemplate(final TemplateEngine templateEngine, final RoutingContext ctx, final String template,
                              final Map<String, Object> params) {
    pauseRequest(ctx);
    try {
      final String flashMessage = FlashMessage.getAndClear(ctx);
      Map<String, Object> actualParams = params;
      if (flashMessage != null) {
        actualParams = new HashMap<>(params);
        actualParams.put(FlashMessage.KEY, flashMessage);
      }
      final Utf8ByteOutput output = new Utf8ByteOutput();
      templateEngine.render(template, actualParams, output);
      final Buffer buffer = Buffer.buffer(output.getContentLength());
      output.writeTo(buffer::appendBytes);
      resumeRequest(ctx);
      ctx.response()
        .putHeader(HttpHeaders.CONTENT_TYPE, "text/html; charset=utf-8")
        .end(buffer)
        .subscribe().with(NOOP, FAILED_RESPONSE);
    } catch (IOException | RuntimeException e) {
      resumeRequest(ctx);
      ctx.fail(e);
    }
  }

  private static void pauseRequest(final RoutingContext ctx) {
    if (!ctx.request().isEnded()) {
      ctx.request().pause();
    }
  }

  private static void resumeRequest(final RoutingContext ctx) {
    if (!ctx.request().isEnded()) {
      ctx.request().resume();
    }
  }

  private static @Nullable String getRequestParam(final HttpServerRequest req, final String name) {
    final String value = req.getParam(name);
    if (value != null && value.isEmpty()) {
      return null;
    }
    return value;
  }

  private static @Nullable String getFormAttribute(final MultiMap form, final String name) {
    final String value = form.get(name);
    if (value != null && value.isEmpty()) {
      return null;
    }
    return value;
  }
}
