package pl.busradar.ingest;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import pl.busradar.ingest.config.KafkaTopicConfig;
import pl.busradar.ingest.poller.VehiclePositionPoller;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@Testcontainers
class VehiclePositionPollerIT {

    private static final String SAMPLE_ZTM_RESPONSE = """
      {
        "lastUpdate": "2026-11-19T14:53:24Z",
        "vehicles": [
          {
            "generated": "2026-11-19T14:53:24Z",
            "routeShortName": "175",
            "tripId": 31,
            "routeId": 175,
            "headsign": "Starogardzka",
            "vehicleCode": "2730",
            "vehicleService": "175-04",
            "vehicleId": 457,
            "speed": 0,
            "direction": 225,
            "delay": 415,
            "scheduledTripStartTime": "2026-11-19T14:25:00Z",
            "lat": 54.31983184814453,
            "lon": 18.593809127807617,
            "gpsQuality": 3
          }
        ]
      }
      """;

    @Container
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:4.3.0");

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("ztm.url", () -> wireMock.baseUrl() + "/gpsPositions?v=2");
        registry.add("ingest.poll-interval", () -> "1h");
    }

    @Autowired
    private VehiclePositionPoller poller;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void shouldFetchMockedZtmPositionsAndPublishToKafkaWithVehicleIdKey() throws Exception {
        wireMock.stubFor(get(urlPathEqualTo("/gpsPositions")).willReturn(okJson(SAMPLE_ZTM_RESPONSE)));

        try (var consumer = createTestConsumer()) {
            poller.pollVehiclePositions();

            var records = await().atMost(Duration.ofSeconds(10))
                    .until(() -> consumer.poll(Duration.ofMillis(250)), r -> !r.isEmpty());

            var record = records.iterator().next();
            assertThat(record.key()).isEqualTo("457");

            var payload = jsonMapper.readTree(record.value());
            assertThat(payload.get("vehicleId").asLong()).isEqualTo(457L);
            assertThat(payload.get("line").asString()).isEqualTo("175");
            assertThat(payload.get("headsign").asString()).isEqualTo("Starogardzka");
            assertThat(payload.get("delaySeconds").asInt()).isEqualTo(415);
            assertThat(payload.get("readAt").asString()).isEqualTo("2026-11-19T14:53:24Z");
        }
    }

    private Consumer<String, String> createTestConsumer() {
        var consumer = new KafkaConsumer<>(
                Map.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                        ConsumerConfig.GROUP_ID_CONFIG, "ingest-it-group",
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"),
                new StringDeserializer(),
                new StringDeserializer());
        consumer.subscribe(List.of(KafkaTopicConfig.VEHICLE_POSITIONS_TOPIC));
        return consumer;
    }
}
