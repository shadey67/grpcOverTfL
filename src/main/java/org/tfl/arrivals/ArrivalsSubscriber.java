package org.tfl.arrivals;

import io.grpc.Status;
import io.grpc.stub.ServerCallStreamObserver;
import org.tfl.tflOverGRPC.ArrivalsUpdate;
import org.tfl.utils.Snapshot;

public class ArrivalsSubscriber {

    private final ServerCallStreamObserver<ArrivalsUpdate> call;

    private Snapshot pending;
    private long lastSentVersion = -1;
    private boolean closed = false;

    public ArrivalsSubscriber(ServerCallStreamObserver<ArrivalsUpdate> call){
        this.call = call;
        call.setOnReadyHandler(this::flush);
    }

    public synchronized void send(Snapshot snapshot){
        long newest = (pending != null) ? pending.version() : lastSentVersion;
        if(closed || snapshot.version() <= newest){
            return;
        }
        pending = snapshot;
        flush();
    }

    private synchronized void flush(){
        if(closed || pending == null || call.isCancelled() || !call.isReady()){
            return;
        }
        Snapshot toSend = pending;
        pending = null;
        call.onNext(toSend.update());
        lastSentVersion = toSend.version();
    }

    public synchronized void fail(Status status){
        if(closed){
            return;
        }
        closed = true;
        pending = null;
        call.onError(status.asRuntimeException());
    }
}
