package hr.tjakopan.contactapp;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.shareddata.AsyncMap;
import io.vertx.mutiny.core.shareddata.Lock;
import io.vertx.mutiny.core.shareddata.SharedData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class Archiver {
  private static final Logger LOGGER = LoggerFactory.getLogger(Archiver.class);

  private final Random random = new Random();
  private final Vertx vertx;
  private final SharedData sharedData;
  private final AsyncMap<String, Object> map;

  public Archiver(final Vertx vertx) {
    this.vertx = vertx;
    this.sharedData = vertx.sharedData();
    this.map = vertx.sharedData().getAsyncMapAndAwait("archive_map");
  }

  public Uni<ArchiveStatus> status() {
    return getLock()
      .onItem().transformToUni(lock -> _status()
        .onTermination().invoke(lock::release));
  }

  private Uni<ArchiveStatus> _status() {
    //noinspection RedundantTypeArguments
    return map.get("archive_status")
      .onItem().ifNull().continueWith(ArchiveStatus.WAITING)
      .onItem().ifNotNull().<ArchiveStatus>transform(ArchiveStatus.class::cast);
  }

  public Uni<Double> progress() {
    return getLock()
      .onItem().transformToUni(lock -> _progress()
        .onTermination().invoke(lock::release));
  }

  private Uni<Double> _progress() {
    //noinspection RedundantTypeArguments
    return map.get("archive_progress")
      .onItem().ifNull().continueWith(0.0)
      .onItem().ifNotNull().<Double>transform(Double.class::cast);
  }

  public Uni<ArchiverState> state() {
    return getLock().onItem().transformToUni(lock -> Uni.combine().all().unis(_status(), _progress())
      .asTuple()
      .onItem().transform(tuple -> new ArchiverState(tuple.getItem1(), tuple.getItem2()))
      .onTermination().invoke(lock::release));
  }

  public Uni<Void> run() {
    return getLock().onItem().transformToUni(lock -> _status()
        .onItem().transformToUni(status -> {
          if (status == ArchiveStatus.WAITING) {
            return map.put("archive_status", ArchiveStatus.RUNNING)
              .onItem().transformToUni(v -> map.put("archive_progress", 0.0));
          }
          return Uni.createFrom().nullItem();
        })
        .onTermination().invoke(lock::release))
      .onItem().invoke(v -> {
        final AtomicInteger i = new AtomicInteger(0);
        timer(i);
      });
  }

  private void timer(final AtomicInteger i) {
    if (i.get() < 10) {
      final long delay = (long) (1000 * random.nextDouble());
      vertx.setTimer(delay, timerId -> getLock()
        .onItem().transformToUni(lock -> _status()
          .onItem().transformToUni(status -> {
            if (status != ArchiveStatus.RUNNING) {
              return Uni.createFrom().nullItem();
            }
            final double progress = (double) i.incrementAndGet() / 10;
            LOGGER.info("Progress: {}", progress);
            return map.put("archive_progress", progress);
          })
          .onTermination().invoke(lock::release))
        .subscribe().with(v -> timer(i), e -> LOGGER.error("Timer error", e)));
    } else {
      vertx.setTimer(1000, timerId -> getLock()
        .onItem().transformToUni(lock -> _status()
          .onItem().transformToUni(status -> {
            if (status != ArchiveStatus.RUNNING) {
              return Uni.createFrom().nullItem();
            }
            return map.put("archive_status", ArchiveStatus.COMPLETE);
          })
          .onTermination().invoke(lock::release))
        .subscribe().with(v -> LOGGER.info("Archive complete"), e -> LOGGER.error("Timer error", e)));
    }
  }

  private Uni<Lock> getLock() {
    return sharedData.getLock("archive_lock");
  }

  public String archiveFile() {
    return "contacts.json";
  }

  public Uni<Void> reset() {
    return getLock()
      .onItem().transformToUni(lock -> map.put("archive_status", ArchiveStatus.WAITING)
        .onItem().invoke(lock::release));
  }
}
