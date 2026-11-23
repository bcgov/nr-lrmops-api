package ca.bc.gov.nrs.lrmops.model.v1;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Immutable;

/**
 * {@code TFM.TFM_VT_ROAD_TENURE_TYPE}: LRMOPS's road tenure types, the code table behind
 * {@code TFM_ROAD.ROADTENURETYPE}.
 *
 * <p>Holds the retired variants ({@code RP-RETIRE}) and {@code !} ("Geometry only") too.
 *
 * <p>Read-only: the API's database user holds SELECT only.
 */
@Entity
@Immutable
@Table(name = "TFM_VT_ROAD_TENURE_TYPE", schema = "TFM")
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TfmRoadTenureTypeEntity {

  /** The code, as {@code TFM_ROAD.ROADTENURETYPE} stores it: {@code FSR}, {@code RP-RETIRE}, … */
  @Id
  @Column(name = "ROADTENURETYPE", length = 10)
  private String roadTenureType;

  /** What the code means, e.g. "Forest Service Road". */
  @Column(name = "DESCRIPTION")
  private String description;
}
