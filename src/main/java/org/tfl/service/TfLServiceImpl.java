package org.tfl.service;

import io.grpc.Context;
import io.grpc.Deadline;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.tfl.client.TflClient;
import org.tfl.model.arrivalsUpdate.TflArrival;
import org.tfl.model.lineStatus.TflLine;
import org.tfl.model.lineStatus.TflLineStatuses;
import org.tfl.tflOverGRPC.*;

import com.google.protobuf.Timestamp;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class TfLServiceImpl extends tflServiceGrpc.tflServiceImplBase {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    private final TflClient tflClient;

    public TfLServiceImpl(TflClient tflClient){
        this.tflClient = tflClient;
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
        try{
            String stopId = request.getStopId();

            List<TflArrival> arrivalList = tflClient.getArrivals(stopId, timeoutFromDeadline());

            responseStreamObserver.onNext(ArrivalsUpdate.newBuilder()
                            .addAllArrivals(toProto(arrivalList))
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

    private static Line toProto(TflLine tflLine){
        Line.Builder line = Line.newBuilder()
                .setId(tflLine.getId())
                .setName(tflLine.getName());

        if(tflLine.getLineStatuses() != null){
            for(TflLineStatuses s : tflLine.getLineStatuses()){
                line.addStatuses(LineStatus.newBuilder()
                        .setDescription(s.getDescription())
                        .setSeverity(s.getSeverity())
                        .build());
            }
        }
        return line.build();
    }

    private static List<Arrival> toProto(List<TflArrival> tflArrivals){
        List<Arrival> arrivals = new ArrayList<>();
        for(TflArrival arrival : tflArrivals){
            Arrival.Builder builder = Arrival.newBuilder()
                    .setDestination(orEmpty(arrival.getDestination()))
                    .setLineName(orEmpty(arrival.getLine_name()))
                    .setLineId(orEmpty(arrival.getLine_id()))
                    .setPlatform(orEmpty(arrival.getPlatform()));

            if(arrival.getExpected_arrival() != null){
                builder.setExpectedArrival(toTimestamp(arrival.getExpected_arrival()));
            }
            arrivals.add(builder.build());
        }
        return arrivals;
    }

    private static String orEmpty(String value){
        return Objects.requireNonNullElse(value, "");
    }

    private static Timestamp toTimestamp(OffsetDateTime dateTime){
        Instant instant = dateTime.toInstant();
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }
}
