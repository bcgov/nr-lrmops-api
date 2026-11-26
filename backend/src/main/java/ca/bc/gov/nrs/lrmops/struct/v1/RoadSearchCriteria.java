package ca.bc.gov.nrs.lrmops.struct.v1;

/**
 * What a road search asks on. Every criterion is optional, and each is a case-insensitive partial
 * match except the tenure type and the district, which are exact; blank counts as not given.
 *
 * @param roadName {@code ROAD_NAME}. Runs of spaces count as one, so {@code "AM 4"} finds
 *     {@code "   AM  4"}
 * @param roadId {@code ROAD_ID}, e.g. {@code R08317}
 * @param sectionId {@code SECTION_ID}
 * @param tenureType {@code ROADTENURETYPE}, exactly (case-insensitive): a code from
 *     {@code GET /api/v1/road-tenure-types}. {@code RP} finds active road permits only;
 *     {@code RP-RETIRE} finds retired ones
 * @param district {@code ORG_UNIT_NO}, the Forest District's org unit number (e.g. {@code 18},
 *     Prince George). A section crossing a district boundary is found under each of its
 *     districts. In place of the tenure holder's {@code CLIENT_NAME}, which is free text set when
 *     the tenure was created and not updated when the road changes hands (business, 2026-11-26)
 */
public record RoadSearchCriteria(
    String roadName,
    String roadId,
    String sectionId,
    String tenureType,
    Long district
) {
}
