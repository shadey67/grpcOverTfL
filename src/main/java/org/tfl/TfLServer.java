package org.tfl;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import org.tfl.client.TflClient;
import org.tfl.service.TfLServiceImpl;

public class TfLServer {
    static void main(String[] args) throws Exception{

        String apiKey = System.getenv("TFL_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Set the TFL_APP_KEY environment variable");
        }

        Server server = ServerBuilder.forPort(8081)
                .addService(new TfLServiceImpl(new TflClient(apiKey)))
                .addService(ProtoReflectionService.newInstance())
                .build()
                .start();

        System.out.println("Server started on port 8081");
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
        server.awaitTermination();
    }
}
