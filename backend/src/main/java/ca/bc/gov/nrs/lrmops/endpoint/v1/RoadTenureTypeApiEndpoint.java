package ca.bc.gov.nrs.lrmops.endpoint.v1;

import ca.bc.gov.nrs.lrmops.security.ApiScopes;
import ca.bc.gov.nrs.lrmops.struct.v1.RoadTenureTypeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * HTTP contract for LRMOPS's road tenure types. Implemented by
 * {@link ca.bc.gov.nrs.lrmops.controller.v1.RoadTenureTypeApiController}.
 */
@RequestMapping("/api/v1/road-tenure-types")
@Tag(name = "Roads", description = "Road sections from LRMOPS (TFM.TFM_ROAD)")
@SecurityRequirement(name = "bearerAuth")
public interface RoadTenureTypeApiEndpoint {

  /**
   * Every road tenure type, by code, with its description: the values {@code tenureType} on
   * {@code GET /api/v1/roads} matches exactly. Retired variants ({@code RP-RETIRE}) included;
   * {@code !} ("Geometry only") left out.
   *
   * <p>Gated on {@link ApiScopes#ROADS_READ}, as the road search it serves.
   */
  @Operation(summary = "List road tenure types",
      description = "Codes (FSR, FSR-RETIRE, RP, …) and descriptions; '!' (Geometry only) left"
          + " out. Needs scope lrmops:roads:read.")
  @PreAuthorize(ApiScopes.ROADS_READ)
  @GetMapping
  ResponseEntity<List<RoadTenureTypeResponse>> listRoadTenureTypes();
}
