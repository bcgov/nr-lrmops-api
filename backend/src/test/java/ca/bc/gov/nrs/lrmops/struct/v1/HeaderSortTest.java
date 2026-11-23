package ca.bc.gov.nrs.lrmops.struct.v1;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

class HeaderSortTest {

  @Test
  void pairsColumnsWithDirectionsByPosition() {
    assertThat(HeaderSort.of(
        List.of(RoadSortColumn.ROAD_ID, RoadSortColumn.ROAD_NAME, RoadSortColumn.ROAD_ID),
        List.of(Sort.Direction.DESC)))
        .containsExactly(
            new HeaderSort<>(RoadSortColumn.ROAD_ID, Sort.Direction.DESC),
            new HeaderSort<>(RoadSortColumn.ROAD_NAME, Sort.Direction.ASC));
  }

  @Test
  void noColumnsIsTheDefaultOrder() {
    assertThat(HeaderSort.<RoadSortColumn>of(null, null)).isEmpty();
  }
}
