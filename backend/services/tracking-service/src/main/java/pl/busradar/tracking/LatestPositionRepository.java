package pl.busradar.tracking;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;
import pl.busradar.events.VehiclePositionEvent;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

@Repository
public class LatestPositionRepository {

    private static final DateTimeFormatter REDIS_TIMESTAMP =
            new DateTimeFormatterBuilder().appendInstant(9).toFormatter();

    private static final DefaultRedisScript<Long> SAVE_IF_NEWER = new DefaultRedisScript<>("""
                    local current = redis.call('GET', KEYS[1])
                    if current then
                        local currentReadAt = cjson.decode(current).readAt
                        if currentReadAt >= ARGV[1] then
                            return 0
                        end
                    end
                    redis.call('SET', KEYS[1], ARGV[2])
                    return 1
                    """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final JsonMapper jsonMapper;

    public LatestPositionRepository(StringRedisTemplate redisTemplate, JsonMapper jsonMapper) {
        this.redisTemplate = redisTemplate;
        this.jsonMapper = jsonMapper;
    }

    public boolean saveIfNewer(VehiclePositionEvent event) {
        String key = "vehicle:" + event.vehicleId();
        String readAt = REDIS_TIMESTAMP.format(event.readAt());

        ObjectNode payload = (ObjectNode) jsonMapper.valueToTree(event);
        payload.put("readAt", readAt);

        Long result =
                redisTemplate.execute(SAVE_IF_NEWER, List.of(key), readAt, jsonMapper.writeValueAsString(payload));

        return Long.valueOf(1L).equals(result);
    }

    public List<VehiclePositionEvent> findAll() {
        Set<String> keys = new HashSet<>();
        var options = ScanOptions.scanOptions().match("vehicle:*").count(100).build();

        try (var cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                keys.add(cursor.next());
            }
        }

        if (keys.isEmpty()) {
            return List.of();
        }

        return redisTemplate.opsForValue().multiGet(keys).stream()
                .filter(Objects::nonNull)
                .map(json -> jsonMapper.readValue(json, VehiclePositionEvent.class))
                .toList();
    }
}
