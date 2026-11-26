package cc.coopersoft.keycloak.phone.providers.rest;

import cc.coopersoft.keycloak.phone.Utils;
import cc.coopersoft.keycloak.phone.providers.constants.TokenCodeType;
import cc.coopersoft.keycloak.phone.providers.exception.PhoneNumberInvalidException;
import cc.coopersoft.keycloak.phone.providers.jpa.TokenCode;
import cc.coopersoft.keycloak.phone.providers.representations.TokenCodeRepresentation;
import cc.coopersoft.keycloak.phone.providers.spi.PhoneVerificationCodeProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TemporalType;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.authentication.actiontoken.execactions.ExecuteActionsActionToken;
import org.keycloak.common.util.Time;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.services.validation.Validation;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static jakarta.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

public class SmsResource {

    private static final Logger logger = Logger.getLogger(SmsResource.class);
    private static final int MAX_EMAIL_OTP_ATTEMPTS = 3;
    private static final String EMAIL_OTP_ATTEMPTS_PREFIX = "emailOtpAttempts:";
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
    @Path("phone-exists")
    @Produces(APPLICATION_JSON)
    public Response phoneExists(@QueryParam("phoneNumber") String phoneNumber) {
        if (Validation.isBlank(phoneNumber)) {
            throw new BadRequestException("Must supply a phone number");
        }

        String canonicalPhone = canonicalizePhone(phoneNumber);
        boolean exists = Utils.findUserByPhone(session, session.getContext().getRealm(), canonicalPhone).isPresent();
        String response = String.format("{\"exists\":%s}", exists);
        return Response.ok(response, APPLICATION_JSON_TYPE).build();
    }

    @GET
    @NoCache
    @Path("user-exists")
    @Produces(APPLICATION_JSON)
    public Response userExists(@QueryParam("email") String email) {

        if (Validation.isBlank(email)) {
            throw new BadRequestException("Must supply an email");
        }

        RealmModel realm = session.getContext().getRealm();
        String normalizedEmail = normalizeEmail(email);
        UserModel user = findUserByEmail(realm, normalizedEmail);
        boolean exists = user != null;
        boolean googleLinked = exists && hasGoogleFederatedIdentity(realm, user);

        String response = String.format("{\"exists\":%s,\"googleLinked\":%s}", exists, googleLinked);
        return Response.ok(response, APPLICATION_JSON_TYPE).build();
    }

    @POST
    @NoCache
    @Path("send-registration-email-otp")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(APPLICATION_JSON)
    public Response sendRegistrationEmailOtp(@FormParam("email") String email) {

        if (Validation.isBlank(email)) {
            throw new BadRequestException("Must supply an email");
        }

        RealmModel realm = session.getContext().getRealm();
        if (!realm.isRegistrationAllowed()) {
            throw new ForbiddenException("Registration is disabled");
        }

        String normalizedEmail = normalizeEmail(email);
        if (findUserByEmail(realm, normalizedEmail) != null) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("{\"error\":\"email_exists\"}")
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        }

        String tokenTarget = emailTokenTarget(normalizedEmail);
        TokenCode tokenCode = getLatestOngoingToken(tokenTarget, TokenCodeType.REGISTRATION);
        if (tokenCode == null) {
            tokenCode = createEmailOtpToken(tokenTarget);
        }

        int expiresInSeconds = getRemainingSeconds(tokenCode);
        if (expiresInSeconds <= 0) {
            tokenCode = createEmailOtpToken(tokenTarget);
            expiresInSeconds = getRemainingSeconds(tokenCode);
        }
        if (!sendRegistrationOtpEmail(realm, normalizedEmail, tokenCode.getCode(), expiresInSeconds)) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"email_send_failed\"}")
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        }

        return Response.ok(String.format("{\"sent\":true,\"expires_in\":%d}", expiresInSeconds), APPLICATION_JSON_TYPE).build();
    }

    @GET
    @NoCache
    @Path("email-verified")
    @Produces(APPLICATION_JSON)
    public Response emailVerified(@QueryParam("email") String email) {
        if (Validation.isBlank(email)) {
            throw new BadRequestException("Must supply an email");
        }

        UserModel user = findUserByEmail(session.getContext().getRealm(), normalizeEmail(email));
        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"user_not_found\"}")
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        }

        String response = String.format("{\"verified\":%s}", user.isEmailVerified());
        return Response.ok(response, APPLICATION_JSON_TYPE).build();
    }

    @POST
    @NoCache
    @Path("send-verify-email")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(APPLICATION_JSON)
    public Response sendVerifyEmail(@FormParam("email") String email) {
        if (Validation.isBlank(email)) {
            throw new BadRequestException("Must supply an email");
        }

        RealmModel realm = session.getContext().getRealm();
        UserModel user = findUserByEmail(realm, normalizeEmail(email));
        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"user_not_found\"}")
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        }

        if (!user.isEnabled()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"user_disabled\"}")
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        }

        if (Validation.isBlank(user.getEmail())) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"email_missing\"}")
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        }

        if (user.isEmailVerified()) {
            return Response.ok("{\"sent\":false,\"alreadyVerified\":true}", APPLICATION_JSON_TYPE).build();
        }

        user.addRequiredAction(UserModel.RequiredAction.VERIFY_EMAIL.name());
        boolean sent = sendVerifyEmailAction(realm, user);
        if (!sent) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"email_send_failed\"}")
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        }

        return Response.ok("{\"sent\":true}", APPLICATION_JSON_TYPE).build();
    }

    @POST
    @NoCache
    @Path("register-user")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(APPLICATION_JSON)
    public Response registerUser(@FormParam("email") String email,
                                 @FormParam("code") String code,
                                 @FormParam("firstName") String firstName,
                                 @FormParam("lastName") String lastName,
                                 @FormParam("password") String password) {

        RealmModel realm = session.getContext().getRealm();
        if (!realm.isRegistrationAllowed()) {
            throw new ForbiddenException("Registration is disabled");
        }
        if (Validation.isBlank(email)
                || Validation.isBlank(code)
                || Validation.isBlank(firstName)
                || Validation.isBlank(lastName)
                || Validation.isBlank(password)) {
            throw new BadRequestException("Email, code, first name, last name and password are required");
        }

        String normalizedEmail = normalizeEmail(email);
        if (findUserByEmail(realm, normalizedEmail) != null) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("{\"error\":\"email_exists\"}")
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        }

        String tokenTarget = emailTokenTarget(normalizedEmail);
        TokenCode tokenCode = getLatestOngoingToken(tokenTarget, TokenCodeType.REGISTRATION);
        if (tokenCode == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"code_expired\"}")
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        }

        if (!tokenCode.getCode().equals(code.trim())) {
            int attempts = incrementEmailOtpAttempts(tokenCode);
            if (attempts >= MAX_EMAIL_OTP_ATTEMPTS) {
                return Response.status(Response.Status.TOO_MANY_REQUESTS)
                        .entity(String.format("{\"error\":\"otp_attempts_exceeded\",\"maxAttempts\":%d}", MAX_EMAIL_OTP_ATTEMPTS))
                        .type(APPLICATION_JSON_TYPE)
                        .build();
            }
            int remainingAttempts = MAX_EMAIL_OTP_ATTEMPTS - attempts;
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(String.format("{\"error\":\"invalid_code\",\"remainingAttempts\":%d}", remainingAttempts))
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        }

        UserModel createdUser = null;
        try {
            createdUser = session.users().addUser(realm, normalizedEmail);
            createdUser.setEnabled(true);
            createdUser.setUsername(normalizedEmail);
            createdUser.setEmail(normalizedEmail);
            createdUser.setEmailVerified(true);
            createdUser.setFirstName(firstName.trim());
            createdUser.setLastName(lastName.trim());
            createdUser.removeRequiredAction("UPDATE_PROFILE");
            createdUser.removeRequiredAction("VERIFY_EMAIL");

            boolean credentialUpdated = createdUser.credentialManager()
                    .updateCredential(UserCredentialModel.password(password, false));
            if (!credentialUpdated) {
                session.users().removeUser(realm, createdUser);
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\":\"invalid_password\"}")
                        .type(APPLICATION_JSON_TYPE)
                        .build();
            }

            markEmailOtpAsValidated(tokenCode, createdUser);
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

    @POST
    @NoCache
    @Path("register-phone-user")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(APPLICATION_JSON)
    public Response registerPhoneUser(@FormParam("phoneNumber") String phoneNumber,
                                      @FormParam("code") String code,
                                      @FormParam("firstName") String firstName,
                                      @FormParam("lastName") String lastName) {

        RealmModel realm = session.getContext().getRealm();
        if (!realm.isRegistrationAllowed()) {
            throw new ForbiddenException("Registration is disabled");
        }

        if (Validation.isBlank(phoneNumber) || Validation.isBlank(code)
                || Validation.isBlank(firstName) || Validation.isBlank(lastName)) {
            throw new BadRequestException("Phone number, code, first name and last name are required");
        }

        String canonicalPhone = canonicalizePhone(phoneNumber);
        if (Utils.findUserByPhone(session, realm, canonicalPhone).isPresent()) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("{\"error\":\"phone_exists\"}")
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        }

        PhoneVerificationCodeProvider codeProvider = session.getProvider(PhoneVerificationCodeProvider.class);
        if (codeProvider.ongoingProcess(canonicalPhone, TokenCodeType.AUTH) == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"code_expired\"}")
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        }

        String informedCode = code.trim();
        UserModel createdUser = null;
        try {
            String username = buildPhoneUsername(canonicalPhone);
            String placeholderEmail = buildPhonePlaceholderEmail(canonicalPhone);
            createdUser = session.users().addUser(realm, username);
            createdUser.setEnabled(true);
            createdUser.setUsername(username);
            createdUser.setEmail(placeholderEmail);
            createdUser.setEmailVerified(true);
            createdUser.setFirstName(firstName.trim());
            createdUser.setLastName(lastName.trim());
            createdUser.removeRequiredAction("UPDATE_PROFILE");
            createdUser.removeRequiredAction("VERIFY_EMAIL");

            codeProvider.validateCode(createdUser, canonicalPhone, informedCode, TokenCodeType.AUTH);

            return Response.status(Response.Status.CREATED)
                    .entity("{\"created\":true}")
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        } catch (ForbiddenException e) {
            if (createdUser != null) {
                session.users().removeUser(realm, createdUser);
            }
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("{\"error\":\"invalid_code\"}")
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        } catch (BadRequestException e) {
            if (createdUser != null) {
                session.users().removeUser(realm, createdUser);
            }
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"code_expired\"}")
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        } catch (ModelDuplicateException e) {
            if (createdUser != null) {
                session.users().removeUser(realm, createdUser);
            }
            return Response.status(Response.Status.CONFLICT)
                    .entity("{\"error\":\"phone_exists\"}")
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

    private EntityManager getEntityManager() {
        return session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String emailTokenTarget(String normalizedEmail) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalizedEmail.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder("em");
            for (int i = 0; i < 9; i++) {
                sb.append(String.format("%02x", hash[i]));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }

    private TokenCode getLatestOngoingToken(String target, TokenCodeType tokenCodeType) {
        List<TokenCode> tokenCodes = getEntityManager()
                .createQuery("FROM TokenCode t WHERE t.realmId = :realmId " +
                        "AND t.phoneNumber = :target " +
                        "AND t.expiresAt >= :now " +
                        "AND t.type = :type ORDER BY t.createdAt DESC", TokenCode.class)
                .setParameter("realmId", session.getContext().getRealm().getId())
                .setParameter("target", target)
                .setParameter("now", new Date(), TemporalType.TIMESTAMP)
                .setParameter("type", tokenCodeType.name())
                .setMaxResults(1)
                .getResultList();

        if (tokenCodes.isEmpty()) {
            return null;
        }
        return tokenCodes.get(0);
    }

    private TokenCode createEmailOtpToken(String tokenTarget) {
        int tokenExpiresIn = Math.max(1, Utils.getOtpExpires(session));
        TokenCodeRepresentation tokenCode = TokenCodeRepresentation.forPhoneNumber(tokenTarget);
        session.getProvider(PhoneVerificationCodeProvider.class)
                .persistCode(tokenCode, TokenCodeType.REGISTRATION, tokenExpiresIn);

        TokenCode entity = getEntityManager().find(TokenCode.class, tokenCode.getId());
        if (entity == null) {
            throw new IllegalStateException("Unable to persist email registration OTP.");
        }
        return entity;
    }

    private int getRemainingSeconds(TokenCode tokenCode) {
        long delta = (tokenCode.getExpiresAt().getTime() - System.currentTimeMillis()) / 1000L;
        return (int) Math.max(0L, delta);
    }

    private int getEmailOtpAttempts(TokenCode tokenCode) {
        String byWhom = tokenCode.getByWhom();
        if (Validation.isBlank(byWhom) || !byWhom.startsWith(EMAIL_OTP_ATTEMPTS_PREFIX)) {
            return 0;
        }

        String value = byWhom.substring(EMAIL_OTP_ATTEMPTS_PREFIX.length());
        try {
            return Math.max(0, Integer.parseInt(value));
        } catch (NumberFormatException e) {
            logger.warnf("Unable to parse email OTP attempts '%s' for token %s", byWhom, tokenCode.getId());
            return 0;
        }
    }

    private int incrementEmailOtpAttempts(TokenCode tokenCode) {
        int attempts = getEmailOtpAttempts(tokenCode) + 1;
        tokenCode.setByWhom(EMAIL_OTP_ATTEMPTS_PREFIX + attempts);
        if (attempts >= MAX_EMAIL_OTP_ATTEMPTS) {
            tokenCode.setExpiresAt(new Date());
        }
        getEntityManager().merge(tokenCode);
        return attempts;
    }

    private void markEmailOtpAsValidated(TokenCode tokenCode, UserModel user) {
        tokenCode.setConfirmed(true);
        tokenCode.setByWhom(user.getId());
        getEntityManager().merge(tokenCode);
    }

    private boolean sendRegistrationOtpEmail(RealmModel realm, String email, String code, int expiresInSeconds) {
        EmailSenderProvider emailSenderProvider = session.getProvider(EmailSenderProvider.class);
        if (emailSenderProvider == null) {
            logger.error("Unable to send registration OTP email: EmailSenderProvider not available");
            return false;
        }
        if (realm.getSmtpConfig() == null || realm.getSmtpConfig().isEmpty()) {
            logger.error("Unable to send registration OTP email: SMTP configuration is missing");
            return false;
        }

        String realmName = Validation.isBlank(realm.getDisplayName()) ? realm.getName() : realm.getDisplayName();
        long expiresInMinutes = Math.max(1L, (expiresInSeconds + 59L) / 60L);
        String subject = String.format("%s - Email OTP", realmName);
        String textBody = String.format(
                "Your OTP for account registration is %s. This code expires in %d minute(s).",
                code,
                expiresInMinutes
        );
        String htmlBody = String.format(
                "<p>Your OTP for account registration is <strong>%s</strong>.</p><p>This code expires in <strong>%d minute(s)</strong>.</p>",
                code,
                expiresInMinutes
        );

        try {
            emailSenderProvider.send(realm.getSmtpConfig(), email, subject, textBody, htmlBody);
            return true;
        } catch (EmailException e) {
            logger.errorf(e, "Failed to send registration OTP email to %s", email);
            return false;
        }
    }

    private String canonicalizePhone(String phoneNumber) {
        try {
            return Utils.canonicalizePhoneNumber(session, phoneNumber.trim());
        } catch (PhoneNumberInvalidException e) {
            throw new BadRequestException("Invalid phone number. Please enter a 10-digit valid phone number.");
        }
    }

    private String buildPhoneUsername(String canonicalPhone) {
        RealmModel realm = session.getContext().getRealm();
        String digitsOnly = canonicalPhone.replaceAll("[^0-9]", "");
        String baseUsername = Validation.isBlank(digitsOnly) ? "phone_user" : "phone_" + digitsOnly;
        String candidate = baseUsername;
        int suffix = 1;

        while (session.users().getUserByUsername(realm, candidate) != null) {
            candidate = baseUsername + "_" + suffix;
            suffix++;
        }

        return candidate;
    }

    private String buildPhonePlaceholderEmail(String canonicalPhone) {
        String digitsOnly = canonicalPhone.replaceAll("[^0-9]", "");
        String localPart = Validation.isBlank(digitsOnly) ? "phoneuser" : "phone" + digitsOnly;
        return localPart + "@phone.local";
    }

    private UserModel findUserByEmail(RealmModel realm, String email) {
        try {
            return KeycloakModelUtils.findUserByNameOrEmail(session, realm, email);
        } catch (ModelDuplicateException e) {
            return session.users().searchForUserByUserAttributeStream(realm, UserModel.EMAIL, email)
                    .findFirst()
                    .orElse(null);
        }
    }

    private boolean hasGoogleFederatedIdentity(RealmModel realm, UserModel user) {
        return session.users()
                .getFederatedIdentitiesStream(realm, user)
                .map(identity -> identity.getIdentityProvider() == null ? "" : identity.getIdentityProvider().toLowerCase(Locale.ROOT))
                .anyMatch(idpAlias -> idpAlias.contains("google"));
    }

    private boolean sendVerifyEmailAction(RealmModel realm, UserModel user) {
        if (Validation.isBlank(user.getEmail())) {
            logger.warnf("Unable to send verification email for user %s: missing email", user.getId());
            return false;
        }

        List<String> actions = Collections.singletonList(UserModel.RequiredAction.VERIFY_EMAIL.name());
        String clientId = Constants.ACCOUNT_MANAGEMENT_CLIENT_ID;
        String redirectUri = null;
        ClientModel client = realm.getClientByClientId(clientId);
        if ((client == null || !client.isEnabled())) {
            AuthenticationSessionModel authSession = session.getContext().getAuthenticationSession();
            if (authSession != null && authSession.getClient() != null && authSession.getClient().isEnabled()) {
                clientId = authSession.getClient().getClientId();
                client = authSession.getClient();
            }
        }

        if (client == null || !client.isEnabled()) {
            logger.errorf("Unable to send verification email for user %s: no enabled client available", user.getId());
            return false;
        }

        int lifespan = realm.getActionTokenGeneratedByAdminLifespan();
        int expiration = Time.currentTime() + lifespan;
        ExecuteActionsActionToken token = new ExecuteActionsActionToken(
                user.getId(),
                user.getEmail(),
                expiration,
                actions,
                redirectUri,
                clientId
        );

        try {
            UriBuilder builder = LoginActionsService.actionTokenProcessor(session.getContext().getUri());
            builder.queryParam("key", token.serialize(session, realm, session.getContext().getUri()));
            String link = builder.build(realm.getName()).toString();

            session.getProvider(EmailTemplateProvider.class)
                    .setAttribute(Constants.TEMPLATE_ATTR_REQUIRED_ACTIONS, actions)
                    .setRealm(realm)
                    .setUser(user)
                    .sendExecuteActions(link, TimeUnit.SECONDS.toMinutes(lifespan));

            return true;
        } catch (EmailException e) {
            logger.errorf(e, "Failed to send verification email for user %s", user.getId());
            return false;
        }
    }

}
