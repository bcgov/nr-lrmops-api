package ca.bc.gov.nrs.lrmops.struct.v1;

/**
 * A road tenure type: a value {@code tenureType} on {@code GET /api/v1/roads} matches, and that
 * road results carry.
 *
 * @param code the code, e.g. {@code FSR}; never a retired variant
 * @param description what it means, e.g. "Forest Service Road"; null when LRMOPS has none
 */
public record RoadTenureTypeResponse(String code, String description) {
}
