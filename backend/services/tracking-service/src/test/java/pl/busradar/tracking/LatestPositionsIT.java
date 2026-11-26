package pl.busradar.tracking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import pl.busradar.events.VehiclePositionEvent;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.kafka.producer.key-serializer=" + "org.apache.kafka.common.serialization.StringSerializer",
            "spring.kafka.producer.value-serializer="
                    + "org.springframework.kafka.support.serializer.JacksonJsonSerializer"
        })
@Testcontainers
@Import(LatestPositionsIT.KafkaTestConfiguration.class)
class LatestPositionsIT {

    private static final String TOPIC = "vehicle-positions";
    private static final String REDIS_TEST_PASSWORD = "tracking-test";

    @Container
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:4.3.0");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:8")
            .withExposedPorts(6379)
            .withCommand("redis-server", "--requirepass", REDIS_TEST_PASSWORD);

    @DynamicPropertySource
    static void connectionProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> REDIS_TEST_PASSWORD);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class KafkaTestConfiguration {

        @Bean
        NewTopic vehiclePositionsTopic() {
            return TopicBuilder.name(TOPIC).partitions(3).replicas(1).build();
        }
    }

    @Autowired
    private KafkaTemplate<String, VehiclePositionEvent> kafkaTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private JsonMapper jsonMapper;

    @Value("${local.server.port}")
    private int port;

    private final RestClient httpClient = RestClient.create();

    @Test
    void storesLatestPositionsAndExposesVehicles() throws Exception {
        assertThat(getVehicles()).isEqualTo(jsonMapper.readTree("[]"));

        var first = event(145989L, Instant.parse("2026-11-08T17:16:32Z"), 20);
        publish(first);
        awaitStored(first);

        var newer = event(145989L, Instant.parse("2026-11-08T17:16:32.100000001Z"), 30);
        publish(newer);
        awaitStored(newer);

        var older = event(145989L, Instant.parse("2026-11-08T17:16:32.100Z"), 5);

        awaitConsumed(publish(older));
        awaitStored(newer);

        JsonNode expected = jsonMapper.readTree("""
                [
                  {
                    "vehicleId": 145989,
                    "vehicleCode": "2810",
                    "line": "123",
                    "headsign": "Brama Wyżynna",
                    "lat": 54.334,
                    "lon": 18.655,
                    "direction": 270,
                    "speed": 30,
                    "delaySeconds": 35,
                    "readAt": "2026-11-08T17:16:32.100000001Z"
                  }
                ]
                """);

        assertThat(getVehicles()).isEqualTo(expected);
    }

    private SendResult<String, VehiclePositionEvent> publish(VehiclePositionEvent event) throws Exception {
        return kafkaTemplate
                .send(TOPIC, String.valueOf(event.vehicleId()), event)
                .get(10, TimeUnit.SECONDS);
    }

    private void awaitConsumed(SendResult<String, VehiclePositionEvent> result) {
        var metadata = result.getRecordMetadata();
        var partition = new TopicPartition(metadata.topic(), metadata.partition());

        try (var admin =
                Admin.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers()))) {
            await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
                var offsets = admin.listConsumerGroupOffsets("tracking")
                        .partitionsToOffsetAndMetadata()
                        .get(10, TimeUnit.SECONDS);

                var committed = offsets.get(partition);

                assertThat(committed).isNotNull();
                assertThat(committed.offset()).isGreaterThanOrEqualTo(metadata.offset() + 1);
            });
        }
    }

    private void awaitStored(VehiclePositionEvent expected) {
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            String json = redisTemplate.opsForValue().get("vehicle:" + expected.vehicleId());

            assertThat(json).isNotNull();

            var actual = jsonMapper.readValue(json, VehiclePositionEvent.class);
            assertThat(actual).isEqualTo(expected);
        });
    }

    private JsonNode getVehicles() {
        var response = httpClient
                .get()
                .uri("http://localhost:" + port + "/vehicles")
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        return jsonMapper.readTree(response.getBody());
    }

    private static VehiclePositionEvent event(long vehicleId, Instant readAt, int speed) {
        return new VehiclePositionEvent(
                vehicleId,
                "2810",
                "123-01",
                "123",
                123,
                null,
                null,
                "Brama Wyżynna",
                54.334,
                18.655,
                speed,
                270,
                35,
                3,
                readAt,
                Instant.parse("2026-11-08T17:17:00Z"));
    }
}
