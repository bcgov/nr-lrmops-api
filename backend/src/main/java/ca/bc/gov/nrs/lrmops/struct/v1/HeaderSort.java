package ca.bc.gov.nrs.lrmops.struct.v1;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.springframework.data.domain.Sort;

/** One column a search is sorted by, and its direction. */
public record HeaderSort<C extends Enum<C>>(C column, Sort.Direction direction) {

  /**
   * The columns as sent, each with its direction: paired by position, a missing direction
   * ascending, and a column named twice taken at its first. None means the default order.
   */
  public static <C extends Enum<C>> List<HeaderSort<C>> of(
      List<C> columns, List<Sort.Direction> directions) {
    if (columns == null || columns.isEmpty()) {
      return List.of();
    }
    List<HeaderSort<C>> sort = new ArrayList<>();
    EnumSet<C> seen = null;
    for (int i = 0; i < columns.size(); i++) {
      C column = columns.get(i);
      if (column == null) {
        continue;
      }
      if (seen == null) {
        seen = EnumSet.noneOf(column.getDeclaringClass());
      }
      if (!seen.add(column)) {
        continue;
      }
      Sort.Direction direction =
          directions != null && i < directions.size() && directions.get(i) != null
              ? directions.get(i)
              : Sort.Direction.ASC;
      sort.add(new HeaderSort<>(column, direction));
    }
    return List.copyOf(sort);
  }
}
