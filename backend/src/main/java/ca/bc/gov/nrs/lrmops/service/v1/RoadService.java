package ca.bc.gov.nrs.lrmops.service.v1;

import ca.bc.gov.nrs.lrmops.repository.v1.TfmRoadRepository;
import ca.bc.gov.nrs.lrmops.struct.v1.HeaderSort;
import ca.bc.gov.nrs.lrmops.struct.v1.PagedResponse;
import ca.bc.gov.nrs.lrmops.struct.v1.RoadSearchCriteria;
import ca.bc.gov.nrs.lrmops.struct.v1.RoadSearchResult;
import ca.bc.gov.nrs.lrmops.struct.v1.RoadSortColumn;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Road search over LRMOPS's {@code TFM_ROAD}. */
@Service
@Transactional(readOnly = true)
public class RoadService {

  /** Guards against a caller asking for the whole table in one page. */
  static final int MAX_PAGE_SIZE = 200;

  /** For a caller that does not say. */
  static final int DEFAULT_PAGE_SIZE = 15;

  private static final String WILDCARD = "%";

  /** The name as the search selects it, so sorting by it sorts what is returned. */
  private static final String ROAD_NAME = "MAX(TRIM(road.roadName))";

  private final TfmRoadRepository roads;

  public RoadService(TfmRoadRepository roads) {
    this.roads = roads;
  }

  /**
   * Road sections matching the criteria, sorted by the columns asked for and then by road name,
   * road id and section id.
   *
   * <p>Every criterion is an unanchored, case-insensitive contains; a blank one is dropped, so all
   * blank returns the first page of everything.
   *
   * @param pageNumber zero-based; a negative one is the first page
   * @param pageSize rows per page, capped at {@value #MAX_PAGE_SIZE}; below 1 is the default
   * @param sort the columns to sort by, the first deciding; none for the default order
   */
  public PagedResponse<RoadSearchResult> search(RoadSearchCriteria criteria, int pageNumber,
      int pageSize, List<HeaderSort<RoadSortColumn>> sort) {
    PageRequest page = PageRequest.of(
        Math.max(pageNumber, 0), boundedPageSize(pageSize), order(sort));
    return paged(roads.search(
        contains(criteria.roadName()),
        contains(criteria.roadId()),
        contains(criteria.sectionId()),
        contains(criteria.tenureType()),
        contains(criteria.clientName()),
        page));
  }

  /**
   * The columns asked for, each breaking the ties of the one before, then road name, road id and
   * section id. Road and section are unique after grouping, so the order is total across pages.
   * Nulls sort last.
   */
  static Sort order(List<HeaderSort<RoadSortColumn>> sort) {
    Sort chosen = Sort.unsorted();
    for (HeaderSort<RoadSortColumn> header : sort) {
      chosen = chosen.and(JpaSort.unsafe(header.direction(), key(header.column())));
    }
    Sort all = chosen.and(JpaSort.unsafe(Sort.Direction.ASC,
        ROAD_NAME, "road.roadId", "road.sectionId"));
    // Blanks last in either direction, as Oracle puts them ascending, so a road without a name
    // never heads the list.
    return Sort.by(all.stream().map(Sort.Order::nullsLast).toList());
  }

  /** What a column sorts on: a grouping column or a selected aggregate, as GROUP BY requires. */
  private static String key(RoadSortColumn column) {
    return switch (column) {
      case ROAD_NAME -> ROAD_NAME;
      case ROAD_ID -> "road.roadId";
      case SECTION_ID -> "road.sectionId";
      case TENURE_TYPE -> "MAX(TRIM(road.roadTenureType))";
      case CLIENT_NAME -> "MAX(TRIM(road.clientName))";
    };
  }

  private static int boundedPageSize(int requested) {
    if (requested < 1) {
      return DEFAULT_PAGE_SIZE;
    }
    return Math.min(requested, MAX_PAGE_SIZE);
  }

  /**
   * A {@code LIKE} pattern matching {@code value} anywhere: trimmed, runs of spaces collapsed,
   * upper-cased, with {@code %}, {@code _} and {@code \} escaped so they match literally. Null
   * when blank.
   */
  static String contains(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    String normalized = value.trim().replaceAll(" {2,}", " ").toUpperCase(Locale.ROOT)
        .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    return WILDCARD + normalized + WILDCARD;
  }

  private static PagedResponse<RoadSearchResult> paged(Page<RoadSearchResult> page) {
    return new PagedResponse<>(
        page.getContent(),
        page.getTotalElements(),
        page.getTotalPages(),
        page.getNumber(),
        page.getSize());
  }
}
