package pl.busradar.ingest.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String VEHICLE_POSITIONS_TOPIC = "vehicle-positions";

    @Bean
    NewTopic vehiclePositionsTopic() {
        return TopicBuilder.name(VEHICLE_POSITIONS_TOPIC)
                .partitions(3)
                .replicas(1)
                .config(
                        TopicConfig.RETENTION_MS_CONFIG, // resolves to retention.ms
                        String.valueOf(604800000) // 7 days in ms
                        )
                .build();
    }
}
