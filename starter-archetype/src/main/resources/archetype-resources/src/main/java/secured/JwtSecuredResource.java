package ${package}.secured;

import org.eclipse.microprofile.jwt.JsonWebToken;
import ${eePackage}.annotation.security.RolesAllowed;
import ${eePackage}.inject.Inject;
import ${eePackage}.ws.rs.GET;
import ${eePackage}.ws.rs.Path;
import ${eePackage}.ws.rs.Produces;
import ${eePackage}.ws.rs.core.MediaType;
import ${eePackage}.ws.rs.core.Response;

/**
 * JWT-protected REST resource demonstrating MicroProfile JWT claim extraction.
 * Send requests with a valid Bearer token in the Authorization header:
 *   Authorization: Bearer &lt;token&gt;
 */
@Path("jwt-secured")
@RolesAllowed({"user", "admin"})
public class JwtSecuredResource {

    @Inject
    private JsonWebToken jwt;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response info() {
        return Response.ok(
                "Subject: " + jwt.getSubject()
                + " | Name: " + jwt.getName()
                + " | Issuer: " + jwt.getIssuer()
                + " | Groups: " + jwt.getGroups()
        ).build();
    }
}
