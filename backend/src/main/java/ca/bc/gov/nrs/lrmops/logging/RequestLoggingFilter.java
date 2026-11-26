package ca.bc.gov.nrs.lrmops.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * One log line per request: what was asked, the answer, and how long it took — so the API's own
 * log shows that it is being used, and how fast, rather than only its callers' logs.
 *
 * <pre>
 * GET /api/v1/roads/names?search=big → 200 in 1184 ms
 * </pre>
 *
 * <p><b>Level by outcome.</b> INFO for an answer, including a 404 — "no such road section" is the
 * ordinary answer while a user is still typing. WARN for any other refusal (400, 401, 403): the
 * caller sent something wrong or is not allowed. ERROR for a 5xx, and for an exception that
 * escapes the application, with its stack trace; {@link ErrorLoggingResolver} logs the stack of
 * one the application turned into a response.
 *
 * <p>The probes and the Prometheus scrape ({@code /actuator/**}) are at DEBUG only: OpenShift asks
 * every few seconds, and those lines would bury the rest.
 *
 * <p>First in the chain, ahead of Spring Security, so that requests it refuses (401, 403) are
 * logged too and the time is the whole request's.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

  private static final String ACTUATOR = "/actuator";

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain chain) throws ServletException, IOException {
    long started = System.nanoTime();
    try {
      chain.doFilter(request, response);
    } catch (IOException | ServletException | RuntimeException ex) {
      // With its stack, unless ErrorLoggingResolver has already logged it.
      if (request.getAttribute(ErrorLoggingResolver.LOGGED) != null) {
        log.error("{} → failed after {} ms: {}", describe(request), millisSince(started),
            ex.toString());
      } else {
        log.error("{} → failed after {} ms", describe(request), millisSince(started), ex);
      }
      throw ex;
    }
    log(request, response.getStatus(), millisSince(started));
  }

  private static void log(HttpServletRequest request, int status, long millis) {
    String line = "{} → {} in {} ms";
    Object[] args = {describe(request), status, millis};
    if (request.getRequestURI().startsWith(ACTUATOR)) {
      log.debug(line, args);
    } else if (status >= 500) {
      log.error(line, args);
    } else if (status >= 400 && status != HttpStatus.NOT_FOUND.value()) {
      log.warn(line, args);
    } else {
      log.info(line, args);
    }
  }

  /** "GET /api/v1/roads/names?search=big": the method, the path and the query string. */
  static String describe(HttpServletRequest request) {
    String query = request.getQueryString();
    return request.getMethod() + " " + request.getRequestURI() + (query == null ? "" : "?" + query);
  }

  private static long millisSince(long startedNanos) {
    return (System.nanoTime() - startedNanos) / 1_000_000;
  }
}
