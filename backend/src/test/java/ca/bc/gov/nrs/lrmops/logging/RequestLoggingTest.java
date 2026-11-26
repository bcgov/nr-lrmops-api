package ca.bc.gov.nrs.lrmops.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ca.bc.gov.nrs.lrmops.exception.RoadSectionNotFoundException;
import ca.bc.gov.nrs.lrmops.service.v1.RoadService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** The request log: a line per request with its status and time, and the errors with a stack. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class RequestLoggingTest {

  @Autowired
  private MockMvc mvc;

  @MockitoBean
  private RoadService roads;

  private static org.springframework.test.web.servlet.request.RequestPostProcessor roadsRead() {
    return jwt().authorities(new SimpleGrantedAuthority("SCOPE_lrmops:roads:read"));
  }

  @Test
  void logsAnAnswerWithItsStatusAndTime(CapturedOutput output) throws Exception {
    when(roads.names(any(), anyInt())).thenReturn(List.of("BIG CREEK FSR"));

    mvc.perform(get("/api/v1/roads/names?search=big").with(roadsRead()))
        .andExpect(status().isOk());

    assertThat(output.getOut())
        .containsPattern(" INFO .*RequestLoggingFilter.*GET /api/v1/roads/names\\?search=big"
            + " → 200 in \\d+ ms");
  }

  @Test
  void logsANotFoundAsAnOrdinaryAnswer(CapturedOutput output) throws Exception {
    when(roads.find("R1", "A")).thenThrow(new RoadSectionNotFoundException("R1", "A"));

    mvc.perform(get("/api/v1/roads/R1/sections/A").with(roadsRead()))
        .andExpect(status().isNotFound());

    assertThat(output.getOut())
        .containsPattern(" INFO .*GET /api/v1/roads/R1/sections/A → 404 in \\d+ ms")
        .doesNotContain("ErrorLoggingResolver");
  }

  @Test
  void warnsOfARefusal(CapturedOutput output) throws Exception {
    mvc.perform(get("/api/v1/roads")).andExpect(status().isUnauthorized());

    assertThat(output.getOut()).containsPattern(" WARN .*GET /api/v1/roads → 401 in \\d+ ms");
  }

  @Test
  void logsAnUnexpectedErrorWithItsStack(CapturedOutput output) throws Exception {
    when(roads.names(any(), anyInt())).thenThrow(new QueryTimeoutException("ORA-01013"));

    // Not resolved to a response: it goes on to the server, which answers 500.
    assertThatThrownBy(() -> mvc.perform(get("/api/v1/roads/names?search=big")
        .with(roadsRead()))).hasRootCauseInstanceOf(QueryTimeoutException.class);

    String logged = output.getOut();
    assertThat(logged)
        .containsPattern("ERROR .*ErrorLoggingResolver.*GET /api/v1/roads/names\\?search=big"
            + " failed: .*ORA-01013")
        .contains("at ca.bc.gov.nrs.lrmops")
        .containsPattern("ERROR .*RequestLoggingFilter.*GET /api/v1/roads/names\\?search=big"
            + " → failed after \\d+ ms: .*ORA-01013");
    // The stack once, not twice.
    assertThat(java.util.regex.Pattern
        .compile("(?m)^org\\.springframework\\.dao\\.QueryTimeoutException: ORA-01013$")
        .matcher(logged).results().count()).isEqualTo(1);
  }

  @Test
  void leavesTheProbesOutOfTheLog(CapturedOutput output) throws Exception {
    mvc.perform(get("/actuator/health")).andExpect(status().isOk());

    assertThat(output.getOut()).doesNotContain("GET /actuator/health");
  }
}
