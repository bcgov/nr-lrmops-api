package ca.bc.gov.nrs.lrmops.repository.v1;

import ca.bc.gov.nrs.lrmops.model.v1.TfmRoadTenureTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Road tenure types, over {@code TFM.TFM_VT_ROAD_TENURE_TYPE}.
 *
 * <p>Read-only: the API's database user holds SELECT only, so the inherited write methods would
 * fail at the database.
 */
@Repository
public interface TfmRoadTenureTypeRepository
    extends JpaRepository<TfmRoadTenureTypeEntity, String> {
}
