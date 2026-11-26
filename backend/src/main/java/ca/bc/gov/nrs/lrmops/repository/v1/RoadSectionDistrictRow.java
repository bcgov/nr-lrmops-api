package ca.bc.gov.nrs.lrmops.repository.v1;

/**
 * One road section's rows in one district, as {@link TfmRoadRepository#findSectionsOfRoads} groups
 * them: a section crossing a district boundary has one of these per district.
 *
 * @param roadId {@code ROAD_ID}
 * @param sectionId {@code SECTION_ID}
 * @param storedRoadName {@code ROAD_NAME} as stored ({@code MAX} of the rows)
 * @param storedTenureType {@code ROADTENURETYPE} as stored, with any {@code -RETIRE} suffix
 * @param districtCode {@code UNITCODE}, trimmed, e.g. {@code DPG}; null when the rows have none
 */
public record RoadSectionDistrictRow(
    String roadId,
    String sectionId,
    String storedRoadName,
    String storedTenureType,
    String districtCode
) {
}
