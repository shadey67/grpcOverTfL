package org.tfl.services;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.tfl.tflOverGRPC.*;

public class TfLServiceImpl extends tflServiceGrpc.tflServiceImplBase {

    @Override
    public void getLineStatus(GetLineStatusRequest request,
                              StreamObserver<GetLineStatusResponse> responseStreamObserver){

        String lineId = request.getLineId().trim().toLowerCase();

        if (lineId.isEmpty()) {
            responseStreamObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("line_id must not be empty")
                    .asRuntimeException());
            return;
        }

        if (!lineId.equals("jubilee")) {
            responseStreamObserver.onError(Status.NOT_FOUND
                    .withDescription("Unknown line: " + lineId)
                    .asRuntimeException());
            return;
        }

        Line line = Line.newBuilder()
                .setId("jubilee")
                .setName("Jubilee Line")
                .addStatuses(
                        LineStatus.newBuilder()
                        .setSeverity(10)
                        .setDescription("Good Service")
                        .build()
                )
                .build();

        responseStreamObserver.onNext(GetLineStatusResponse.newBuilder().setLine(line).build());
        responseStreamObserver.onCompleted();
    }
}
