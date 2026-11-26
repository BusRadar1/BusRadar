package pl.busradar.tracking;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VehiclesController {

    private final VehicleQueryService queryService;

    public VehiclesController(VehicleQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/vehicles")
    public List<VehicleResponse> getVehicles() {
        return queryService.findAll();
    }
}
