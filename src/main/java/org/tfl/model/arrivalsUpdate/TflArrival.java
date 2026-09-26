package org.tfl.model.arrivalsUpdate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import java.time.OffsetDateTime;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TflArrival {

    @JsonProperty("lineId")
    private String line_id;

    @JsonProperty("lineName")
    private String line_name;

    @JsonProperty("platformName")
    private String platform;

    @JsonProperty("destinationName")
    private String destination;

    @JsonProperty("expectedArrival")
    private OffsetDateTime expected_arrival;
}
