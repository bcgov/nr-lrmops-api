package ca.bc.gov.nrs.lrmops.security;

/**
 * Per-operation OAuth2 scope expressions for {@code @PreAuthorize}.
 *
 * <p>Spring's default JWT converter maps each space-delimited entry of the token's {@code scope}
 * claim to a {@code SCOPE_<name>} authority, so the expressions below match client scopes that
 * must be assigned to the calling client in Keycloak. The scopes themselves are created in each
 * environment's realm at deploy time by {@code .github/scripts/ensure-keycloak-scopes.sh}; keep
 * its {@code SCOPES} list in step with this class.
 *
 * <p>Scopes follow a {@code <service>:<resource>:<action>} convention, namespaced to this service
 * to avoid collisions in a shared realm:
 * <ul>
 *   <li>{@code lrmops:roads:read} — road search, and a road section by road and section id.</li>
 *   <li>{@code lrmops:road-locations:read} — what LRMOPS would assign at a coordinate.</li>
 *   <li>{@code lrmops:cbr-sites:read} — LRMOPS's details for a CBR crossing site.</li>
 * </ul>
 */
public final class ApiScopes {

  private ApiScopes() {
  }

  public static final String ROADS_READ = "hasAuthority('SCOPE_lrmops:roads:read')";
  public static final String ROAD_LOCATIONS_READ =
      "hasAuthority('SCOPE_lrmops:road-locations:read')";
  public static final String CBR_SITES_READ = "hasAuthority('SCOPE_lrmops:cbr-sites:read')";
}
