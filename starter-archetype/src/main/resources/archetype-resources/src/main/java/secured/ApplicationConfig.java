package ${package}.secured;

import ${eePackage}.annotation.security.DeclareRoles;
import ${eePackage}.enterprise.context.ApplicationScoped;<% if (formAuthDB || basicAuthDB) { %>
import ${eePackage}.inject.Named;<% } %><% if (formAuthDB || formAuthLDAP) { %>
import ${eePackage}.security.enterprise.authentication.mechanism.http.FormAuthenticationMechanismDefinition;
import ${eePackage}.security.enterprise.authentication.mechanism.http.LoginToContinue;<% } %><% if (basicAuthDB || basicAuthLDAP) { %>
import ${eePackage}.security.enterprise.authentication.mechanism.http.BasicAuthenticationMechanismDefinition;<% } %><% if (formAuthDB || basicAuthDB) { %>
import ${eePackage}.security.enterprise.identitystore.DatabaseIdentityStoreDefinition;
import ${eePackage}.security.enterprise.identitystore.Pbkdf2PasswordHash;
import java.util.HashMap;
import java.util.Map;<% } %><% if (formAuthLDAP || basicAuthLDAP) { %>
import ${eePackage}.security.enterprise.identitystore.LdapIdentityStoreDefinition;<% } %>

<% if (formAuthDB || basicAuthDB) { %>
@DatabaseIdentityStoreDefinition(
    callerQuery = "#{'select password from caller where name = ?'}",
    groupsQuery = "select group_name from caller_groups where caller_name = ?",
    hashAlgorithm = Pbkdf2PasswordHash.class,
    priorityExpression = "#{'${100}'}",
    hashAlgorithmParameters = {
        "${'${applicationConfig.hashAlgorithmParameters}'}"
    }
)<% } %><% if (formAuthLDAP || basicAuthLDAP) { %>
@LdapIdentityStoreDefinition(
    url = "ldap://localhost:33389/",
    callerBaseDn = "ou=caller,dc=jsr375,dc=net",
    groupSearchBase = "ou=group,dc=jsr375,dc=net"
)<% } %><% if (formAuthDB || formAuthLDAP) { %>
@FormAuthenticationMechanismDefinition(
    loginToContinue = @LoginToContinue(
        loginPage="/login.xhtml",
        errorPage="/login_error.xhtml"
    )
)<% } %><% if (basicAuthDB || basicAuthLDAP) { %>
@BasicAuthenticationMechanismDefinition(realmName = "payara-realm")<% } %>
@DeclareRoles({ "user", "admin" })
@ApplicationScoped<% if (formAuthDB || basicAuthDB) { %>
@Named<% } %>
public class ApplicationConfig {
<% if (formAuthDB || basicAuthDB) { %>
    public String[] getHashAlgorithmParameters() {
        return getHashAlgorithmParameterMap().entrySet()
                .stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .toArray(String[]::new);
    }

    public Map<String, String> getHashAlgorithmParameterMap() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("Pbkdf2PasswordHash.Iterations", "3072");
        parameters.put("Pbkdf2PasswordHash.Algorithm", "PBKDF2WithHmacSHA512");
        parameters.put("Pbkdf2PasswordHash.SaltSizeBytes", "64");
        return parameters;
    }
<% } %>
}
