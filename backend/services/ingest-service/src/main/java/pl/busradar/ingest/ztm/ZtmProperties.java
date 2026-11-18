package pl.busradar.ingest.ztm;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ztm")
public record ZtmProperties(URI url, Duration connectTimeout, Duration readTimeout) {}
