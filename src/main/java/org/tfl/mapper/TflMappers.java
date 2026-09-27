package org.tfl.mapper;

import org.tfl.model.arrivalsUpdate.TflArrival;
import org.tfl.model.lineStatus.TflLine;
import org.tfl.model.lineStatus.TflLineStatuses;
import org.tfl.tflOverGRPC.Arrival;
import org.tfl.tflOverGRPC.Line;
import org.tfl.tflOverGRPC.LineStatus;
import java.util.Comparator;
import java.util.List;

import static org.tfl.utils.CommonUtils.orEmpty;
import static org.tfl.utils.CommonUtils.toMinutes;

public class TflMappers {

    private TflMappers(){};

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
        return tflArrivals.stream()
                .sorted(Comparator.comparing(TflArrival::getExpected_arrival))
                .map(TflMappers::toProto)
                .toList();
        }

    private static Arrival toProto(TflArrival arrival){
        return Arrival.newBuilder()
                .setDestination(orEmpty(arrival.getDestination()))
                .setLineName(orEmpty(arrival.getLine_name()))
                .setLineId(orEmpty(arrival.getLine_id()))
                .setPlatform(orEmpty(arrival.getPlatform()))
                .setTimeToArrival(toMinutes(arrival.getTime_to_station()) + " Minutes")
                .build();
    }
}
