package ca.bc.gov.nrs.lrmops.service.v1;

import static ca.bc.gov.nrs.lrmops.util.RoadValues.baseTenureType;
import static ca.bc.gov.nrs.lrmops.util.RoadValues.collapseSpaces;
import static ca.bc.gov.nrs.lrmops.util.RoadValues.isRetired;

import ca.bc.gov.nrs.lrmops.exception.RoadSectionNotFoundException;
import ca.bc.gov.nrs.lrmops.model.v1.TfmRoadEntity;
import ca.bc.gov.nrs.lrmops.repository.v1.RoadSectionDistrictRow;
import ca.bc.gov.nrs.lrmops.repository.v1.TfmRoadRepository;
import ca.bc.gov.nrs.lrmops.struct.v1.District;
import ca.bc.gov.nrs.lrmops.struct.v1.HeaderSort;
import ca.bc.gov.nrs.lrmops.struct.v1.PagedResponse;
import ca.bc.gov.nrs.lrmops.struct.v1.RoadSectionKey;
import ca.bc.gov.nrs.lrmops.struct.v1.RoadSectionResponse;
import ca.bc.gov.nrs.lrmops.struct.v1.RoadSearchCriteria;
import ca.bc.gov.nrs.lrmops.struct.v1.RoadSearchResult;
import ca.bc.gov.nrs.lrmops.struct.v1.RoadSortColumn;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Road search and road section lookup over LRMOPS's {@code TFM_ROAD}. */
@Service
@Transactional(readOnly = true)
public class RoadService {

  /** Guards against a caller asking for the whole table in one page. */
  static final int MAX_PAGE_SIZE = 200;

  /** For a caller that does not say. */
  static final int DEFAULT_PAGE_SIZE = 15;

  /** Road names returned for a pick list, at most. */
  static final int MAX_NAMES = 50;

  /** Road names returned for a pick list when the caller does not say. */
  static final int DEFAULT_NAMES = 20;

  /** Fewer characters than this match too many names to be worth a pick list. */
  static final int MIN_NAME_TERM = 2;

  /**
   * Sections one lookup may ask for: Oracle's limit on an {@code IN} list, as the lookup asks by
   * road id and there is at most one road id per section.
   */
  public static final int MAX_LOOKUP = 1000;

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
   * <p>Every criterion but the tenure type and the district is an unanchored, case-insensitive
   * contains; the tenure type is an exact code ({@code FSR} active only, {@code FSR-RETIRE} retired
   * only), the district an org unit number. A blank one is dropped, so all blank returns the first
   * page of everything.
   *
   * <p>The page's districts are read after it, by its road ids — indexed, a page at most
   * {@value #MAX_PAGE_SIZE} sections.
   *
   * @param pageNumber zero-based; a negative one is the first page
   * @param pageSize rows per page, capped at {@value #MAX_PAGE_SIZE}; below 1 is the default
   * @param sort the columns to sort by, the first deciding; none for the default order
   */
  public PagedResponse<RoadSearchResult> search(RoadSearchCriteria criteria, int pageNumber,
      int pageSize, List<HeaderSort<RoadSortColumn>> sort) {
    PageRequest page = PageRequest.of(
        Math.max(pageNumber, 0), boundedPageSize(pageSize), order(sort));
    Page<RoadSearchResult> found = roads.search(
        contains(criteria.roadName()),
        contains(criteria.roadId()),
        contains(criteria.sectionId()),
        exact(criteria.tenureType()),
        criteria.district(),
        page);
    if (found.isEmpty()) {
      return paged(found);
    }
    Map<List<String>, List<RoadSectionDistrictRow>> rows = sectionRows(found.getContent().stream()
        .map(RoadSearchResult::roadId).collect(Collectors.toSet()));
    return paged(found.map(road -> road.withDistricts(
        districtCodes(rows.getOrDefault(keyOf(road.roadId(), road.sectionId()), List.of())))));
  }

  /**
   * The road section named by a road id and a section id.
   *
   * <p>Case-insensitive, and surrounding spaces ignored. The section's rows are merged into one:
   * name, tenure type, BCTS office and client are taken as {@link #search} takes them
   * ({@code MAX}), so the two never disagree, and every district is listed.
   *
   * @throws RoadSectionNotFoundException if either id is blank, or LRMOPS has no such section
   */
  public RoadSectionResponse find(String roadId, String sectionId) {
    if (!StringUtils.hasText(roadId) || !StringUtils.hasText(sectionId)) {
      throw new RoadSectionNotFoundException(roadId, sectionId);
    }
    List<TfmRoadEntity> rows = roads.findSection(
        roadId.trim().toUpperCase(Locale.ROOT), sectionId.trim().toUpperCase(Locale.ROOT));
    if (rows.isEmpty()) {
      throw new RoadSectionNotFoundException(roadId, sectionId);
    }

    TfmRoadEntity first = rows.get(0);
    String tenureType = max(rows, TfmRoadEntity::getRoadTenureType);
    // One entry per district, in the rows' order, with the lengths of its rows added up.
    Map<List<Object>, List<TfmRoadEntity>> byDistrict = rows.stream()
        .filter(row -> row.getOrgUnitNo() != null || row.getUnitCode() != null)
        .collect(Collectors.groupingBy(
            row -> Arrays.asList(row.getOrgUnitNo(), trimmed(row.getUnitCode())),
            LinkedHashMap::new, Collectors.toList()));
    List<District> districts = byDistrict.values().stream()
        .map(inDistrict -> new District(
            inDistrict.get(0).getOrgUnitNo(),
            trimmed(inDistrict.get(0).getUnitCode()),
            totalLength(inDistrict)))
        .toList();

    return new RoadSectionResponse(
        first.getRoadId(),
        first.getSectionId(),
        collapseSpaces(max(rows, TfmRoadEntity::getRoadName)),
        baseTenureType(tenureType),
        isRetired(tenureType),
        districts,
        max(rows, TfmRoadEntity::getBctsOffice),
        collapseSpaces(max(rows, TfmRoadEntity::getClientName)));
  }

  /**
   * Road names containing {@code term}, for a pick list: names starting with it first, then
   * alphabetically. Case-insensitive; runs of spaces count as one.
   *
   * @param limit names returned, at most {@value #MAX_NAMES}; below 1 is {@value #DEFAULT_NAMES}
   * @return none when the term has fewer than {@value #MIN_NAME_TERM} characters
   */
  public List<String> names(String term, int limit) {
    String text = normalizedName(term);
    if (text == null || text.length() < MIN_NAME_TERM) {
      return List.of();
    }
    String escaped = escapeLike(text);
    int bounded = limit < 1 ? DEFAULT_NAMES : Math.min(limit, MAX_NAMES);
    return roads.findNames(WILDCARD + escaped + WILDCARD, escaped + WILDCARD, bounded);
  }

  /**
   * Every road section named exactly {@code roadName}: case-insensitive, surrounding spaces
   * ignored, runs of spaces counting as one. None when blank.
   */
  public List<RoadSectionKey> sectionsNamed(String roadName) {
    String name = normalizedName(roadName);
    return name == null ? List.of() : roads.findSectionsNamed(name);
  }

  /**
   * The road sections asked for, as {@link #search} returns them, in no particular order. One
   * that LRMOPS does not have is left out, as is a key missing either half.
   *
   * <p>Matched as {@link #find} matches: case-insensitive, surrounding spaces ignored.
   *
   * @throws IllegalArgumentException if more than {@value #MAX_LOOKUP} are asked for
   */
  public List<RoadSearchResult> lookUp(Collection<RoadSectionKey> keys) {
    if (keys.size() > MAX_LOOKUP) {
      throw new IllegalArgumentException(
          "At most " + MAX_LOOKUP + " road sections can be looked up at once");
    }
    Set<List<String>> wanted = keys.stream()
        .filter(key -> key != null
            && StringUtils.hasText(key.roadId()) && StringUtils.hasText(key.sectionId()))
        .map(key -> keyOf(key.roadId(), key.sectionId()))
        .collect(Collectors.toSet());
    if (wanted.isEmpty()) {
      return List.of();
    }
    Set<String> roadIds = wanted.stream().map(key -> key.get(0)).collect(Collectors.toSet());
    return sectionRows(roadIds).entrySet().stream()
        .filter(section -> wanted.contains(section.getKey()))
        .map(section -> merged(section.getValue()))
        .toList();
  }

  /** The given roads' sections, each section's rows (a row per district) under its key. */
  private Map<List<String>, List<RoadSectionDistrictRow>> sectionRows(Set<String> roadIds) {
    return roads.findSectionsOfRoads(roadIds).stream().collect(Collectors.groupingBy(
        row -> keyOf(row.roadId(), row.sectionId()), LinkedHashMap::new, Collectors.toList()));
  }

  /** One section from its rows: name and tenure type by {@code MAX}, as the search takes them. */
  private static RoadSearchResult merged(List<RoadSectionDistrictRow> rows) {
    RoadSectionDistrictRow first = rows.get(0);
    return new RoadSearchResult(first.roadId(), first.sectionId(),
        max(rows, RoadSectionDistrictRow::storedRoadName),
        max(rows, RoadSectionDistrictRow::storedTenureType))
        .withDistricts(districtCodes(rows));
  }

  /** The rows' district codes, each once, alphabetically; blanks left out. */
  private static List<String> districtCodes(List<RoadSectionDistrictRow> rows) {
    return rows.stream().map(RoadSectionDistrictRow::districtCode)
        .filter(StringUtils::hasText).distinct().sorted().toList();
  }

  /** A section's key for matching: both halves trimmed and upper-cased. */
  private static List<String> keyOf(String roadId, String sectionId) {
    return List.of(roadId.trim().toUpperCase(Locale.ROOT),
        sectionId.trim().toUpperCase(Locale.ROOT));
  }

  /** A name as the queries compare it: trimmed, runs of spaces collapsed, upper-cased. */
  private static String normalizedName(String value) {
    return StringUtils.hasText(value)
        ? value.trim().replaceAll(" {2,}", " ").toUpperCase(Locale.ROOT)
        : null;
  }

  /** {@code %}, {@code _} and {@code \} escaped, so they match literally in a {@code LIKE}. */
  private static String escapeLike(String value) {
    return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }

  /** The rows' lengths added up, in metres; null when none of them records one. */
  private static Double totalLength(List<TfmRoadEntity> rows) {
    List<Double> lengths = rows.stream()
        .map(TfmRoadEntity::getRoadLength)
        .filter(Objects::nonNull)
        .toList();
    return lengths.isEmpty() ? null : lengths.stream().mapToDouble(Double::doubleValue).sum();
  }

  /** The greatest trimmed value of a column across the rows, as SQL's MAX(TRIM(…)); nulls skipped. */
  private static <R> String max(List<R> rows, Function<R, String> column) {
    return rows.stream()
        .map(column)
        .map(RoadService::trimmed)
        .filter(Objects::nonNull)
        .max(Comparator.naturalOrder())
        .orElse(null);
  }

  private static String trimmed(String value) {
    return value == null ? null : value.trim();
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
      case DISTRICT -> "MIN(TRIM(road.unitCode))";
    };
  }

  private static int boundedPageSize(int requested) {
    if (requested < 1) {
      return DEFAULT_PAGE_SIZE;
    }
    return Math.min(requested, MAX_PAGE_SIZE);
  }

  /** A code to match exactly: trimmed and upper-cased. Null when blank. */
  static String exact(String value) {
    return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
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
