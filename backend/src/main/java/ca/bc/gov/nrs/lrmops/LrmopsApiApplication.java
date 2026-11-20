package ca.bc.gov.nrs.lrmops;

import ca.bc.gov.nrs.lrmops.configuration.OracleTlsCompatibility;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * LRM Ops API.
 *
 * <p>A read-only OAuth 2.0 resource server over the TFM Oracle schema (LRMOPS), for the CBR team:
 * road search and lookup, road details at a location, and LRMOPS's details for a CBR site. Callers
 * are other services, authenticating with BC Gov SSO client-credentials tokens. It connects as
 * {@code PROXY_FSA_LRM_READ_WRITE_USER}, which holds SELECT grants only; see {@code scripts/db/}.
 */
@SpringBootApplication
public class LrmopsApiApplication {

  public static void main(String[] args) {
    OracleTlsCompatibility.allowRsaKeyExchange();
    SpringApplication.run(LrmopsApiApplication.class, args);
  }
}
