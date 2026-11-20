package ca.bc.gov.nrs.lrmops.struct.v1;

/**
 * What a road search asks on. Every criterion is optional, and each is a case-insensitive partial
 * match; blank counts as not given.
 *
 * @param roadName {@code ROAD_NAME}. Runs of spaces count as one, so {@code "AM 4"} finds
 *     {@code "   AM  4"}
 * @param roadId {@code ROAD_ID}, e.g. {@code R08317}
 * @param sectionId {@code SECTION_ID}
 * @param tenureType {@code ROADTENURETYPE}: {@code RP}, {@code FSR}, {@code SUP}, {@code CP},
 *     {@code NSR}, {@code OTH}. {@code RP} finds active and retired road permits;
 *     {@code RETIRE} finds only retired roads
 * @param clientName {@code CLIENT_NAME}, the tenure holder
 */
public record RoadSearchCriteria(
    String roadName,
    String roadId,
    String sectionId,
    String tenureType,
    String clientName
) {
}
