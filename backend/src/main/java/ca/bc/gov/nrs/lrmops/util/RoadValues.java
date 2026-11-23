package ca.bc.gov.nrs.lrmops.util;

import java.util.Locale;

/** How LRMOPS stores road values, and how the API returns them. */
public final class RoadValues {

  /** The suffix LRMOPS appends to a retired road's tenure type, e.g. {@code RP-RETIRE}. */
  public static final String RETIRED_SUFFIX = "-RETIRE";

  private RoadValues() {
  }

  /** Trimmed, runs of spaces collapsed: LRMOPS stores names like {@code "   AM  4"}. */
  public static String collapseSpaces(String value) {
    return value == null ? null : value.trim().replaceAll(" {2,}", " ");
  }

  /** Whether a stored tenure type marks the road retired. */
  public static boolean isRetired(String tenureType) {
    return tenureType != null
        && tenureType.trim().toUpperCase(Locale.ROOT).endsWith(RETIRED_SUFFIX);
  }

  /** The tenure type without its retired suffix: {@code RP-RETIRE} is {@code RP}. */
  public static String baseTenureType(String tenureType) {
    if (tenureType == null) {
      return null;
    }
    String trimmed = tenureType.trim();
    return isRetired(trimmed)
        ? trimmed.substring(0, trimmed.length() - RETIRED_SUFFIX.length())
        : trimmed;
  }
}
