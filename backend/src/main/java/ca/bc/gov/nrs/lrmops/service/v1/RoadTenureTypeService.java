package ca.bc.gov.nrs.lrmops.service.v1;

import ca.bc.gov.nrs.lrmops.repository.v1.TfmRoadTenureTypeRepository;
import ca.bc.gov.nrs.lrmops.struct.v1.RoadTenureTypeResponse;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** LRMOPS's road tenure types, from {@code TFM_VT_ROAD_TENURE_TYPE}. */
@Service
@Transactional(readOnly = true)
public class RoadTenureTypeService {

  private final TfmRoadTenureTypeRepository tenureTypes;

  public RoadTenureTypeService(TfmRoadTenureTypeRepository tenureTypes) {
    this.tenureTypes = tenureTypes;
  }

  /**
   * Every road tenure type, by code: the values {@code tenureType} on the road search matches,
   * retired variants ({@code RP-RETIRE}) included.
   *
   * <p>Left out: entries whose code has no letter or digit — LRMOPS's {@code !}, "Geometry only",
   * which marks a line with no tenure rather than a tenure type. Codes and descriptions trimmed.
   */
  public List<RoadTenureTypeResponse> list() {
    return tenureTypes.findAll().stream()
        .filter(type -> isTenureCode(type.getRoadTenureType()))
        .map(type -> new RoadTenureTypeResponse(
            type.getRoadTenureType().trim(),
            type.getDescription() == null ? null : type.getDescription().trim()))
        .sorted(Comparator.comparing(RoadTenureTypeResponse::code))
        .toList();
  }

  private static boolean isTenureCode(String code) {
    return code != null && code.chars().anyMatch(Character::isLetterOrDigit);
  }
}
