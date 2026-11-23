package ca.bc.gov.nrs.lrmops.controller.v1;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

/** GET /api/v1/road-tenure-types end to end: scope, and the list returned. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql("/tfm-road-tenure-type-data.sql")
class RoadTenureTypeApiControllerTest {

  private static final String PATH = "/api/v1/road-tenure-types";

  @Autowired
  private MockMvc mvc;

  @Test
  void needsTheRoadsScope() throws Exception {
    mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
    mvc.perform(get(PATH).with(jwt())).andExpect(status().isForbidden());
  }

  @Test
  void listsTheTypesByCodeWithTheRetiredVariantsButNotGeometryOnly() throws Exception {
    mvc.perform(get(PATH).with(jwt().authorities(new SimpleGrantedAuthority(
            "SCOPE_lrmops:roads:read"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(4))
        .andExpect(jsonPath("$[0].code").value("FSR"))
        .andExpect(jsonPath("$[0].description").value("Forest Service Road"))
        .andExpect(jsonPath("$[1].code").value("OTH"))
        .andExpect(jsonPath("$[1].description").doesNotExist())
        .andExpect(jsonPath("$[2].code").value("RP"))
        .andExpect(jsonPath("$[2].description").value("Road Permit"))
        .andExpect(jsonPath("$[3].code").value("RP-RETIRE"))
        .andExpect(jsonPath("$[3].description").value("Retired Road Permit"));
  }
}
