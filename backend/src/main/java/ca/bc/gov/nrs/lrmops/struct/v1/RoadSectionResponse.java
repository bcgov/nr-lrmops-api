package ca.bc.gov.nrs.lrmops.struct.v1;

import java.util.List;

/**
 * One road section from LRMOPS.
 *
 * @param roadId {@code ROAD_ID}, as LRMOPS stores it
 * @param sectionId {@code SECTION_ID}, as LRMOPS stores it
 * @param roadName the name, trimmed, runs of spaces collapsed; null when LRMOPS has none
 * @param tenureType the tenure type without the retired suffix; null when LRMOPS has none
 * @param retired whether LRMOPS marks the road retired (tenure type ending {@code -RETIRE})
 * @param districts every district the section lies in, by org unit number. <b>Usually one; two
 *     when the section crosses a district boundary</b>, and the caller decides which applies.
 *     Empty when LRMOPS records none
 * @param bctsOffice the BC Timber Sales office, e.g. {@code TPG}; null for most roads
 */
public record RoadSectionResponse(
    String roadId,
    String sectionId,
    String roadName,
    String tenureType,
    boolean retired,
    List<District> districts,
    String bctsOffice
) {
}
