package ca.bc.gov.nrs.lrmops.struct.v1;

/**
 * A road section by its key: a road id and a section id, as LRMOPS stores them.
 *
 * <p>What {@code GET /api/v1/roads/sections} returns for a road name, and what
 * {@code POST /api/v1/roads/sections/lookup} takes. Callers that record road sections (CBR's
 * {@code FOREST_FILE_ID} and {@code ROAD_SECTION_ID}) hold these, not names.
 *
 * @param roadId {@code ROAD_ID}, e.g. {@code R08317}
 * @param sectionId {@code SECTION_ID}, e.g. {@code I}
 */
public record RoadSectionKey(String roadId, String sectionId) {
}
