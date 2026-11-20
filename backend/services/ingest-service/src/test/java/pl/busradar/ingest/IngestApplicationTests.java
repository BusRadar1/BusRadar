package pl.busradar.ingest;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "ingest.polling.enabled=false")
class IngestApplicationTests {

    @Test
    void contextLoads() {}
}
