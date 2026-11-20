package ca.bc.gov.nrs.lrmops.model.v1;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Immutable;

/**
 * {@code TFM.TFM_ROAD}: LRMOPS's road sections, loaded nightly.
 *
 * <p><b>An ArcSDE feature table, not a road register.</b> It has no primary key on the road, and
 * one road and section can have several rows:
 * <ul>
 *   <li>one per district, when a section crosses a district boundary;</li>
 *   <li>an {@code OLDGEOMETRY} and a {@code NEWGEOMETRY} row while an edit is in progress
 *       ({@code ROADSTATUS}; null otherwise).</li>
 * </ul>
 * So the entity is keyed by {@code OBJECTID}, ArcSDE's row id (unique, {@code R27_SDE_ROWID_UK}),
 * and queries group by {@link #roadId} and {@link #sectionId}.
 *
 * <p>Read-only: the API's database user holds SELECT only.
 */
@Entity
@Immutable
@Table(name = "TFM_ROAD", schema = "TFM")
@Getter
@ToString
@EqualsAndHashCode(of = "objectId")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TfmRoadEntity {

  @Id
  @Column(name = "OBJECTID")
  private Long objectId;

  /** The road: a road permit or forest service road id, e.g. {@code R08317}. */
  @Column(name = "ROAD_ID", length = 10)
  private String roadId;

  /** The section of the road, e.g. {@code I}. Free text: {@code 352002}, {@code SEC 3}. */
  @Column(name = "SECTION_ID", length = 30)
  private String sectionId;

  /** Stored with leading and double spaces, e.g. {@code "   AM  4"}. */
  @Column(name = "ROAD_NAME", length = 50)
  private String roadName;

  /**
   * {@code RP}, {@code FSR}, {@code SUP}, {@code CP}, {@code NSR} or {@code OTH}; a retired road's
   * ends in {@code -RETIRE}, e.g. {@code RP-RETIRE}.
   */
  @Column(name = "ROADTENURETYPE", length = 10)
  private String roadTenureType;

  /** Null, {@code OLDGEOMETRY} or {@code NEWGEOMETRY}. Not active/retired. */
  @Column(name = "ROADSTATUS", length = 20)
  private String roadStatus;

  /** The tenure holder's name. TFM has no client number. */
  @Column(name = "CLIENT_NAME", length = 60)
  private String clientName;
}
