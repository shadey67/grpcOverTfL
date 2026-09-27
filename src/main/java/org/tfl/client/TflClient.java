package org.tfl.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.tfl.exception.TflHttpException;
import org.tfl.model.arrivalsUpdate.TflArrival;
import org.tfl.model.lineStatus.TflLine;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

public class TflClient {

    private static final String BASE_URL = "https://api.tfl.gov.uk";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final String apiKey;

    public TflClient(String apiKey){
        this.apiKey = apiKey;
    }

    public Optional<TflLine> getLineStatus(String lineId, Duration timeout) throws Exception {
        URI uri = URI.create(BASE_URL + "/Line/" + lineId + "/Status?app_key="+apiKey);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(timeout)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch(Exception e){
            throw new Exception("Failed to reach TfL: " + e.getMessage());
        }

        if(response.statusCode() == 404){
            return Optional.empty();
        }
        if(response.statusCode() != 200){
            throw new TflHttpException(response.statusCode(),
                    "TfL returned HTTP " + response.statusCode());
        }

        try{
            List<TflLine> lines = mapper.readValue(response.body(), new TypeReference<>() {});
            return lines.stream().findFirst();
        } catch (IOException e){
            throw new Exception("Couldn't parse TfL response", e);
        }
    }

    public List<TflArrival> getArrivals(String stopId, Duration timeout) throws Exception {
        URI uri = URI.create(BASE_URL + "/StopPoint/" + stopId + "/arrivals?app_key="+apiKey);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(timeout)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch(Exception e){
            throw new Exception("Failed to reach TfL: " + e.getMessage());
        }

        if(response.statusCode() != 200){
            throw new TflHttpException(response.statusCode(),
                    "TfL returned HTTP " + response.statusCode());
        }

        try{
            return mapper.readValue(response.body(), new TypeReference<>() {});
        } catch (IOException e){
            throw new Exception("Couldn't parse TfL response", e);
        }
    }
}
