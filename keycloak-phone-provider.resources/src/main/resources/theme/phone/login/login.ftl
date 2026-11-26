<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('code','phoneNumber','username','password') displayInfo=realm.password && realm.registrationAllowed && !registrationDisabled??; section>
<#if section = "header">
<#elseif section = "form">
<script src="https://cdn.jsdelivr.net/npm/vue/dist/vue.js"></script>
<script src="https://cdn.jsdelivr.net/npm/axios/dist/axios.min.js"></script>
<link href="https://fonts.googleapis.com/icon?family=Material+Icons" rel="stylesheet">

<style>
  @import url('https://fonts.googleapis.com/css2?family=Barlow:wght@400;500;600;700&display=swap');

  [v-cloak] { display: none; }

  body {
    margin: 0;
    background: #efefef;
    font-family: 'Barlow', sans-serif;
  }

  .auth-shell {
    width: 100%;
    min-height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #f3f3f3;
    padding: 32px 0;
    box-sizing: border-box;
  }

  .auth-card {
    width: 100%;
    max-width: 560px;
    background: #f4f4f4;
    border: 1px solid #d8d8d8;
    box-sizing: border-box;
    padding: 32px 28px 24px;
    min-height: 460px;
  }

  .option-button {
    width: 100%;
    height: 62px;
    border: none;
    border-radius: 7px;
    margin-bottom: 18px;
    color: #fff;
    font-size: 18px;
    font-weight: 600;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
    cursor: pointer;
    font-family: 'Barlow', sans-serif;
    box-shadow: none;
    text-decoration: none;
  }

  .option-button.red {
    background: #d93a32;
  }

  .option-button.green {
    background: #1db36a;
  }

  .option-button.light {
    background: #f1f1f1;
    color: #000;
    border: 1px solid #d5d5d5;
  }

  a.option-button.light {
    cursor: pointer;
  }

  button.option-button.light[disabled] {
    cursor: not-allowed;
  }

  .option-button .material-icons {
    font-size: 26px;
  }

  .option-button .coming-soon {
    font-size: 12px;
    font-weight: 500;
    margin-left: 6px;
    opacity: 0.75;
  }

  .section-title {
    font-size: 38px;
    line-height: 1.2;
    margin: 0 0 26px 0;
    color: #1f1f1f;
    font-weight: 400;
    font-family: 'Barlow', sans-serif;
  }

  .field-label {
    display: block;
    color: #4b4b4b;
    font-size: 15px;
    margin: 0 0 8px 0;
    font-weight: 400;
  }

  .input-text {
    width: 100%;
    border: none;
    border-bottom: 2px solid #2f5bd7;
    background: transparent;
    height: 42px;
    font-size: 20px;
    color: #111;
    padding: 0 2px;
    margin-bottom: 18px;
    box-sizing: border-box;
    outline: none;
    font-family: 'Barlow', sans-serif;
  }

  .input-text[readonly] {
    border-bottom-color: #bdbdbd;
    color: #303030;
  }

  .input-text.password {
    letter-spacing: 1px;
  }

  .input-inline {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 18px;
  }

  .input-inline .input-text {
    flex: 1;
    margin-bottom: 0;
  }

  .inline-button {
    min-width: 120px;
    height: 44px;
    border: none;
    border-radius: 8px;
    background: #2f5bd7;
    color: #fff;
    font-size: 18px;
    font-weight: 600;
    cursor: pointer;
    text-transform: uppercase;
    font-family: 'Barlow', sans-serif;
  }

  .inline-button.light {
    background: transparent;
    color: #2f5bd7;
    border: none;
    min-width: 1px;
    padding: 0;
    width: auto;
  }

  .inline-button.primary {
    background: #2f5bd7;
  }

  .inline-button.secondary {
    background: transparent;
    color: #2f5bd7;
    min-width: auto;
    padding: 0 12px;
  }

  .actions-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 18px;
    margin-top: 10px;
  }

  .actions-row .secondary {
    background: transparent;
    border: none;
    color: #2f5bd7;
    font-size: 18px;
    text-transform: uppercase;
    cursor: pointer;
    font-family: 'Barlow', sans-serif;
  }

  .actions-row .primary {
    min-width: 150px;
    height: 52px;
    border: none;
    border-radius: 8px;
    background: #2f5bd7;
    color: #fff;
    font-size: 18px;
    font-weight: 700;
    text-transform: uppercase;
    cursor: pointer;
    font-family: 'Barlow', sans-serif;
  }

  .actions-row .primary:disabled {
    opacity: 0.7;
    cursor: not-allowed;
  }

  .muted-text {
    font-size: 14px;
    color: #4b4b4b;
    margin-top: 8px;
  }

  .hidden { display: none !important; }
</style>

<form id="kc-form-login" action="${url.loginAction}" method="post">
  <input type="hidden" id="username" name="username" v-model="email" />
  <input type="hidden" id="password" name="password" v-model="password" />
  <input type="hidden" id="phoneNumberHidden" name="phoneNumber" v-model="phoneNumber" />
  <input type="hidden" id="phoneActivated" name="phoneActivated" value="false" />
  <input type="hidden" id="code" name="code" v-model="otpCode" />
</form>

<#assign googleLoginUrl = "">
<#if social.providers??>
  <#list social.providers as p>
    <#if (p.alias?? && p.alias?lower_case?contains("google")) || (p.displayName?? && p.displayName?lower_case?contains("google"))>
      <#assign googleLoginUrl = p.loginUrl>
      <#break>
    </#if>
  </#list>
</#if>

<div id="auth-app" v-cloak>
    <div class="auth-shell">
      <div class="auth-card">
        <div v-if="screen === 'home'">
          <button type="button" class="option-button red" @click="showEmailFlow()">
            <span class="material-icons">mail</span>
            <span>Sign in with email</span>
          </button>

          <button type="button" class="option-button green" @click="showPhoneFlow()">
            <span class="material-icons">phone</span>
            <span>Sign in with phone</span>
          </button>

          <#if googleLoginUrl?has_content>
            <a class="option-button light" href="${googleLoginUrl}">
              <span class="material-icons">g_mobiledata</span>
              <span>Sign in with Google</span>
            </a>
          <#else>
            <button type="button" class="option-button light" disabled>
              <span class="material-icons">g_mobiledata</span>
              <span>Sign in with Google</span>
              <span class="coming-soon">Not configured</span>
            </button>
          </#if>
        </div>

        <div v-else-if="screen === 'email'">
          <h2 class="section-title">Sign in with email</h2>

          <label class="field-label" for="emailField">Email</label>
          <input id="emailField" class="input-text" type="email" v-model="email" placeholder="" autocomplete="email" />
          <div class="muted-text" v-if="emailErrorMessage" style="color:#d93a32;">{{ emailErrorMessage }}</div>

          <div class="actions-row">
            <button type="button" class="secondary" @click="backHome()">CANCEL</button>
            <button type="button" class="primary" :disabled="emailCheckInProgress" @click="handleEmailNext()">{{ emailCheckInProgress ? 'CHECKING' : 'NEXT' }}</button>
          </div>
        </div>

        <div v-else-if="screen === 'password'">
          <h2 class="section-title">Sign in with email</h2>

          <label class="field-label">Email</label>
          <input class="input-text" type="text" :value="email" readonly />

          <label class="field-label" for="passwordField">Password</label>
          <input id="passwordField" class="input-text password" type="password" v-model="password" autocomplete="current-password" />

          <div class="actions-row">
            <button type="button" class="secondary" @click="goBackToEmail()">CANCEL</button>
            <button type="button" class="primary" @click="submitPassword()">NEXT</button>
          </div>
        </div>

        <div v-else-if="screen === 'email-google-screen'">
          <h2 class="section-title">Sign in</h2>
          <p style="font-size:22px; margin:0 0 16px 0; color:#1f1f1f; font-weight:500;">
            You already have an account
          </p>
          <p class="muted-text" style="font-size:18px; line-height:1.35; color:#1f1f1f;">
            You’ve already used <strong>{{ email }}</strong>. Sign in with Google to continue.
          </p>

          <div class="actions-row" style="margin-top:26px;">
            <button type="button" class="secondary" @click="goBackToEmail()">CANCEL</button>
            <button type="button" class="primary" :disabled="!googleLoginUrl" @click="signInWithGoogle()">
              SIGN IN WITH GOOGLE
            </button>
          </div>
          <div class="muted-text" v-if="!googleLoginUrl" style="color:#d93a32;">
            Google sign in is not configured for this realm.
          </div>
        </div>

        <div v-else-if="screen === 'email-create-screen'">
          <h2 class="section-title">Create account</h2>
          <p class="muted-text">Enter your details and OTP sent to your email.</p>

          <label class="field-label">Email</label>
          <input class="input-text" type="text" :value="email" readonly />

          <label class="field-label" for="emailFirstNameField">First name</label>
          <input id="emailFirstNameField" class="input-text" type="text" v-model="emailFirstName" />

          <label class="field-label" for="emailLastNameField">Last name</label>
          <input id="emailLastNameField" class="input-text" type="text" v-model="emailLastName" />

          <label class="field-label" for="createPasswordField">Choose password</label>
          <input id="createPasswordField" class="input-text password" type="password" v-model="createPassword" autocomplete="new-password" />

          <label class="field-label">OTP</label>
          <div class="input-inline" style="display:flex; justify-content:center; gap:12px; flex-wrap:wrap; margin-top:8px;">
            <input v-for="(digit, index) in emailOtpDigits" :key="'email-' + index" type="text" maxlength="1" inputmode="numeric"
                   class="input-text" style="width:42px; text-align:center; font-size:22px; margin:0; border-bottom:2px solid #2f5bd7;"
                   v-model="emailOtpDigits[index]" @input="handleEmailOtpInput(index, $event)" @keydown="handleEmailOtpKeydown(index, $event)" :ref="'emailOtpInput' + index" />
          </div>

          <div class="muted-text" v-if="createAccountErrorMessage" style="color:#d93a32;">{{ createAccountErrorMessage }}</div>
          <div class="muted-text" v-if="emailOtpTimerText">{{ emailOtpTimerText }}</div>
          <div class="muted-text" v-if="emailShowResend" style="cursor:pointer; color:#2f5bd7;" @click="resendEmailOtp()">Resend OTP</div>

          <div class="actions-row">
            <button type="button" class="secondary" @click="goBackToEmail()">CANCEL</button>
            <button type="button" class="primary" :disabled="createAccountInProgress || emailOtpSendInProgress" @click="submitCreateAccount()">
              {{ createAccountInProgress ? 'VERIFYING' : (emailOtpSendInProgress ? 'SENDING OTP' : 'VERIFY & CREATE') }}
            </button>
          </div>
        </div>

        <div v-else-if="screen === 'phone-screen' || screen === 'otp-screen' || screen === 'phone-create-screen'">
          <div v-if="screen === 'phone-screen'">
            <div class="mb20">
              <label for="phoneNumberInput" class="field-label">JSW Registered Mobile Number</label>
              <input type="tel" id="phoneNumberInput" class="input-text" v-model="phoneNumber" maxlength="10" inputmode="numeric" />
            </div>

            <div class="mb20">
              <label class="field-label" for="captchaInput">Captcha</label>
              <div class="input-inline">
                <input type="text" id="captchaInput" class="input-text" v-model="captchaInput" />
                <button type="button" class="inline-button light" @click="generateCaptcha()">↻</button>
              </div>
              <div class="muted-text">Captcha: <strong>{{ currentCaptcha }}</strong></div>
            </div>
            <div class="muted-text" v-if="phoneErrorMessage" style="color:#d93a32;">{{ phoneErrorMessage }}</div>

            <div class="actions-row">
              <button type="button" class="secondary" @click="backHome()">CANCEL</button>
              <button type="button" class="primary" :disabled="phoneCheckInProgress" @click="validateAndSendOTP()">
                {{ phoneCheckInProgress ? 'CHECKING' : 'NEXT' }}
              </button>
            </div>
          </div>

          <div v-else-if="screen === 'otp-screen'">
            <h2 class="section-title">OTP Verification</h2>
            <p class="muted-text">You will receive an OTP on your registered phone number.</p>

            <div class="input-inline" style="display:flex; justify-content:center; gap:12px; flex-wrap:wrap; margin-top:26px;">
              <input v-for="(digit, index) in otpDigits" :key="index" type="text" maxlength="1" inputmode="numeric"
                     class="input-text" style="width:42px; text-align:center; font-size:22px; margin:0; border-bottom:2px solid #2f5bd7;"
                     v-model="otpDigits[index]" @input="handleOtpInput(index, $event)" @keydown="handleOtpKeydown(index, $event)" :ref="'otpInput' + index" />
            </div>

            <div class="muted-text" v-if="phoneErrorMessage" style="color:#d93a32;">{{ phoneErrorMessage }}</div>
            <div class="muted-text" v-if="timerText">{{ timerText }}</div>
            <div class="muted-text" v-if="showResend" style="cursor:pointer; color:#2f5bd7;" @click="resendOtp()">Resend OTP</div>

            <div class="actions-row" style="margin-top:26px;">
              <button type="button" class="secondary" @click="backHome()">CANCEL</button>
              <button type="button" class="primary" @click="submitPhoneOtp()">VERIFY</button>
            </div>
          </div>

          <div v-else>
            <h2 class="section-title">Create account</h2>
            <p class="muted-text">Enter your details and OTP to create your account.</p>

            <label class="field-label">Mobile number</label>
            <input class="input-text" type="text" :value="'+91' + phoneNumber" readonly />

            <label class="field-label" for="phoneFirstNameField">First name</label>
            <input id="phoneFirstNameField" class="input-text" type="text" v-model="phoneFirstName" />

            <label class="field-label" for="phoneLastNameField">Last name</label>
            <input id="phoneLastNameField" class="input-text" type="text" v-model="phoneLastName" />

            <label class="field-label">OTP</label>
            <div class="input-inline" style="display:flex; justify-content:center; gap:12px; flex-wrap:wrap; margin-top:8px;">
              <input v-for="(digit, index) in otpDigits" :key="'create-' + index" type="text" maxlength="1" inputmode="numeric"
                     class="input-text" style="width:42px; text-align:center; font-size:22px; margin:0; border-bottom:2px solid #2f5bd7;"
                     v-model="otpDigits[index]" @input="handleOtpInput(index, $event)" @keydown="handleOtpKeydown(index, $event)" :ref="'otpInput' + index" />
            </div>

            <div class="muted-text" v-if="phoneErrorMessage" style="color:#d93a32;">{{ phoneErrorMessage }}</div>
            <div class="muted-text" v-if="timerText">{{ timerText }}</div>
            <div class="muted-text" v-if="showResend" style="cursor:pointer; color:#2f5bd7;" @click="resendOtp()">Resend OTP</div>

            <div class="actions-row" style="margin-top:26px;">
              <button type="button" class="secondary" @click="backHome()">CANCEL</button>
              <button type="button" class="primary" :disabled="phoneRegisterInProgress" @click="submitPhoneRegistration()">
                {{ phoneRegisterInProgress ? 'SAVING' : 'VERIFY' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

<script type="text/javascript">
  var authApp = new Vue({
    el: '#auth-app',
    data: {
      screen: 'home',
      email: '',
      password: '',
      emailFirstName: '',
      emailLastName: '',
      createPassword: '',
      emailOtpCode: '',
      emailOtpDigits: ['', '', '', '', '', ''],
      emailOtpTimerText: '',
      emailShowResend: false,
      phoneNumber: '',
      captchaInput: '',
      currentCaptcha: '',
      otpCode: '',
      otpDigits: ['', '', '', '', '', ''],
      timerText: '',
      showResend: false,
      emailCheckInProgress: false,
      emailOtpSendInProgress: false,
      emailErrorMessage: '',
      createAccountInProgress: false,
      createAccountErrorMessage: '',
      phoneCheckInProgress: false,
      phoneRegisterInProgress: false,
      phoneUserExists: true,
      phoneFirstName: '',
      phoneLastName: '',
      phoneErrorMessage: '',
      googleLoginUrl: '<#if googleLoginUrl?has_content>${googleLoginUrl?js_string}<#else></#if>',
      registrationAllowed: <#if realm.registrationAllowed>true<#else>false</#if>
    },
    mounted: function() {
      this.generateCaptcha();
    },
    watch: {
      emailOtpDigits: {
        handler: function(val) {
          this.emailOtpCode = val.join('');
        },
        deep: true
      },
      otpDigits: {
        handler: function(val) {
          this.otpCode = val.join('');
          document.getElementById('code').value = this.otpCode;
        },
        deep: true
      }
    },
    methods: {
      backHome: function() {
        this.screen = 'home';
        this.email = '';
        this.password = '';
        this.emailCheckInProgress = false;
        this.emailErrorMessage = '';
        this.resetEmailRegistrationState();
        this.phoneNumber = '';
        this.phoneFirstName = '';
        this.phoneLastName = '';
        this.captchaInput = '';
        this.otpDigits = ['', '', '', '', '', ''];
        this.timerText = '';
        this.showResend = false;
        this.otpCode = '';
        this.phoneCheckInProgress = false;
        this.phoneRegisterInProgress = false;
        this.phoneUserExists = true;
        this.phoneErrorMessage = '';
        document.getElementById('phoneActivated').value = 'false';
        document.getElementById('phoneNumberHidden').value = '';
        document.getElementById('code').value = '';
        this.generateCaptcha();
      },
      resetEmailRegistrationState: function() {
        this.emailFirstName = '';
        this.emailLastName = '';
        this.createPassword = '';
        this.emailOtpCode = '';
        this.emailOtpDigits = ['', '', '', '', '', ''];
        this.emailOtpTimerText = '';
        this.emailShowResend = false;
        this.emailOtpSendInProgress = false;
        this.createAccountInProgress = false;
        this.createAccountErrorMessage = '';
      },
      showEmailFlow: function() {
        this.screen = 'email';
        this.emailErrorMessage = '';
        this.password = '';
        this.emailCheckInProgress = false;
        this.resetEmailRegistrationState();
      },
      showPhoneFlow: function() {
        this.screen = 'phone-screen';
        this.phoneErrorMessage = '';
        this.phoneCheckInProgress = false;
        this.phoneRegisterInProgress = false;
        this.phoneUserExists = true;
        this.phoneFirstName = '';
        this.phoneLastName = '';
        this.otpDigits = ['', '', '', '', '', ''];
        this.otpCode = '';
        this.timerText = '';
        this.showResend = false;
        this.generateCaptcha();
      },
      signInWithGoogle: function() {
        if (!this.googleLoginUrl) {
          this.emailErrorMessage = 'Google sign in is not configured for this realm.';
          return;
        }
        window.location.href = this.googleLoginUrl;
      },
      handleEmailNext: function() {
        const value = (this.email || '').trim();
        if (!value) {
          this.emailErrorMessage = 'Please enter your email.';
          return;
        }
        var normalizedValue = value.toLowerCase();
        this.emailErrorMessage = '';
        this.email = normalizedValue;
        this.emailCheckInProgress = true;

        var vm = this;
        axios.get(window.location.origin + '/realms/${realm.name}/sms/user-exists', {
          params: { email: normalizedValue }
        }).then(function(res) {
          var existsValue = res && res.data ? res.data.exists : false;
          var exists = existsValue === true || existsValue === 'true';
          var googleLinkedValue = res && res.data ? res.data.googleLinked : false;
          var googleLinked = googleLinkedValue === true || googleLinkedValue === 'true';
          if (!exists && !vm.registrationAllowed) {
            vm.emailErrorMessage = 'Registration is disabled. Please contact support.';
            return;
          }
          vm.createAccountErrorMessage = '';
          if (exists) {
            if (googleLinked) {
              vm.screen = 'email-google-screen';
              return;
            }
            vm.screen = 'password';
            return;
          }
          vm.resetEmailRegistrationState();
          vm.screen = 'email-create-screen';
          vm.sendRegistrationEmailOtp();
        }).catch(function(err) {
          vm.emailErrorMessage = 'Unable to verify email right now. Please try again.';
        }).then(function() {
          vm.emailCheckInProgress = false;
        });
      },
      goBackToEmail: function() {
        this.screen = 'email';
        this.emailErrorMessage = '';
        this.password = '';
        this.resetEmailRegistrationState();
      },
      submitEmailLoginWithPassword: function(password) {
        document.getElementById('phoneActivated').value = 'false';
        document.getElementById('phoneNumberHidden').value = '';
        document.getElementById('code').value = '';
        document.getElementById('username').value = this.email;
        document.getElementById('password').value = password;
        document.getElementById('kc-form-login').submit();
      },
      submitPassword: function() {
        if (this.password && this.password.length > 0) {
          this.submitEmailLoginWithPassword(this.password);
        }
      },
      sendRegistrationEmailOtp: function() {
        this.createAccountErrorMessage = '';
        this.emailOtpSendInProgress = true;

        var payload = new URLSearchParams();
        payload.append('email', this.email);

        var vm = this;
        axios.post(window.location.origin + '/realms/${realm.name}/sms/send-registration-email-otp',
          payload,
          { headers: { 'Content-Type': 'application/x-www-form-urlencoded' } }
        ).then(function(res) {
          var expiresIn = res && res.data ? Number(res.data.expires_in || 0) : 0;
          vm.emailOtpDigits = ['', '', '', '', '', ''];
          vm.emailOtpCode = '';
          vm.disableEmailOtpSend(expiresIn > 0 ? expiresIn : 60);
        }).catch(function(err) {
          var status = err && err.response ? err.response.status : 0;
          if (status === 409) {
            vm.createAccountErrorMessage = 'Email is already registered. Please sign in with password.';
            vm.screen = 'password';
            vm.emailShowResend = false;
            vm.emailOtpTimerText = '';
            return;
          }
          if (status === 403) {
            vm.createAccountErrorMessage = 'Registration is disabled. Please contact support.';
            vm.emailShowResend = false;
            vm.emailOtpTimerText = '';
            return;
          }
          vm.createAccountErrorMessage = 'Unable to send OTP right now. Please try again.';
          vm.emailShowResend = true;
          vm.emailOtpTimerText = '';
        }).then(function() {
          vm.emailOtpSendInProgress = false;
        });
      },
      disableEmailOtpSend: function(seconds) {
        var vm = this;
        if (seconds <= 0) {
          this.emailOtpTimerText = '';
          this.emailShowResend = true;
          return;
        }
        var mins = Math.floor(seconds / 60);
        var secs = seconds % 60;
        this.emailOtpTimerText = 'Resend OTP in ' + String(mins).padStart(2, '0') + ':' + String(secs).padStart(2, '0');
        this.emailShowResend = false;
        setTimeout(function() {
          vm.disableEmailOtpSend(seconds - 1);
        }, 1000);
      },
      resendEmailOtp: function() {
        if (this.emailOtpSendInProgress) {
          return;
        }
        this.sendRegistrationEmailOtp();
      },
      handleEmailOtpInput: function(index, event) {
        var value = (event.target.value || '').replace(/\D/g, '');
        this.emailOtpDigits[index] = value.slice(-1);
        if (value && index < 5) {
          var next = this.$refs['emailOtpInput' + (index + 1)];
          if (next && next[0]) {
            next[0].focus();
          }
        }
      },
      handleEmailOtpKeydown: function(index, event) {
        if (event.key === 'Backspace' && !this.emailOtpDigits[index] && index > 0) {
          var prev = this.$refs['emailOtpInput' + (index - 1)];
          if (prev && prev[0]) {
            prev[0].focus();
          }
        }
      },
      submitCreateAccount: function() {
        var firstName = (this.emailFirstName || '').trim();
        var lastName = (this.emailLastName || '').trim();
        var otp = this.emailOtpCode || this.emailOtpDigits.join('');

        if (!firstName || !lastName) {
          this.createAccountErrorMessage = 'Please enter first name and last name.';
          return;
        }
        if (!this.createPassword) {
          this.createAccountErrorMessage = 'Please choose a password.';
          return;
        }
        if (!otp || otp.length !== 6) {
          this.createAccountErrorMessage = 'Please enter a valid 6-digit OTP.';
          return;
        }
        this.createAccountErrorMessage = '';
        this.createAccountInProgress = true;

        document.getElementById('phoneActivated').value = 'false';
        document.getElementById('phoneNumberHidden').value = '';
        document.getElementById('code').value = '';

        var payload = new URLSearchParams();
        payload.append('email', this.email);
        payload.append('code', otp);
        payload.append('firstName', firstName);
        payload.append('lastName', lastName);
        payload.append('password', this.createPassword);

        var vm = this;
        axios.post(window.location.origin + '/realms/${realm.name}/sms/register-user',
          payload,
          { headers: { 'Content-Type': 'application/x-www-form-urlencoded' } }
        ).then(function() {
          vm.password = vm.createPassword;
          vm.submitEmailLoginWithPassword(vm.password);
        }).catch(function(err) {
          var status = err && err.response ? err.response.status : 0;
          var errorCode = err && err.response && err.response.data ? err.response.data.error : '';
          var remainingAttempts = err && err.response && err.response.data ? err.response.data.remainingAttempts : null;
          if (status === 409) {
            vm.createAccountErrorMessage = 'Email is already registered. Please sign in with password.';
            vm.screen = 'password';
            return;
          }
          if (status === 429 || errorCode === 'otp_attempts_exceeded') {
            vm.createAccountErrorMessage = 'Maximum OTP attempts reached. Please resend OTP and try again.';
            vm.emailOtpTimerText = '';
            vm.emailShowResend = true;
            return;
          }
          if (status === 403 && errorCode === 'invalid_code') {
            if (remainingAttempts !== null && remainingAttempts !== undefined) {
              vm.createAccountErrorMessage = 'Incorrect OTP. ' + remainingAttempts + ' attempt(s) left.';
            } else {
              vm.createAccountErrorMessage = 'Incorrect OTP. Please enter the correct code sent to your email.';
            }
            return;
          }
          if (status === 400 && errorCode === 'code_expired') {
            vm.createAccountErrorMessage = 'OTP expired. Please resend OTP.';
            vm.emailOtpTimerText = '';
            vm.emailShowResend = true;
            return;
          }
          if (status === 400 && errorCode === 'invalid_password') {
            vm.createAccountErrorMessage = 'Password does not meet realm policy. Please choose a stronger password.';
            return;
          }
          if (status === 403) {
            vm.createAccountErrorMessage = 'Registration is disabled. Please contact support.';
            return;
          }
          vm.createAccountErrorMessage = 'Unable to create account. Please try again.';
        }).then(function() {
          vm.createAccountInProgress = false;
        });
      },
      generateCaptcha: function() {
        const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
        let captcha = '';
        for (let i = 0; i < 5; i++) {
          captcha += chars.charAt(Math.floor(Math.random() * chars.length));
        }
        this.currentCaptcha = captcha;
        this.captchaInput = '';
      },
      validateAndSendOTP: function() {
        if ((this.phoneNumber || '').trim().length !== 10) {
          this.phoneErrorMessage = 'Please enter a valid 10-digit mobile number.';
          return;
        }
        if ((this.captchaInput || '').trim().toUpperCase() !== (this.currentCaptcha || '').toUpperCase()) {
          this.phoneErrorMessage = 'Captcha does not match. Please try again.';
          this.generateCaptcha();
          return;
        }
        this.phoneErrorMessage = '';
        this.checkPhoneAndSendOtp();
      },
      checkPhoneAndSendOtp: function() {
        var vm = this;
        vm.phoneCheckInProgress = true;
        var normalizedPhone = '+91' + vm.phoneNumber;
        axios.get(window.location.origin + '/realms/${realm.name}/sms/phone-exists', {
          params: { phoneNumber: normalizedPhone }
        }).then(function(res) {
          var existsValue = res && res.data ? res.data.exists : false;
          vm.phoneUserExists = existsValue === true || existsValue === 'true';
          vm.sendOtpForPhone(vm.phoneUserExists ? 'otp-screen' : 'phone-create-screen');
        }).catch(function(err) {
          var backendError = err && err.response && err.response.data ? (err.response.data.error || err.response.data.error_description) : '';
          vm.phoneErrorMessage = backendError || 'Unable to verify phone right now. Please try again.';
          vm.generateCaptcha();
        }).then(function() {
          vm.phoneCheckInProgress = false;
        });
      },
      sendOtpForPhone: function(nextScreen) {
        var vm = this;
        var normalizedPhone = '+91' + vm.phoneNumber;
        axios.get(window.location.origin + '/realms/${realm.name}/sms/authentication-code', {
          params: { phoneNumber: normalizedPhone }
        }).then(function(res) {
          vm.screen = nextScreen || 'otp-screen';
          vm.showResend = false;
          vm.otpDigits = ['', '', '', '', '', ''];
          vm.otpCode = '';
          vm.phoneErrorMessage = '';
          vm.disableSend(res.data.expires_in || 60);
        }).catch(function(err) {
          var backendError = err && err.response && err.response.data ? (err.response.data.error || err.response.data.error_description) : '';
          vm.phoneErrorMessage = backendError || 'Unable to send OTP right now. Please try again.';
          vm.generateCaptcha();
        });
      },
      disableSend: function(seconds) {
        var vm = this;
        if (seconds <= 0) {
          this.timerText = '';
          this.showResend = true;
          return;
        }

        var mins = Math.floor(seconds / 60);
        var secs = seconds % 60;
        this.timerText = 'Resend OTP in ' + String(mins).padStart(2, '0') + ':' + String(secs).padStart(2, '0');
        this.showResend = false;
        setTimeout(function() {
          vm.disableSend(seconds - 1);
        }, 1000);
      },
      handleOtpInput: function(index, event) {
        var value = (event.target.value || '').replace(/\D/g, '');
        this.otpDigits[index] = value.slice(-1);
        if (value && index < 5) {
          var next = this.$refs['otpInput' + (index + 1)];
          if (next && next[0]) {
            next[0].focus();
          }
        }
      },
      handleOtpKeydown: function(index, event) {
        if (event.key === 'Backspace' && !this.otpDigits[index]) {
          if (index > 0) {
            var prev = this.$refs['otpInput' + (index - 1)];
            if (prev && prev[0]) {
              prev[0].focus();
            }
          }
        }
      },
      resendOtp: function() {
        this.otpDigits = ['', '', '', '', '', ''];
        this.otpCode = '';
        this.phoneErrorMessage = '';
        this.sendOtpForPhone(this.phoneUserExists ? 'otp-screen' : 'phone-create-screen');
      },
      submitPhoneOtp: function() {
        var otp = this.otpCode || this.otpDigits.join('');
        if (!otp || otp.length !== 6) {
          this.phoneErrorMessage = 'Please enter a valid 6-digit OTP.';
          return;
        }
        this.phoneErrorMessage = '';
        this.submitPhoneLoginWithOtp(otp);
      },
      submitPhoneRegistration: function() {
        var firstName = (this.phoneFirstName || '').trim();
        var lastName = (this.phoneLastName || '').trim();
        var otp = this.otpCode || this.otpDigits.join('');

        if (!firstName || !lastName) {
          this.phoneErrorMessage = 'Please enter first name and last name.';
          return;
        }
        if (!otp || otp.length !== 6) {
          this.phoneErrorMessage = 'Please enter a valid 6-digit OTP.';
          return;
        }

        this.phoneRegisterInProgress = true;
        this.phoneErrorMessage = '';

        var payload = new URLSearchParams();
        payload.append('phoneNumber', '+91' + this.phoneNumber);
        payload.append('code', otp);
        payload.append('firstName', firstName);
        payload.append('lastName', lastName);

        var vm = this;
        axios.post(window.location.origin + '/realms/${realm.name}/sms/register-phone-user',
          payload,
          { headers: { 'Content-Type': 'application/x-www-form-urlencoded' } }
        ).then(function() {
          vm.phoneUserExists = true;
          vm.submitPhoneLoginWithOtp(otp);
        }).catch(function(err) {
          var status = err && err.response ? err.response.status : 0;
          var errorCode = err && err.response && err.response.data ? err.response.data.error : '';

          if (status === 409) {
            vm.phoneErrorMessage = 'Phone number is already registered. Please sign in with OTP.';
            vm.phoneUserExists = true;
            vm.screen = 'otp-screen';
            return;
          }
          if (status === 403 && errorCode === 'invalid_code') {
            vm.phoneErrorMessage = 'Incorrect OTP. Please enter the correct code sent to your phone.';
            return;
          }
          if (status === 400 && errorCode === 'code_expired') {
            vm.phoneErrorMessage = 'OTP expired. Please resend OTP.';
            return;
          }
          if (status === 403) {
            vm.phoneErrorMessage = 'Registration is disabled. Please contact support.';
            return;
          }
          vm.phoneErrorMessage = 'Unable to create account right now. Please try again.';
        }).then(function() {
          vm.phoneRegisterInProgress = false;
        });
      },
      submitPhoneLoginWithOtp: function(otp) {
        document.getElementById('phoneActivated').value = 'true';
        document.getElementById('username').value = '';
        document.getElementById('password').value = '';
        document.getElementById('phoneNumberHidden').value = '+91' + this.phoneNumber;
        document.getElementById('code').value = otp;
        document.getElementById('kc-form-login').submit();
      }
    }
  });
</script>
</#if>
</@layout.registrationLayout>

