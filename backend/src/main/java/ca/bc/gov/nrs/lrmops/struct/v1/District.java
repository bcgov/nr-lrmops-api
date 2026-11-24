package ca.bc.gov.nrs.lrmops.struct.v1;

/**
 * A district a road section lies in.
 *
 * @param orgUnitNo {@code ORG_UNIT_NO}, numbered as CBR numbers org units, e.g. {@code 18}
 * @param code {@code UNITCODE}, e.g. {@code DPG}
 * @param lengthMetres how much of the section lies in this district, in metres — the sum of
 *     {@code ROADLENGTH} over the section's rows in it; null when LRMOPS records none
 */
public record District(Long orgUnitNo, String code, Double lengthMetres) {
}
