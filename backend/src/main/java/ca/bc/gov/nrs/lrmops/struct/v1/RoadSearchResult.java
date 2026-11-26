package ca.bc.gov.nrs.lrmops.struct.v1;

import java.util.List;
import static ca.bc.gov.nrs.lrmops.util.RoadValues.baseTenureType;
import static ca.bc.gov.nrs.lrmops.util.RoadValues.collapseSpaces;
import static ca.bc.gov.nrs.lrmops.util.RoadValues.isRetired;

/**
 * One road section, as a road search returns it: once per road and section, however many
 * {@code TFM_ROAD} rows it has.
 *
 * @param roadId {@code ROAD_ID}
 * @param sectionId {@code SECTION_ID}
 * @param roadName the name, trimmed, runs of spaces collapsed; null when LRMOPS has none
 * @param tenureType the tenure type without the retired suffix; null when LRMOPS has none
 * @param retired whether LRMOPS marks the road retired (tenure type ending {@code -RETIRE})
 * @param districts the codes of the Forest Districts the section lies in, e.g. {@code DPG},
 *     alphabetically: usually one, two when it crosses a district boundary; empty when LRMOPS
 *     records none
 */
public record RoadSearchResult(
    String roadId,
    String sectionId,
    String roadName,
    String tenureType,
    boolean retired,
    List<String> districts
) {

  public RoadSearchResult {
    districts = districts == null ? List.of() : List.copyOf(districts);
  }

  /**
   * From the search query's columns as stored: the tenure type with its {@code -RETIRE} suffix,
   * the name with LRMOPS's extra spaces. The districts come after, from {@link #withDistricts}.
   */
  public RoadSearchResult(String roadId, String sectionId, String storedRoadName,
      String storedTenureType) {
    this(roadId, sectionId, collapseSpaces(storedRoadName), baseTenureType(storedTenureType),
        isRetired(storedTenureType), List.of());
  }

  /** The same section, with its districts. */
  public RoadSearchResult withDistricts(List<String> codes) {
    return new RoadSearchResult(roadId, sectionId, roadName, tenureType, retired, codes);
  }
}
