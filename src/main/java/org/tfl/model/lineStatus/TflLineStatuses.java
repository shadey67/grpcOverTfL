package org.tfl.model.lineStatus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TflLineStatuses {

    @JsonProperty("statusSeverity")
    private Integer severity;

    @JsonProperty("statusSeverityDescription")
    private String description;
}
