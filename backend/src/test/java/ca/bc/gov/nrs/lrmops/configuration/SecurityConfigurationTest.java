package ca.bc.gov.nrs.lrmops.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Reading ALLOWED_ORIGINS. */
class SecurityConfigurationTest {

  @Test
  void parsesCommaSeparatedOrigins() {
    assertThat(SecurityConfiguration.parseOrigins(" https://a.gov.bc.ca , ,https://b.gov.bc.ca"))
        .containsExactly("https://a.gov.bc.ca", "https://b.gov.bc.ca");
  }

  @Test
  void emptyOrMissingAllowsNone() {
    assertThat(SecurityConfiguration.parseOrigins("")).isEmpty();
    assertThat(SecurityConfiguration.parseOrigins(null)).isEmpty();
  }
}
