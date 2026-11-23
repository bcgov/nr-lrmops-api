package ca.bc.gov.nrs.lrmops.struct.v1;

import java.util.List;

/**
 * One page of a search, with the true total across every page.
 *
 * @param content the rows on this page
 * @param totalElements how many rows matched in all
 * @param totalPages how many pages of {@code pageSize} that makes
 * @param pageNumber this page, zero-based
 * @param pageSize the page size used
 */
public record PagedResponse<T>(
    List<T> content,
    long totalElements,
    int totalPages,
    int pageNumber,
    int pageSize
) {
}
