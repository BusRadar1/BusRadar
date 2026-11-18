package pl.busradar.ingest.ztm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import tools.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ZtmResponseDto(String lastUpdate, List<JsonNode> vehicles) {}
