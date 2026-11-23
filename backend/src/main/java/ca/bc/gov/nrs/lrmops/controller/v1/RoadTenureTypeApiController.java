package ca.bc.gov.nrs.lrmops.controller.v1;

import ca.bc.gov.nrs.lrmops.endpoint.v1.RoadTenureTypeApiEndpoint;
import ca.bc.gov.nrs.lrmops.service.v1.RoadTenureTypeService;
import ca.bc.gov.nrs.lrmops.struct.v1.RoadTenureTypeResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * LRMOPS's road tenure types. Mappings and authorization are declared on
 * {@link RoadTenureTypeApiEndpoint}.
 */
@RestController
public class RoadTenureTypeApiController implements RoadTenureTypeApiEndpoint {

  private final RoadTenureTypeService tenureTypes;

  public RoadTenureTypeApiController(RoadTenureTypeService tenureTypes) {
    this.tenureTypes = tenureTypes;
  }

  @Override
  public ResponseEntity<List<RoadTenureTypeResponse>> listRoadTenureTypes() {
    return ResponseEntity.ok(tenureTypes.list());
  }
}
