package org.tfl.arrivals;

import io.grpc.Status;
import org.tfl.client.TflClient;
import org.tfl.exception.TflHttpException;
import org.tfl.model.arrivalsUpdate.TflArrival;
import org.tfl.tflOverGRPC.ArrivalsUpdate;
import org.tfl.utils.Snapshot;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import static org.tfl.constants.Constants.*;
import static org.tfl.mapper.TflMappers.toProto;

public class ArrivalsHub {

    private final TflClient tflClient;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    private final ConcurrentHashMap<String, StopFeed> feeds = new ConcurrentHashMap<>();

    public ArrivalsHub(TflClient tflClient){
        this.tflClient = tflClient;
    }

    private class StopFeed implements Runnable{
        final String stopId;
        final Set<ArrivalsSubscriber> subscribers = ConcurrentHashMap.newKeySet();
        volatile Snapshot latest;

        private volatile ScheduledFuture<?> task;
        private volatile boolean stopped = false;
        private long version = 0;
        private int consecutiveFailures = 0;

        StopFeed(String stopId){
            this.stopId = stopId;
        }

        void start(){
            task = scheduler.scheduleWithFixedDelay(
                    this, 0, POLL_INTERVAL.toSeconds(), TimeUnit.SECONDS
            );
            System.out.println("[" + stopId + "] started polling");
        }

        void stop(){
            stopped = true;
            if(task != null){
                task.cancel(false);
            }
        }

        @Override
        public void run(){
            if(stopped){
                if(task != null){
                    task.cancel(false);
                }
                return;
            }
            try{
                System.out.println("[" + stopId + "] polling TfL for "
                        + subscribers.size() + " subscriber(s)");

                List<TflArrival> arrivals = tflClient.getArrivals(stopId, TFL_TIMEOUT);

                ArrivalsUpdate update = ArrivalsUpdate.newBuilder()
                        .setStopId(stopId)
                        .addAllArrivals(toProto(arrivals))
                        .setFetchedAt(now())
                        .build();

                Snapshot snapshot = new Snapshot(++version, update);
                latest = snapshot;
                consecutiveFailures = 0;

                for (ArrivalsSubscriber subscriber : subscribers){
                    subscriber.send(snapshot);
                }
            } catch (TflHttpException e) {
                if (e.getStatusCode() == 404 && latest == null) {
                    closeAll(Status.NOT_FOUND.withDescription("Unknown stop: " + stopId));
                    return;
                }
                consecutiveFailures++;
                System.out.println("[" + stopId + "] poll failed ("
                        + consecutiveFailures + "): " + e.getMessage());
                if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                    closeAll(Status.UNAVAILABLE.withDescription("TfL unavailable: " + e.getMessage()));
                }
            } catch (Exception e) {
                closeAll(Status.INTERNAL.withDescription("Unexpected error").withCause(e));
            }
        }

        private void closeAll(Status status){
            feeds.remove(stopId, this);
            stop();
            for (ArrivalsSubscriber subscriber : subscribers) {
                subscriber.fail(status);
            }
            System.out.println("[" + stopId + "] closed: " + status.getCode());
        }

        private static com.google.protobuf.Timestamp now() {
            Instant now = Instant.now();
            return com.google.protobuf.Timestamp.newBuilder()
                    .setSeconds(now.getEpochSecond())
                    .setNanos(now.getNano())
                    .build();
        }
    }

    public void shutdown(){
        scheduler.shutdownNow();
    }

    public void subscribe(String stopId, ArrivalsSubscriber subscriber){
        StopFeed feed = feeds.compute(stopId, (id, existing) -> {
            StopFeed f = (existing != null) ? existing : new StopFeed(id);
            f.subscribers.add(subscriber);
            if(existing == null){
                f.start();
            }
            return f;
        });

        Snapshot latest = feed.latest;
        if(latest != null){
            subscriber.send(latest);
        }
    }

    public void unsubscribe(String stopId, ArrivalsSubscriber subscriber){
        feeds.computeIfPresent(stopId, (id, feed) -> {
            feed.subscribers.remove(subscriber);
            if(feed.subscribers.isEmpty()){
                feed.stop();
                System.out.println("[" + id + "] no subscribers left, stopped polling");
                return null;
            }
            return feed;
        });
    }
}
