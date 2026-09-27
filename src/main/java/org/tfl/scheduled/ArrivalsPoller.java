package org.tfl.scheduled;

import io.grpc.Status;
import io.grpc.stub.ServerCallStreamObserver;
import lombok.Setter;
import org.tfl.client.TflClient;
import org.tfl.exception.TflHttpException;
import org.tfl.model.arrivalsUpdate.TflArrival;
import org.tfl.tflOverGRPC.ArrivalsUpdate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import static org.tfl.mapper.TflMappers.toProto;

public class ArrivalsPoller implements Runnable{

    private final String stopId;
    private final ServerCallStreamObserver<ArrivalsUpdate> call;
    private final TflClient tflClient;
    @Setter
    private ScheduledFuture<?> task;
    private int consecutiveFailures = 0;
    private boolean sentFirstUpdate = false;
    private static final Duration TFL_TIMEOUT = Duration.ofSeconds(5);
    private static final int MAX_CONSECUTIVE_FAILURES = 3;

    public ArrivalsPoller(
            String stopId,
            ServerCallStreamObserver<ArrivalsUpdate> call,
            TflClient client
    ){
        this.stopId = stopId;
        this.call = call;
        this.tflClient = client;
    }

    @Override
    public void run(){
        if(call.isCancelled()){
            stop();
            return;
        }
        try{
            System.out.println("Polling TfL for " + stopId);
            List<TflArrival> arrivals = tflClient.getArrivals(stopId, TFL_TIMEOUT);
            String stopName = arrivals.getFirst().getStop_name();

            call.onNext(ArrivalsUpdate.newBuilder()
                    .setStopId(stopId)
                    .setStopName(stopName)
                    .addAllArrivals(toProto(arrivals))
                    .setFetchedAt(now())
                    .build());

            sentFirstUpdate = true;
            consecutiveFailures = 0;
        } catch (TflHttpException e) {
            if (e.getStatusCode() == 404 && !sentFirstUpdate) {
                finishWithError(Status.NOT_FOUND.withDescription("Unknown stop: " + stopId));
                return;
            }
            consecutiveFailures++;
            System.out.println("TfL poll failed (" + consecutiveFailures + "): " + e.getMessage());
            if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                finishWithError(Status.UNAVAILABLE.withDescription("TfL unavailable: " + e.getMessage()));
            }

        } catch (Exception e) {
            finishWithError(Status.INTERNAL.withDescription("Unexpected error").withCause(e));
        }
    }

    private void finishWithError(Status status) {
        stop();
        call.onError(status.asRuntimeException());
    }

    private void stop() {
        if (task != null) {
            task.cancel(false);
        }
    }

    private static com.google.protobuf.Timestamp now() {
        Instant now = Instant.now();
        return com.google.protobuf.Timestamp.newBuilder()
                .setSeconds(now.getEpochSecond())
                .setNanos(now.getNano())
                .build();
    }
}
