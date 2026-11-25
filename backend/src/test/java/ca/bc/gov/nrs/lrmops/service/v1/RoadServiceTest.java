package ca.bc.gov.nrs.lrmops.service.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.bc.gov.nrs.lrmops.exception.RoadSectionNotFoundException;
import ca.bc.gov.nrs.lrmops.struct.v1.District;
import ca.bc.gov.nrs.lrmops.struct.v1.HeaderSort;
import ca.bc.gov.nrs.lrmops.struct.v1.PagedResponse;
import ca.bc.gov.nrs.lrmops.struct.v1.RoadSearchCriteria;
import ca.bc.gov.nrs.lrmops.struct.v1.RoadSearchResult;
import ca.bc.gov.nrs.lrmops.struct.v1.RoadSectionKey;
import ca.bc.gov.nrs.lrmops.struct.v1.RoadSectionResponse;
import ca.bc.gov.nrs.lrmops.struct.v1.RoadSortColumn;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

/** Road search and section lookup through the service and the Hibernate query, on H2 seeded by tfm-road-data.sql. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(RoadService.class)
@Sql("/tfm-road-data.sql")
class RoadServiceTest {

  private static final RoadSearchCriteria ANY = new RoadSearchCriteria(null, null, null, null, null);

  @Autowired
  private RoadService service;

  @Autowired
  private jakarta.persistence.EntityManager entityManager;

  private PagedResponse<RoadSearchResult> search(RoadSearchCriteria criteria) {
    return service.search(criteria, 0, 50, List.of());
  }

  private static List<String> pairs(PagedResponse<RoadSearchResult> page) {
    return page.content().stream().map(r -> r.roadId() + "/" + r.sectionId()).toList();
  }

  @Test
  void oneRowPerPairInRoadNameOrder() {
    PagedResponse<RoadSearchResult> page = search(ANY);

    // Two-district section once; NEWGEOMETRY-only and section-less rows not at all; no name last.
    assertThat(pairs(page)).containsExactly(
        "S00100/a 1", "R08317/I", "R07571/AA", "9456/01", "R00001/B");
    assertThat(page.totalElements()).isEqualTo(5);
  }

  @Test
  void namesAreTrimmedAndRetiredRoadsFlagged() {
    RoadSearchResult road = search(new RoadSearchCriteria(null, "R08317", null, null, null))
        .content().get(0);

    assertThat(road).isEqualTo(new RoadSearchResult("R08317", "I", "AM 4", "RP", true, "CANFOR"));
  }

  @Test
  void anEditInProgressShowsTheOldGeometry() {
    RoadSearchResult road = search(new RoadSearchCriteria(null, "9456", null, null, null))
        .content().get(0);

    assertThat(road.roadName()).isEqualTo("CEDAR ROAD");
    assertThat(road.retired()).isFalse();
  }

  @Test
  void nameMatchIgnoresCaseAndExtraSpaces() {
    assertThat(pairs(search(new RoadSearchCriteria("  am   4 ", null, null, null, null))))
        .containsExactly("R08317/I");
  }

  @Test
  void everyCriterionIsAPartialMatch() {
    assertThat(pairs(search(new RoadSearchCriteria(null, "0757", "a", null, null))))
        .containsExactly("R07571/AA");
    assertThat(pairs(search(new RoadSearchCriteria(null, null, null, null, "fraser"))))
        .containsExactly("9456/01");
  }

  @Test
  void tenureTypeIsAnExactCode() {
    assertThat(pairs(search(new RoadSearchCriteria(null, null, null, " rp ", null))))
        .containsExactly("9456/01");
    assertThat(pairs(search(new RoadSearchCriteria(null, null, null, "rp-retire", null))))
        .containsExactly("R08317/I");
    assertThat(search(new RoadSearchCriteria(null, null, null, "retire", null)).content())
        .isEmpty();
  }

  @Test
  void wildcardsInInputMatchLiterally() {
    assertThat(pairs(search(new RoadSearchCriteria("100%", null, null, null, null))))
        .containsExactly("S00100/a 1");
    assertThat(pairs(search(new RoadSearchCriteria("N_H", null, null, null, null))))
        .containsExactly("S00100/a 1");
    assertThat(search(new RoadSearchCriteria("%", null, null, null, null)).content()).hasSize(1);
    assertThat(search(new RoadSearchCriteria("_", null, null, null, null)).content()).hasSize(1);
  }

  @Test
  void blankCriteriaAreIgnored() {
    assertThat(search(new RoadSearchCriteria(" ", "", null, "  ", null)).totalElements())
        .isEqualTo(5);
  }

  @Test
  void noMatchIsAnEmptyPage() {
    PagedResponse<RoadSearchResult> page =
        search(new RoadSearchCriteria("NOWHERE", null, null, null, null));

    assertThat(page.content()).isEmpty();
    assertThat(page.totalElements()).isZero();
  }

  @Test
  void sortsByTheColumnsAskedThenTheDefaultOrder() {
    PagedResponse<RoadSearchResult> page = service.search(ANY, 0, 50,
        List.of(new HeaderSort<>(RoadSortColumn.TENURE_TYPE, Sort.Direction.DESC)));

    // By the stored type, descending: SUP, RP-RETIRE, RP, OTH, FSR.
    assertThat(pairs(page)).containsExactly(
        "S00100/a 1", "R08317/I", "9456/01", "R00001/B", "R07571/AA");
  }

  @Test
  void sortsByEveryColumn() {
    for (RoadSortColumn column : RoadSortColumn.values()) {
      assertThat(service.search(ANY, 0, 50,
          List.of(new HeaderSort<>(column, Sort.Direction.DESC))).content()).hasSize(5);
    }
  }

  @Test
  void pagesCarryTheTotal() {
    PagedResponse<RoadSearchResult> second = service.search(ANY, 1, 2, List.of());

    assertThat(pairs(second)).containsExactly("R07571/AA", "9456/01");
    assertThat(second.totalElements()).isEqualTo(5);
    assertThat(second.totalPages()).isEqualTo(3);
  }

  @Test
  void aPagePastTheEndStillCounts() {
    PagedResponse<RoadSearchResult> page = service.search(ANY, 10, 2, List.of());

    assertThat(page.content()).isEmpty();
    assertThat(page.totalElements()).isEqualTo(5);
  }

  @Test
  void pageSizeIsBounded() {
    assertThat(service.search(ANY, 0, 0, List.of()).pageSize())
        .isEqualTo(RoadService.DEFAULT_PAGE_SIZE);
    assertThat(service.search(ANY, 0, 10_000, List.of()).pageSize())
        .isEqualTo(RoadService.MAX_PAGE_SIZE);
    assertThat(service.search(ANY, -1, 2, List.of()).pageNumber()).isZero();
  }

  @Test
  void containsEscapesLikeWildcards() {
    assertThat(RoadService.contains(" a\\b%c_d  e ")).isEqualTo("%A\\\\B\\%C\\_D E%");
    assertThat(RoadService.contains("  ")).isNull();
  }

  @Test
  void findsASectionWithItsDistrictAndBctsOffice() {
    assertThat(service.find("R08317", "I")).isEqualTo(new RoadSectionResponse(
        "R08317", "I", "AM 4", "RP", true, List.of(new District(18L, "DPG", 1500.0)), "TPG", "CANFOR"));
  }

  @Test
  void aSectionAcrossTwoDistrictsListsBoth() {
    RoadSectionResponse section = service.find("R07571", "AA");

    assertThat(section.districts())
        .containsExactly(new District(18L, "DPG", 629.0), new District(19L, "DMH", 774.0));
    assertThat(section.roadName()).isEqualTo("BIG CREEK FSR");
  }

  @Test
  void anEditInProgressIsTheOldGeometryOnly() {
    RoadSectionResponse section = service.find("9456", "01");

    assertThat(section.roadName()).isEqualTo("CEDAR ROAD");
    assertThat(section.districts()).containsExactly(new District(18L, "DPG", 2000.0));
  }

  @Test
  void lookupIgnoresCaseAndSurroundingSpaces() {
    RoadSectionResponse section = service.find(" s00100 ", " A 1 ");

    // Returned as LRMOPS stores it.
    assertThat(section.roadId()).isEqualTo("S00100");
    assertThat(section.sectionId()).isEqualTo("a 1");
  }

  @Test
  void aSectionWithNoNameOrDistrictIsStillFound() {
    RoadSectionResponse section = service.find("R00001", "B");

    assertThat(section.roadName()).isNull();
    assertThat(section.districts()).isEmpty();
  }

  @Test
  void lookupIsExactNotPartial() {
    assertThatThrownBy(() -> service.find("R07571", "A"))
        .isInstanceOf(RoadSectionNotFoundException.class);
    assertThatThrownBy(() -> service.find("R0757", "AA"))
        .isInstanceOf(RoadSectionNotFoundException.class);
  }

  @Test
  void unknownNewGeometryOnlyOrBlankIsNotFound() {
    assertThatThrownBy(() -> service.find("NOPE", "1"))
        .isInstanceOf(RoadSectionNotFoundException.class);
    assertThatThrownBy(() -> service.find("9999", "01"))
        .isInstanceOf(RoadSectionNotFoundException.class);
    assertThatThrownBy(() -> service.find(" ", "I"))
        .isInstanceOf(RoadSectionNotFoundException.class);
  }

  // ── Road names, sections by name, lookup ──────────────────────────

  @Test
  void namesContainingTheTermStartingWithItFirst() {
    // Both contain "CE"; CEDAR ROAD starts with it. NEWGEOMETRY's CEDAR ROAD NEW is left out.
    assertThat(service.names("ce", 20)).containsExactly("CEDAR ROAD");
    assertThat(service.names("r", 20)).isEmpty();
    assertThat(service.names("o", 20)).isEmpty();
    assertThat(service.names("RO", 20)).containsExactly("CEDAR ROAD");
  }

  @Test
  void namesAreCollapsedDistinctAndLimited() {
    assertThat(service.names("  am   4", 20)).containsExactly("AM 4");
    // BIG CREEK FSR has two rows, one per district: listed once.
    assertThat(service.names("CREEK", 20)).containsExactly("BIG CREEK FSR");
    assertThat(service.names("% D", 20)).containsExactly("100% DOWN_HILL");
    assertThat(service.names("_", 20)).isEmpty();
    assertThat(service.names("A", 20)).isEmpty();
  }

  @Test
  void namesStartingWithTheTermComeFirst() {
    // Contains "CE" and sorts before CEDAR ROAD, but doesn't start with "CE".
    entityManager.createNativeQuery("INSERT INTO tfm.tfm_road VALUES"
        + " (10, 'R00003', 'A', 'ACE ROAD', 'RP', NULL, NULL, 18, 'DPG', NULL, 10)")
        .executeUpdate();

    assertThat(service.names("CE", 20)).containsExactly("CEDAR ROAD", "ACE ROAD");
    assertThat(service.names("CE", 1)).containsExactly("CEDAR ROAD");
  }

  @Test
  void sectionsNamedExactlyIgnoringCaseAndSpaces() {
    assertThat(service.sectionsNamed(" big  creek fsr "))
        .containsExactly(new RoadSectionKey("R07571", "AA"));
    assertThat(service.sectionsNamed("am 4")).containsExactly(new RoadSectionKey("R08317", "I"));
    assertThat(service.sectionsNamed("BIG CREEK")).isEmpty();
    assertThat(service.sectionsNamed("CEDAR ROAD NEW")).isEmpty();
    assertThat(service.sectionsNamed("  ")).isEmpty();
  }

  @Test
  void looksUpTheSectionsAskedForOnly() {
    List<RoadSearchResult> found = service.lookUp(List.of(
        new RoadSectionKey("r08317", " i "),
        new RoadSectionKey("R07571", "AA"),
        new RoadSectionKey("S00100", "A 1"),
        new RoadSectionKey("R07571", "ZZ"),
        new RoadSectionKey("9999", "01"),
        new RoadSectionKey(null, "01")));

    assertThat(found).extracting(r -> r.roadId() + "/" + r.sectionId() + "=" + r.roadName())
        .containsExactlyInAnyOrder("R08317/I=AM 4", "R07571/AA=BIG CREEK FSR",
            "S00100/a 1=100% DOWN_HILL");
    assertThat(service.lookUp(List.of())).isEmpty();
  }

  @Test
  void refusesTooManySectionsAtOnce() {
    List<RoadSectionKey> many = java.util.stream.IntStream.rangeClosed(0, RoadService.MAX_LOOKUP)
        .mapToObj(i -> new RoadSectionKey("R" + i, "A")).toList();

    assertThatThrownBy(() -> service.lookUp(many)).isInstanceOf(IllegalArgumentException.class);
  }
}
