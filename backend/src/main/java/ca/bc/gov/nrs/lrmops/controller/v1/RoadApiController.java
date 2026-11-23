package ca.bc.gov.nrs.lrmops.controller.v1;

import ca.bc.gov.nrs.lrmops.endpoint.v1.RoadApiEndpoint;
import ca.bc.gov.nrs.lrmops.service.v1.RoadService;
import ca.bc.gov.nrs.lrmops.struct.v1.HeaderSort;
import ca.bc.gov.nrs.lrmops.struct.v1.PagedResponse;
import ca.bc.gov.nrs.lrmops.struct.v1.RoadSearchCriteria;
import ca.bc.gov.nrs.lrmops.struct.v1.RoadSearchResult;
import ca.bc.gov.nrs.lrmops.struct.v1.RoadSectionResponse;
import ca.bc.gov.nrs.lrmops.struct.v1.RoadSortColumn;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Roads from LRMOPS. Mappings and authorization are declared on {@link RoadApiEndpoint}. */
@RestController
public class RoadApiController implements RoadApiEndpoint {

  private final RoadService roadService;

  public RoadApiController(RoadService roadService) {
    this.roadService = roadService;
  }

  @Override
  public ResponseEntity<PagedResponse<RoadSearchResult>> searchRoads(RoadSearchCriteria criteria,
      int pageNumber, int pageSize, List<RoadSortColumn> sortBy,
      List<Sort.Direction> sortDirection) {
    return ResponseEntity.ok(roadService.search(
        criteria, pageNumber, pageSize, HeaderSort.of(sortBy, sortDirection)));
  }

  @Override
  public ResponseEntity<RoadSectionResponse> getRoadSection(String roadId, String sectionId) {
    return ResponseEntity.ok(roadService.find(roadId, sectionId));
  }
}
