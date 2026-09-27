package org.tfl.service;

import io.grpc.*;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import org.tfl.client.TflClient;
import org.tfl.model.lineStatus.TflLine;
import org.tfl.scheduled.ArrivalsPoller;
import org.tfl.tflOverGRPC.*;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.tfl.mapper.TflMappers.toProto;

public class TfLServiceImpl extends tflServiceGrpc.tflServiceImplBase {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(30);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    private final TflClient tflClient;

    public TfLServiceImpl(TflClient tflClient){
        this.tflClient = tflClient;
    }

    public void shutdown(){
        scheduler.shutdownNow();
    }

    @Override
    public void getLineStatus(
            GetLineStatusRequest request,
            StreamObserver<GetLineStatusResponse> responseStreamObserver)
    {
        try {
            String lineId = request.getLineId().trim().toLowerCase();

            TflLine tflLine = tflClient.getLineStatus(lineId, timeoutFromDeadline())
                    .orElseThrow(() -> Status.NOT_FOUND
                            .withDescription("Unknown line: " + lineId)
                            .asRuntimeException());

            responseStreamObserver.onNext(GetLineStatusResponse.newBuilder()
                            .setLine(toProto(tflLine))
                            .build());
            responseStreamObserver.onCompleted();
        } catch (StatusRuntimeException e) {
            responseStreamObserver.onError(e);
        } catch (Exception e) {
            responseStreamObserver.onError(Status.INTERNAL
                    .withDescription("Unexpected error")
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    @Override
    public void watchArrivals(
            WatchArrivalsRequest request,
            StreamObserver<ArrivalsUpdate> responseStreamObserver)
    {
        var call = (ServerCallStreamObserver<ArrivalsUpdate>) responseStreamObserver;
        String stopId = request.getStopId();

        ArrivalsPoller poller = new ArrivalsPoller(stopId, call, tflClient);
        ScheduledFuture<?> task = scheduler.scheduleWithFixedDelay(
                poller, 0, POLL_INTERVAL.toSeconds(), TimeUnit.SECONDS
        );
        poller.setTask(task);

        call.setOnCancelHandler(() -> {
            task.cancel(false);
            System.out.println("Client cancelled, stopped polling " + stopId);
        });

    }

    private Duration timeoutFromDeadline(){
        Deadline deadline = Context.current().getDeadline();
        if(deadline == null){
            return DEFAULT_TIMEOUT;
        }
        long remainingMs = deadline.timeRemaining(TimeUnit.MILLISECONDS);
        if(remainingMs<=0){
            throw Status.DEADLINE_EXCEEDED
                    .withDescription("Deadline already passed")
                    .asRuntimeException();
        }
        return Duration.ofMillis(remainingMs);
    }
}
