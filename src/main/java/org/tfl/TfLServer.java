package org.tfl;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import org.tfl.client.TflClient;
import org.tfl.arrivals.ArrivalsHub;
import org.tfl.service.TfLServiceImpl;

public class TfLServer {
    static void main() throws Exception{

        String apiKey = System.getenv("TFL_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Set the TFL_API_KEY environment variable");
        }

        TflClient tflClient = new TflClient(apiKey);
        ArrivalsHub arrivalsHub = new ArrivalsHub(tflClient);

        Server server = ServerBuilder.forPort(8081)
                .addService(new TfLServiceImpl(tflClient, arrivalsHub))
                .addService(ProtoReflectionService.newInstance())
                .build()
                .start();

        System.out.println("Server started on port 8081");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.shutdown();
            arrivalsHub.shutdown();
        }));
        server.awaitTermination();
    }
}
