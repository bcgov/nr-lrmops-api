package ca.bc.gov.nrs.lrmops;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** The context starts, the probes are anonymous, and everything else needs a token. */
@SpringBootTest
@AutoConfigureMockMvc
// Spring Boot tests switch metrics export off unless asked; the Prometheus scrape is under test.
@AutoConfigureObservability
@ActiveProfiles("test")
class LrmopsApiApplicationTest {

  @Autowired
  private MockMvc mvc;

  @Test
  void healthIsAnonymous() throws Exception {
    mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
    mvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
  }

  @Test
  void prometheusIsAnonymous() throws Exception {
    mvc.perform(get("/actuator/prometheus")).andExpect(status().isOk());
  }

  @Test
  void apiDocsAreAnonymous() throws Exception {
    mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
  }

  @Test
  void apiNeedsAToken() throws Exception {
    mvc.perform(get("/api/v1/roads")).andExpect(status().isUnauthorized());
  }

  /** A valid token gets past authentication; with no endpoint at that path yet, that is a 404. */
  @Test
  void aValidTokenIsAuthenticated() throws Exception {
    mvc.perform(get("/api/v1/roads").with(jwt())).andExpect(status().isNotFound());
  }

  @Test
  void otherActuatorEndpointsAreNotOpen() throws Exception {
    mvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
  }

  @Test
  void preflightFromAnAllowedOriginIsAnswered() throws Exception {
    mvc.perform(options("/api/v1/roads")
            .header("Origin", "https://allowed.example.gov.bc.ca")
            .header("Access-Control-Request-Method", "GET")
            .header("Access-Control-Request-Headers", "Authorization"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin",
            "https://allowed.example.gov.bc.ca"));
  }

  @Test
  void preflightFromAnotherOriginIsRefused() throws Exception {
    mvc.perform(options("/api/v1/roads")
            .header("Origin", "https://elsewhere.example.com")
            .header("Access-Control-Request-Method", "GET"))
        .andExpect(status().isForbidden());
  }

  @Test
  void writeMethodsAreNotAllowedCrossOrigin() throws Exception {
    mvc.perform(options("/api/v1/roads")
            .header("Origin", "https://allowed.example.gov.bc.ca")
            .header("Access-Control-Request-Method", "POST"))
        .andExpect(status().isForbidden());
  }
}
