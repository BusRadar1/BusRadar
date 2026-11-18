package pl.busradar.ingest.ztm;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.ResourceAccessException;
import pl.busradar.events.VehiclePositionEvent;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
class ZtmClientTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void ztmProperties(DynamicPropertyRegistry registry) {
        registry.add("ztm.url", () -> wireMock.getRuntimeInfo().getHttpBaseUrl() + "/gpsPositions?v=2");
        registry.add("ztm.read-timeout", () -> "2s");
    }

    @Autowired
    private ZtmClient client;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void returnsEventsFromRecordedResponse() throws Exception {
        String responseBody = recordedResponse();

        wireMock.stubFor(get(urlEqualTo("/gpsPositions?v=2")).willReturn(okJson(responseBody)));

        Instant beforeFetch = Instant.now();
        var events = client.fetchVehiclePositions();
        Instant afterFetch = Instant.now();

        assertEquals(200, events.size());

        var first = events.getFirst();
        var expected = new VehiclePositionEvent(
                145989L,
                "2810",
                "123-01",
                "123",
                123,
                201,
                Instant.parse("2026-11-08T17:03:00Z"),
                "Orunia Równa",
                54.33407974243164,
                18.655229568481445,
                20,
                270,
                35,
                3,
                Instant.parse("2026-11-08T17:16:32Z"),
                first.ingestedAt());

        assertEquals(expected, first);
        assertNotNull(first.ingestedAt());
        assertTrue(!first.ingestedAt().isBefore(beforeFetch));
        assertTrue(!first.ingestedAt().isAfter(afterFetch));
        assertTrue(events.stream().allMatch(event -> first.ingestedAt().equals(event.ingestedAt())));

        wireMock.verify(1, getRequestedFor(urlEqualTo("/gpsPositions?v=2")));
    }

    private String recordedResponse() throws Exception {
        try (var resource = getClass().getResourceAsStream("/ztm/2026-11-08-18.jsonl.gz")) {
            assertNotNull(resource, "Recorded ZTM fixture must be available");

            try (var reader =
                    new BufferedReader(new InputStreamReader(new GZIPInputStream(resource), StandardCharsets.UTF_8))) {
                String firstLine = reader.readLine();
                assertNotNull(firstLine, "Recorded ZTM fixture must not be empty");

                return jsonMapper.readTree(firstLine).get("body").toString();
            }
        }
    }

    @Test
    void ignoresUnknownResponseAndVehicleFields() throws Exception {
        ObjectNode response = (ObjectNode) jsonMapper.readTree(recordedResponse());
        response.put("newResponseField", "ignored");

        ObjectNode firstVehicle = (ObjectNode) response.get("vehicles").get(0);
        firstVehicle.put("newVehicleField", "ignored");

        wireMock.stubFor(get(urlEqualTo("/gpsPositions?v=2")).willReturn(okJson(response.toString())));

        var events = client.fetchVehiclePositions();

        assertEquals(200, events.size());
        assertEquals(145989L, events.getFirst().vehicleId());
        assertEquals("123", events.getFirst().line());

        wireMock.verify(1, getRequestedFor(urlEqualTo("/gpsPositions?v=2")));
    }

    @Test
    void skipsAndLogsInvalidVehiclesWithoutStoppingTheRest(CapturedOutput output) throws Exception {
        ObjectNode response = (ObjectNode) jsonMapper.readTree(recordedResponse());
        ObjectNode validVehicle = (ObjectNode) response.get("vehicles").get(0);

        ObjectNode missingId = validVehicle.deepCopy();
        missingId.putNull("vehicleId");

        ObjectNode invalidTimestamp = validVehicle.deepCopy();
        invalidTimestamp.put("vehicleId", 999999L);
        invalidTimestamp.put("generated", "invalid-time");

        var vehicles = jsonMapper.createArrayNode();
        vehicles.add(missingId);
        vehicles.add(invalidTimestamp);
        vehicles.add(validVehicle);
        response.set("vehicles", vehicles);

        wireMock.stubFor(get(urlEqualTo("/gpsPositions?v=2")).willReturn(okJson(response.toString())));

        var events = client.fetchVehiclePositions();

        assertEquals(1, events.size());
        assertEquals(145989L, events.getFirst().vehicleId());
        assertEquals(Instant.parse("2026-11-08T17:16:32Z"), events.getFirst().readAt());

        assertTrue(output.getOut()
                .contains(
                        "Skipping invalid ZTM vehicle: vehicleId=null, " + "reason=Missing required field: vehicleId"));
        assertTrue(output.getOut()
                .contains("Skipping invalid ZTM vehicle: vehicleId=999999, "
                        + "reason=Invalid timestamp in field: generated"));

        wireMock.verify(1, getRequestedFor(urlEqualTo("/gpsPositions?v=2")));
    }

    @ParameterizedTest
    @ValueSource(ints = {302, 404, 503})
    void throwsClearExceptionForNon2xxResponse(int status) {
        wireMock.stubFor(
                get(urlEqualTo("/gpsPositions?v=2")).willReturn(aResponse().withStatus(status)));

        var exception = assertThrows(ZtmClientException.class, () -> client.fetchVehiclePositions());

        assertEquals("ZTM API returned HTTP " + status, exception.getMessage());

        wireMock.verify(1, getRequestedFor(urlEqualTo("/gpsPositions?v=2")));
    }

    @Test
    void throwsClearExceptionOnReadTimeout() {
        wireMock.stubFor(get(urlEqualTo("/gpsPositions?v=2"))
                .willReturn(okJson("{\"lastUpdate\":null,\"vehicles\":[]}").withFixedDelay(5000)));

        var exception = assertThrows(ZtmClientException.class, () -> client.fetchVehiclePositions());

        assertTrue(exception.getMessage().contains("timeout"));

        var cause = assertInstanceOf(ResourceAccessException.class, exception.getCause());

        assertInstanceOf(HttpTimeoutException.class, cause.getMostSpecificCause());

        wireMock.verify(1, getRequestedFor(urlEqualTo("/gpsPositions?v=2")));
    }

    @Test
    void skipsAndLogsInvalidFieldTypeWithoutStoppingTheRest(CapturedOutput output) throws Exception {
        ObjectNode response = (ObjectNode) jsonMapper.readTree(recordedResponse());
        ObjectNode validVehicle = (ObjectNode) response.get("vehicles").get(0);

        ObjectNode invalidVehicle = validVehicle.deepCopy();
        invalidVehicle.put("vehicleId", 999998L);
        invalidVehicle.put("speed", "invalid");

        var vehicles = jsonMapper.createArrayNode();
        vehicles.add(invalidVehicle);
        vehicles.add(validVehicle);
        response.set("vehicles", vehicles);

        wireMock.stubFor(get(urlEqualTo("/gpsPositions?v=2")).willReturn(okJson(response.toString())));

        var events = client.fetchVehiclePositions();

        assertEquals(1, events.size());
        assertEquals(145989L, events.getFirst().vehicleId());
        assertEquals(20, events.getFirst().speed());

        assertTrue(output.getOut().contains("Skipping invalid ZTM vehicle: vehicleId=999998, reason="));

        wireMock.verify(1, getRequestedFor(urlEqualTo("/gpsPositions?v=2")));
    }
}
