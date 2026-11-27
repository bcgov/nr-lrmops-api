package ca.bc.gov.nrs.lrmops.struct.v1;

/**
 * The columns a road search can be sorted by ({@code sortBy}).
 *
 * <p>With none given, results come in road name, road id, section id order. That order also
 * breaks ties after the columns asked for, so paging never repeats or skips a row.
 */
public enum RoadSortColumn {
  ROAD_NAME,
  ROAD_ID,
  SECTION_ID,
  TENURE_TYPE,
  /** By the section's first district code, alphabetically. */
  DISTRICT
}
