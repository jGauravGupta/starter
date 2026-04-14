package ${package}.${restSubpackage};

import ${eePackage}.ws.rs.ApplicationPath;
import ${eePackage}.ws.rs.core.Application;<% if (jwtAuth) { %>
import org.eclipse.microprofile.auth.LoginConfig;<% } %>

/**
 * Configures RESTful Web Services for the application.
 */<% if (jwtAuth) { %>
@LoginConfig(authMethod = "MP-JWT", realmName = "jwt-realm")<% } %>
@ApplicationPath("resources")
public class RestConfiguration extends Application {
    
}
