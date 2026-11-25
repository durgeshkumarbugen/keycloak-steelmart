package cc.coopersoft.keycloak.phone.providers.rest;

import cc.coopersoft.keycloak.phone.providers.constants.TokenCodeType;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.services.validation.Validation;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import static jakarta.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

public class SmsResource {

    private final KeycloakSession session;

    public SmsResource(KeycloakSession session) {
        this.session = session;
    }

    @Path("verification-code")
    public VerificationCodeResource getVerificationCodeResource() {
        return new VerificationCodeResource(session);
    }

    @Path("authentication-code")
    public TokenCodeResource getAuthenticationCodeResource() {
        return new TokenCodeResource(session, TokenCodeType.AUTH);
    }

    @Path("registration-code")
    public TokenCodeResource getRegistrationCodeResource() {
        return new TokenCodeResource(session, TokenCodeType.REGISTRATION);
    }

    @Path("reset-code")
    public TokenCodeResource getResetCodeResource() {
        return new TokenCodeResource(session, TokenCodeType.RESET);
    }

    @Path("otp-code")
    public TokenCodeResource getOTPCodeResource() {
        return new TokenCodeResource(session, TokenCodeType.OTP);
    }

    @GET
    @NoCache
    @Path("user-exists")
    @Produces(APPLICATION_JSON)
    public Response userExists(@QueryParam("email") String email) {

        if (Validation.isBlank(email)) {
            throw new BadRequestException("Must supply an email");
        }

        boolean exists;
        try {
            exists = KeycloakModelUtils.findUserByNameOrEmail(session, session.getContext().getRealm(), email.trim()) != null;
        } catch (ModelDuplicateException e) {
            exists = true;
        }

        String response = String.format("{\"exists\":%s}", exists);
        return Response.ok(response, APPLICATION_JSON_TYPE).build();
    }

    @POST
    @NoCache
    @Path("register-user")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(APPLICATION_JSON)
    public Response registerUser(@FormParam("email") String email,
                                 @FormParam("firstName") String firstName,
                                 @FormParam("lastName") String lastName,
                                 @FormParam("password") String password) {

        RealmModel realm = session.getContext().getRealm();
        if (!realm.isRegistrationAllowed()) {
            throw new ForbiddenException("Registration is disabled");
        }
        if (Validation.isBlank(email) || Validation.isBlank(password)) {
            throw new BadRequestException("Email and password are required");
        }

        String normalizedEmail = email.trim();
        boolean exists;
        try {
            exists = KeycloakModelUtils.findUserByNameOrEmail(session, realm, normalizedEmail) != null;
        } catch (ModelDuplicateException e) {
            exists = true;
        }
        if (exists) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("{\"error\":\"email_exists\"}")
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        }

        UserModel createdUser = null;
        try {
            createdUser = session.users().addUser(realm, normalizedEmail);
            createdUser.setEnabled(true);
            createdUser.setUsername(normalizedEmail);
            createdUser.setEmail(normalizedEmail);

            if (!Validation.isBlank(firstName)) {
                createdUser.setFirstName(firstName.trim());
            }
            if (!Validation.isBlank(lastName)) {
                createdUser.setLastName(lastName.trim());
            }

            boolean credentialUpdated = createdUser.credentialManager()
                    .updateCredential(org.keycloak.models.UserCredentialModel.password(password, false));
            if (!credentialUpdated) {
                session.users().removeUser(realm, createdUser);
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\":\"invalid_password\"}")
                        .type(APPLICATION_JSON_TYPE)
                        .build();
            }

            return Response.status(Response.Status.CREATED)
                    .entity("{\"created\":true}")
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        } catch (ModelDuplicateException e) {
            if (createdUser != null) {
                session.users().removeUser(realm, createdUser);
            }
            return Response.status(Response.Status.CONFLICT)
                    .entity("{\"error\":\"email_exists\"}")
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        } catch (ModelException e) {
            if (createdUser != null) {
                session.users().removeUser(realm, createdUser);
            }
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"invalid_user_data\"}")
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        }
    }

}
