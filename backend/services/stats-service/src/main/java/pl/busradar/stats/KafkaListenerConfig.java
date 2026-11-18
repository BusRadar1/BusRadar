package pl.busradar.stats;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaListenerConfig {

    @Bean
    DefaultErrorHandler kafkaErrorHandler() {
        // A failed batch is sought back and retried. The offset stays uncommitted
        // until insertBatch returns, including while PostgreSQL is unavailable.
        return new DefaultErrorHandler(new FixedBackOff(5_000L, FixedBackOff.UNLIMITED_ATTEMPTS));
    }
}
