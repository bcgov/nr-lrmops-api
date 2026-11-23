package ca.bc.gov.nrs.lrmops.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * No road section in LRMOPS for the road and section asked for: a 404.
 *
 * <p><b>Ordinary, not exceptional.</b> A form that looks the pair up as the user types asks
 * mostly about pairs that are half-typed. Callers should treat it as "no such road" rather than as
 * a failure.
 */
public class RoadSectionNotFoundException extends ResponseStatusException {

  public RoadSectionNotFoundException(String roadId, String sectionId) {
    super(HttpStatus.NOT_FOUND, "No road section " + roadId + "-" + sectionId + " was found.");
  }
}
