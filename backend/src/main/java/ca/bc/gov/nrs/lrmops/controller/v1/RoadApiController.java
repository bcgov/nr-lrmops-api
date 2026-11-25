package ca.bc.gov.nrs.lrmops.controller.v1;

import ca.bc.gov.nrs.lrmops.endpoint.v1.RoadApiEndpoint;
import ca.bc.gov.nrs.lrmops.service.v1.RoadService;
import ca.bc.gov.nrs.lrmops.struct.v1.HeaderSort;
import ca.bc.gov.nrs.lrmops.struct.v1.PagedResponse;
import ca.bc.gov.nrs.lrmops.struct.v1.RoadSearchCriteria;
import ca.bc.gov.nrs.lrmops.struct.v1.RoadSearchResult;
import ca.bc.gov.nrs.lrmops.struct.v1.RoadSectionKey;
import ca.bc.gov.nrs.lrmops.struct.v1.RoadSectionResponse;
import ca.bc.gov.nrs.lrmops.struct.v1.RoadSortColumn;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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

  @Override
  public ResponseEntity<List<String>> getRoadNames(String search, int limit) {
    return ResponseEntity.ok(roadService.names(search, limit));
  }

  @Override
  public ResponseEntity<List<RoadSectionKey>> getRoadSectionsNamed(String roadName) {
    return ResponseEntity.ok(roadService.sectionsNamed(roadName));
  }

  @Override
  public ResponseEntity<List<RoadSearchResult>> lookUpRoadSections(List<RoadSectionKey> keys) {
    if (keys == null || keys.size() > RoadService.MAX_LOOKUP) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Send between 0 and " + RoadService.MAX_LOOKUP + " road sections");
    }
    return ResponseEntity.ok(roadService.lookUp(keys));
  }
}
