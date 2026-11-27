package cc.coopersoft.keycloak.phone.providers.rest;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.validation.Validation;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

public class RecaptchaResource {

    private static final Logger logger = Logger.getLogger(RecaptchaResource.class);
    private static final String[] SITE_KEY_ENV_KEYS = {
        "RECAPTCHA_SITE_KEY",
        "GOOGLE_RECAPTCHA_SITE_KEY",
        "CAPTCHA_SITE_KEY"
    };

    @SuppressWarnings("unused")
    private final KeycloakSession session;

    public RecaptchaResource(KeycloakSession session) {
        this.session = session;
    }

    @GET
    @NoCache
    @Path("key")
    @Produces(APPLICATION_JSON)
    public Response getKey() {
        String siteKey = readSiteKeyFromEnvironment();
        if (Validation.isBlank(siteKey)) {
            logger.warn("reCAPTCHA site key is not configured. Expected one of env vars: RECAPTCHA_SITE_KEY, GOOGLE_RECAPTCHA_SITE_KEY, CAPTCHA_SITE_KEY");
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"recaptcha_site_key_not_configured\"}")
                .type(APPLICATION_JSON_TYPE)
                .build();
        }

        String escaped = jsonEscape(siteKey.trim());
        String response = String.format(
            "{\"capacha_key\":\"%s\",\"captcha_key\":\"%s\",\"site_key\":\"%s\"}",
            escaped,
            escaped,
            escaped
        );
        return Response.ok(response, APPLICATION_JSON_TYPE).build();
    }

    private String readSiteKeyFromEnvironment() {
        for (String envKey : SITE_KEY_ENV_KEYS) {
            String value = System.getenv(envKey);
            if (!Validation.isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
