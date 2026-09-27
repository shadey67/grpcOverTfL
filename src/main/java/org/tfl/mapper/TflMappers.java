package org.tfl.mapper;

import org.tfl.model.arrivalsUpdate.TflArrival;
import org.tfl.model.lineStatus.TflLine;
import org.tfl.model.lineStatus.TflLineStatuses;
import org.tfl.tflOverGRPC.Arrival;
import org.tfl.tflOverGRPC.Line;
import org.tfl.tflOverGRPC.LineStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TflMappers {

    public static Line toProto(TflLine tflLine){
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

    public static List<Arrival> toProto(List<TflArrival> tflArrivals){
        List<Arrival> arrivals = new ArrayList<>();
        for(TflArrival arrival : tflArrivals){
            arrivals.add(Arrival.newBuilder()
                    .setDestination(orEmpty(arrival.getDestination()))
                    .setLineName(orEmpty(arrival.getLine_name()))
                    .setLineId(orEmpty(arrival.getLine_id()))
                    .setPlatform(orEmpty(arrival.getPlatform()))
                    .setTimeToArrival(toMinutes(arrival.getTime_to_station()) + " Minutes")
                    .build());
        }
        return arrivals;
    }

    private static String orEmpty(String value){
        return Objects.requireNonNullElse(value, "");
    }

    private static String toMinutes(Integer secondsToStation){
        if(secondsToStation == null){
            return "";
        }
        long minutes = Math.ceilDiv(Math.max(secondsToStation, 0), 60);
        return Long.toString(minutes);
    }
}
