package ca.bc.gov.nrs.lrmops.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.ErrorResponse;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

/**
 * Logs, with its stack trace, an exception a request ended in that was not an expected answer — a
 * database error, a query timeout, a bug — before Spring turns it into the response it would have
 * anyway. Resolves nothing itself.
 *
 * <p>Expected outcomes are left to {@link RequestLoggingFilter}'s one line: a 4xx the API chose
 * (no such road section, a bad parameter — anything that is an {@link ErrorResponse} below 500),
 * and a refusal by Spring Security (not allowed, not authenticated).
 */
@Component
public class ErrorLoggingResolver implements HandlerExceptionResolver, Ordered {

  private static final Logger log = LoggerFactory.getLogger(ErrorLoggingResolver.class);

  /** Set on a request whose exception this has logged with its stack. */
  static final String LOGGED = ErrorLoggingResolver.class.getName() + ".logged";

  @Override
  public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response,
      Object handler, Exception ex) {
    if (!expected(ex)) {
      log.error("{} failed: {}", RequestLoggingFilter.describe(request), ex.toString(), ex);
      // Its stack is in the log now; the request line need not repeat it.
      request.setAttribute(LOGGED, Boolean.TRUE);
    }
    return null;
  }

  private static boolean expected(Exception ex) {
    if (ex instanceof AccessDeniedException || ex instanceof AuthenticationException) {
      return true;
    }
    return ex instanceof ErrorResponse error && error.getStatusCode().value() < 500;
  }

  /** First, so it sees every exception before one of the resolvers that answer it. */
  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }
}
