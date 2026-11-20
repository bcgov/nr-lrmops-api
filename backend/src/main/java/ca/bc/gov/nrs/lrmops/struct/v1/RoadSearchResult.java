package ca.bc.gov.nrs.lrmops.struct.v1;

import java.util.Locale;

/**
 * One road section, as a road search returns it: once per road and section, however many
 * {@code TFM_ROAD} rows it has.
 *
 * @param roadId {@code ROAD_ID}
 * @param sectionId {@code SECTION_ID}
 * @param roadName the name, trimmed, runs of spaces collapsed; null when LRMOPS has none
 * @param tenureType the tenure type without the retired suffix; null when LRMOPS has none
 * @param retired whether LRMOPS marks the road retired (tenure type ending {@code -RETIRE})
 * @param clientName the tenure holder's name; null when LRMOPS has none
 */
public record RoadSearchResult(
    String roadId,
    String sectionId,
    String roadName,
    String tenureType,
    boolean retired,
    String clientName
) {

  /** The suffix LRMOPS appends to a retired road's tenure type. */
  static final String RETIRED_SUFFIX = "-RETIRE";

  /**
   * From the search query's columns as stored: the tenure type with its {@code -RETIRE} suffix,
   * the name with LRMOPS's extra spaces.
   */
  public RoadSearchResult(String roadId, String sectionId, String storedRoadName,
      String storedTenureType, String clientName) {
    this(roadId, sectionId, collapseSpaces(storedRoadName), baseType(storedTenureType),
        isRetired(storedTenureType), collapseSpaces(clientName));
  }

  private static boolean isRetired(String tenureType) {
    return tenureType != null
        && tenureType.trim().toUpperCase(Locale.ROOT).endsWith(RETIRED_SUFFIX);
  }

  private static String baseType(String tenureType) {
    if (tenureType == null) {
      return null;
    }
    String trimmed = tenureType.trim();
    return isRetired(trimmed)
        ? trimmed.substring(0, trimmed.length() - RETIRED_SUFFIX.length())
        : trimmed;
  }

  private static String collapseSpaces(String value) {
    return value == null ? null : value.trim().replaceAll(" {2,}", " ");
  }
}
