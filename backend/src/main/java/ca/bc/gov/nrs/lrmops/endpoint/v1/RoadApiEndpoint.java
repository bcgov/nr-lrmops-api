package ca.bc.gov.nrs.lrmops.endpoint.v1;

import ca.bc.gov.nrs.lrmops.security.ApiScopes;
import ca.bc.gov.nrs.lrmops.struct.v1.PagedResponse;
import ca.bc.gov.nrs.lrmops.struct.v1.RoadSearchCriteria;
import ca.bc.gov.nrs.lrmops.struct.v1.RoadSearchResult;
import ca.bc.gov.nrs.lrmops.struct.v1.RoadSortColumn;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * HTTP contract for roads from LRMOPS. Implemented by
 * {@link ca.bc.gov.nrs.lrmops.controller.v1.RoadApiController}.
 */
@RequestMapping("/api/v1/roads")
@Tag(name = "Roads", description = "Road sections from LRMOPS (TFM.TFM_ROAD)")
@SecurityRequirement(name = "bearerAuth")
public interface RoadApiEndpoint {

  /**
   * Road sections matching the criteria, one page at a time with the true total.
   *
   * <p>Gated on {@link ApiScopes#ROADS_READ}. Always 200 for a valid request: an empty page is an
   * answer, not an error.
   *
   * <p><b>Sorted by a column</b> when {@code sortBy} names one ({@link RoadSortColumn}),
   * {@code sortDirection} {@code ASC} or {@code DESC}; repeat both to sort by several, the first
   * deciding. Road name, road id and section id after them. An unknown column is a 400.
   *
   * <p>A page size above 200 is cut to 200; below 1, the default of 15.
   */
  @Operation(summary = "Search road sections",
      description = "Every criterion is optional and a case-insensitive partial match. One row"
          + " per road and section. Retired roads are included, flagged retired. Needs scope"
          + " lrmops:roads:read.")
  @PreAuthorize(ApiScopes.ROADS_READ)
  @GetMapping
  ResponseEntity<PagedResponse<RoadSearchResult>> searchRoads(
      @ModelAttribute RoadSearchCriteria criteria,
      @RequestParam(name = "pageNumber", defaultValue = "0") int pageNumber,
      @RequestParam(name = "pageSize", defaultValue = "15") int pageSize,
      @RequestParam(name = "sortBy", required = false) List<RoadSortColumn> sortBy,
      @RequestParam(name = "sortDirection", required = false)
      List<Sort.Direction> sortDirection);
}
