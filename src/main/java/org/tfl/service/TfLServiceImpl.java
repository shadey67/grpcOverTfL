package org.tfl.service;

import io.grpc.*;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import org.tfl.client.TflClient;
import org.tfl.model.lineStatus.TflLine;
import org.tfl.arrivals.ArrivalsHub;
import org.tfl.arrivals.ArrivalsSubscriber;
import org.tfl.tflOverGRPC.*;

import static org.tfl.mapper.TflMappers.toProto;
import static org.tfl.utils.CommonUtils.timeoutFromDeadline;

public class TfLServiceImpl extends tflServiceGrpc.tflServiceImplBase {

    private final TflClient tflClient;
    private final ArrivalsHub arrivalsHub;

    public TfLServiceImpl(TflClient tflClient, ArrivalsHub arrivalsHub){
        this.tflClient = tflClient;
        this.arrivalsHub = arrivalsHub;
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

        var subscriber = new ArrivalsSubscriber(call) ;
        call.setOnCancelHandler(() -> arrivalsHub.unsubscribe(stopId, subscriber));
        arrivalsHub.subscribe(stopId, subscriber);
    }
}
