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
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

/** GET /api/v1/roads end to end: scope, parameters, and the JSON returned. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql("/tfm-road-data.sql")
class RoadApiControllerTest {

  @Autowired
  private MockMvc mvc;

  private static JwtRequestPostProcessor withScope(String scope) {
    return jwt().authorities(new SimpleGrantedAuthority("SCOPE_" + scope));
  }

  private static JwtRequestPostProcessor roadsRead() {
    return withScope("lrmops:roads:read");
  }

  @Test
  void needsAToken() throws Exception {
    mvc.perform(get("/api/v1/roads")).andExpect(status().isUnauthorized());
  }

  @Test
  void needsTheRoadsScope() throws Exception {
    mvc.perform(get("/api/v1/roads").with(jwt())).andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/roads").with(withScope("lrmops:cbr-sites:read")))
        .andExpect(status().isForbidden());
  }

  @Test
  void returnsAPageWithTheTotal() throws Exception {
    mvc.perform(get("/api/v1/roads").param("pageSize", "2").with(roadsRead()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.totalElements").value(5))
        .andExpect(jsonPath("$.totalPages").value(3))
        .andExpect(jsonPath("$.pageNumber").value(0))
        .andExpect(jsonPath("$.pageSize").value(2));
  }

  @Test
  void bindsTheCriteriaAndReturnsTheRoadFields() throws Exception {
    mvc.perform(get("/api/v1/roads").param("roadId", "R08317").with(roadsRead()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].roadId").value("R08317"))
        .andExpect(jsonPath("$.content[0].sectionId").value("I"))
        .andExpect(jsonPath("$.content[0].roadName").value("AM 4"))
        .andExpect(jsonPath("$.content[0].tenureType").value("RP"))
        .andExpect(jsonPath("$.content[0].retired").value(true))
        .andExpect(jsonPath("$.content[0].clientName").value("CANFOR"));
  }

  @Test
  void sortsByTheColumnAsked() throws Exception {
    mvc.perform(get("/api/v1/roads")
            .param("sortBy", "ROAD_ID")
            .param("sortDirection", "DESC")
            .with(roadsRead()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].roadId").value("S00100"));
  }

  @Test
  void rejectsAnUnknownSortColumn() throws Exception {
    mvc.perform(get("/api/v1/roads").param("sortBy", "NOPE").with(roadsRead()))
        .andExpect(status().isBadRequest());
  }
}
