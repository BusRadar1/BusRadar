package pl.busradar.ingest.ztm;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import pl.busradar.events.VehiclePositionEvent;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public class ZtmClient {

    private static final Logger log = LoggerFactory.getLogger(ZtmClient.class);

    private final RestClient restClient;
    private final ZtmProperties properties;
    private final ZtmVehicleMapper mapper;
    private final JsonMapper jsonMapper;

    public ZtmClient(
            @Qualifier("ztmRestClient") RestClient restClient,
            ZtmProperties properties,
            ZtmVehicleMapper mapper,
            JsonMapper jsonMapper) {
        this.restClient = restClient;
        this.properties = properties;
        this.mapper = mapper;
        this.jsonMapper = jsonMapper;
    }

    public List<VehiclePositionEvent> fetchVehiclePositions() {
        ZtmResponseDto response;

        try {
            response = restClient
                    .get()
                    .uri(properties.url())
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), (request, httpResponse) -> {
                        throw new ZtmClientException("ZTM API returned HTTP "
                                + httpResponse.getStatusCode().value());
                    })
                    .body(ZtmResponseDto.class);
        } catch (ResourceAccessException exception) {
            throw new ZtmClientException(
                    "ZTM request failed due to a timeout or I/O error: " + exception.getMessage(), exception);
        } catch (RestClientException exception) {
            throw new ZtmClientException("Failed to retrieve or decode the ZTM response", exception);
        }

        if (response == null || response.vehicles() == null) {
            throw new ZtmClientException("ZTM response is empty or missing the vehicles field");
        }

        Instant ingestedAt = Instant.now();
        List<VehiclePositionEvent> events = new ArrayList<>();

        for (JsonNode vehicleNode : response.vehicles()) {
            try {
                ZtmVehicleDto vehicle = jsonMapper.treeToValue(vehicleNode, ZtmVehicleDto.class);
                events.add(mapper.map(vehicle, ingestedAt));
            } catch (JacksonException | IllegalArgumentException exception) {
                log.warn(
                        "Skipping invalid ZTM vehicle: vehicleId={}, reason={}",
                        vehicleNode == null ? null : vehicleNode.get("vehicleId"),
                        exception.getMessage());
            }
        }

        return List.copyOf(events);
    }
}
