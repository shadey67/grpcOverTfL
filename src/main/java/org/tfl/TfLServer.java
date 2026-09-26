package org.tfl;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import org.tfl.services.TfLServiceImpl;

public class TfLServer {
    static void main(String[] args) throws Exception{
        Server server = ServerBuilder.forPort(8081)
                .addService(new TfLServiceImpl())
                .addService(ProtoReflectionService.newInstance())
                .build()
                .start();

        System.out.println("Server started on port 8081");
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
        server.awaitTermination();
    }
}
