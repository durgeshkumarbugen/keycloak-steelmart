package cc.coopersoft.keycloak.phone.providers.rest;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

public class RecaptchaResourceProvider implements RealmResourceProvider {

    private final KeycloakSession session;

    RecaptchaResourceProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource() {
        return new RecaptchaResource(session);
    }

    @Override
    public void close() {
    }
}
