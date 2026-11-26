package ca.bc.gov.nrs.lrmops.endpoint.v1;

import ca.bc.gov.nrs.lrmops.security.ApiScopes;
import ca.bc.gov.nrs.lrmops.struct.v1.PagedResponse;
import ca.bc.gov.nrs.lrmops.struct.v1.RoadSearchCriteria;
import ca.bc.gov.nrs.lrmops.struct.v1.RoadSearchResult;
import ca.bc.gov.nrs.lrmops.struct.v1.RoadSectionKey;
import ca.bc.gov.nrs.lrmops.struct.v1.RoadSectionResponse;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
      description = "Every criterion is optional and a case-insensitive partial match, except"
          + " tenureType, an exact code from /api/v1/road-tenure-types, and district, a Forest"
          + " District's org unit number. One row per road and section, with the codes of every"
          + " district it lies in. Retired roads are included, flagged retired. Needs scope"
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

  /**
   * The road section named by a road id and a section id: whether LRMOPS has it, and its details.
   *
   * <p>Gated on {@link ApiScopes#ROADS_READ}. Case-insensitive. 404 when LRMOPS has no such
   * section, which is the ordinary answer while a user is still typing either half.
   *
   * <p>{@code districts} usually has one entry, and two when the section crosses a district
   * boundary; the caller decides which applies. Retired roads are returned, flagged
   * {@code retired}; whether to accept one is the caller's decision.
   */
  @Operation(summary = "Get a road section",
      description = "The section's name, tenure type, retired flag, districts and BCTS office;"
          + " 404 when LRMOPS has no such section. Needs scope lrmops:roads:read.")
  @PreAuthorize(ApiScopes.ROADS_READ)
  @GetMapping("/{roadId}/sections/{sectionId}")
  ResponseEntity<RoadSectionResponse> getRoadSection(
      @PathVariable("roadId") String roadId,
      @PathVariable("sectionId") String sectionId);

  /**
   * Road names containing the term, for a pick list: names starting with it first, then
   * alphabetically.
   *
   * <p>Gated on {@link ApiScopes#ROADS_READ}. Case-insensitive; runs of spaces count as one. An
   * empty list for a term under two characters. {@code limit} is cut to 50; below 1, 20.
   */
  @Operation(summary = "Road names for a pick list",
      description = "Distinct names containing the term (at least 2 characters), those starting"
          + " with it first. Needs scope lrmops:roads:read.")
  @PreAuthorize(ApiScopes.ROADS_READ)
  @GetMapping("/names")
  ResponseEntity<List<String>> getRoadNames(
      @RequestParam(name = "search", defaultValue = "") String search,
      @RequestParam(name = "limit", defaultValue = "20") int limit);

  /**
   * Every road section with exactly this name, for a caller that filters its own records by road
   * name.
   *
   * <p>Gated on {@link ApiScopes#ROADS_READ}. Case-insensitive; surrounding spaces ignored, runs
   * of spaces counting as one. Empty for a blank name or one LRMOPS does not have.
   */
  @Operation(summary = "Road sections with a name",
      description = "Road and section ids of every section named exactly roadName"
          + " (case-insensitive). Needs scope lrmops:roads:read.")
  @PreAuthorize(ApiScopes.ROADS_READ)
  @GetMapping("/sections")
  ResponseEntity<List<RoadSectionKey>> getRoadSectionsNamed(
      @RequestParam(name = "roadName", defaultValue = "") String roadName);

  /**
   * Many road sections at once, by road id and section id: their names, tenure types and clients,
   * for a caller showing a list of its own records with their roads.
   *
   * <p>Gated on {@link ApiScopes#ROADS_READ}. A POST because the keys are free text and many: at
   * most 1,000 a request, more is a 400. Matched case-insensitively; a section LRMOPS does not
   * have is left out of the answer, so the caller can tell it from one without a name.
   */
  @Operation(summary = "Look up road sections",
      description = "Up to 1000 {roadId, sectionId} keys in; the sections LRMOPS has out, as the"
          + " road search returns them. Needs scope lrmops:roads:read.")
  @PreAuthorize(ApiScopes.ROADS_READ)
  @PostMapping("/sections/lookup")
  ResponseEntity<List<RoadSearchResult>> lookUpRoadSections(
      @RequestBody List<RoadSectionKey> keys);
}
