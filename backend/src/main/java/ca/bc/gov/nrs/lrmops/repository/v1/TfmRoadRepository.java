package ca.bc.gov.nrs.lrmops.repository.v1;

import ca.bc.gov.nrs.lrmops.model.v1.TfmRoadEntity;
import ca.bc.gov.nrs.lrmops.struct.v1.RoadSearchResult;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Road sections, over {@code TFM.TFM_ROAD}.
 *
 * <p>Read-only: the API's database user holds SELECT only, so the inherited write methods would
 * fail at the database.
 */
@Repository
public interface TfmRoadRepository extends JpaRepository<TfmRoadEntity, Long> {

  /**
   * The road search.
   *
   * <h2>One row per road and section</h2>
   * The table can hold several rows for a pair (see {@link TfmRoadEntity}), so the rows are
   * grouped. Name, tenure type and client are taken with {@code MAX}: a pair's rows have never
   * been seen to disagree on the name, and {@code MAX} picks one value the same way every time if
   * the others ever do.
   *
   * <h2>Which rows count</h2>
   * <ul>
   *   <li>{@code ROADSTATUS} null or {@code OLDGEOMETRY}. {@code NEWGEOMETRY} rows are the other
   *       half of an edit in progress; LRMOPS's own CBR export drops them the same way.</li>
   *   <li>A road id and a section id both present: a row missing either can't be asked for.</li>
   *   <li>Retired roads included (a decision pending with the business), flagged on the
   *       result.</li>
   * </ul>
   *
   * <h2>Null-guarded parameters</h2>
   * Each criterion is a {@code LIKE} pattern already upper-cased, escaped and wrapped in
   * {@code %} by the service, or null when not given. The name is compared with runs of spaces
   * collapsed. There is no index on {@code ROAD_NAME} or {@code CLIENT_NAME}, so a search on
   * either reads the whole table (about 300k rows).
   *
   * <h2>Ordered by the caller</h2>
   * The order comes on the {@code Pageable}, built by {@code RoadService}. It may name only the
   * grouping columns or the aggregates selected, as {@code GROUP BY} requires.
   *
   * <h2>Counted from a derived table</h2>
   * The count is of distinct road and section pairs, which Oracle can't count with a
   * multi-column {@code COUNT(DISTINCT …)}; it counts the same groups the search pages over.
   */
  @Query(value = """
      SELECT new ca.bc.gov.nrs.lrmops.struct.v1.RoadSearchResult(
               road.roadId,
               road.sectionId,
               MAX(TRIM(road.roadName)),
               MAX(TRIM(road.roadTenureType)),
               MAX(TRIM(road.clientName)))
        FROM TfmRoadEntity road
       WHERE COALESCE(road.roadStatus, 'OLDGEOMETRY') = 'OLDGEOMETRY'
         AND road.roadId IS NOT NULL
         AND road.sectionId IS NOT NULL
         AND (:roadName IS NULL
              OR UPPER(FUNCTION('REGEXP_REPLACE', TRIM(road.roadName), ' {2,}', ' '))
                 LIKE :roadName ESCAPE '\\')
         AND (:roadId IS NULL OR UPPER(road.roadId) LIKE :roadId ESCAPE '\\')
         AND (:sectionId IS NULL OR UPPER(road.sectionId) LIKE :sectionId ESCAPE '\\')
         AND (:tenureType IS NULL OR UPPER(road.roadTenureType) LIKE :tenureType ESCAPE '\\')
         AND (:clientName IS NULL
              OR UPPER(FUNCTION('REGEXP_REPLACE', TRIM(road.clientName), ' {2,}', ' '))
                 LIKE :clientName ESCAPE '\\')
       GROUP BY road.roadId, road.sectionId
      """,
      countQuery = """
      SELECT COUNT(*)
        FROM (SELECT road.roadId AS roadId,
                     road.sectionId AS sectionId
                FROM TfmRoadEntity road
               WHERE COALESCE(road.roadStatus, 'OLDGEOMETRY') = 'OLDGEOMETRY'
                 AND road.roadId IS NOT NULL
                 AND road.sectionId IS NOT NULL
                 AND (:roadName IS NULL
                      OR UPPER(FUNCTION('REGEXP_REPLACE', TRIM(road.roadName), ' {2,}', ' '))
                         LIKE :roadName ESCAPE '\\')
                 AND (:roadId IS NULL OR UPPER(road.roadId) LIKE :roadId ESCAPE '\\')
                 AND (:sectionId IS NULL OR UPPER(road.sectionId) LIKE :sectionId ESCAPE '\\')
                 AND (:tenureType IS NULL
                      OR UPPER(road.roadTenureType) LIKE :tenureType ESCAPE '\\')
                 AND (:clientName IS NULL
                      OR UPPER(FUNCTION('REGEXP_REPLACE', TRIM(road.clientName), ' {2,}', ' '))
                         LIKE :clientName ESCAPE '\\')
               GROUP BY road.roadId, road.sectionId
             ) matched
      """)
  Page<RoadSearchResult> search(
      @Param("roadName") String roadName,
      @Param("roadId") String roadId,
      @Param("sectionId") String sectionId,
      @Param("tenureType") String tenureType,
      @Param("clientName") String clientName,
      Pageable page);

  /**
   * Every row that counts for one road section: one per district it crosses, usually one.
   *
   * <p>{@code NEWGEOMETRY} rows left out, as in {@link #search}. The road id is matched as given,
   * so the index on {@code ROAD_ID} is used; the service upper-cases it, which is how LRMOPS
   * stores road ids. The section id is compared upper-cased on both sides, as it is free text.
   *
   * @param roadId the road id, trimmed and upper-cased
   * @param sectionId the section id, trimmed and upper-cased
   */
  @Query("""
      SELECT road
        FROM TfmRoadEntity road
       WHERE road.roadId = :roadId
         AND UPPER(road.sectionId) = :sectionId
         AND COALESCE(road.roadStatus, 'OLDGEOMETRY') = 'OLDGEOMETRY'
       ORDER BY road.orgUnitNo, road.objectId
      """)
  List<TfmRoadEntity> findSection(
      @Param("roadId") String roadId,
      @Param("sectionId") String sectionId);
}
