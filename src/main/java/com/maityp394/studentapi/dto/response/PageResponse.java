package com.maityp394.studentapi.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Standard pagination metadata envelope wrapping paginated collection responses.
 *
 * @param <T> the type of elements in the page
 * @param content the slice of items for the current page
 * @param pageNumber the current 0-based page index
 * @param pageSize the configured page size
 * @param totalElements the total count of elements matching query
 * @param totalPages the total count of pages
 * @param isFirst whether this is the first page
 * @param isLast whether this is the final page
 * @param hasNext whether more pages are available after this one
 */
public record PageResponse<T>(
    List<T> content,
    @Schema(example = "0") int pageNumber,
    @Schema(example = "5") int pageSize,
    @Schema(example = "25") long totalElements,
    @Schema(example = "5") int totalPages,
    @Schema(example = "true") boolean isFirst,
    @Schema(example = "false") boolean isLast,
    @Schema(example = "true") boolean hasNext) {

  /**
   * Factory method constructing a {@link PageResponse} from a Spring Data {@link Page}.
   *
   * @param page the Spring Data page instance
   * @param <T> the type of elements
   * @return the populated PageResponse
   */
  public static <T> PageResponse<T> from(Page<T> page) {
    return new PageResponse<>(
        page.getContent(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages(),
        page.isFirst(),
        page.isLast(),
        page.hasNext());
  }
}
