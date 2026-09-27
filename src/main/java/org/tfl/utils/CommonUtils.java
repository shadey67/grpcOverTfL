package org.tfl.utils;

import io.grpc.Context;
import io.grpc.Deadline;
import io.grpc.Status;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.tfl.constants.Constants.DEFAULT_TIMEOUT;


public class CommonUtils {

    public static String orEmpty(String value){
        return Objects.requireNonNullElse(value, "");
    }

    public static String toMinutes(Integer secondsToStation){
        if(secondsToStation == null){
            return "";
        }
        long minutes = Math.ceilDiv(Math.max(secondsToStation, 0), 60);
        return Long.toString(minutes);
    }

    public static Duration timeoutFromDeadline(){
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
