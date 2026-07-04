<#import "template.ftl" as layout>
<@layout.authLayout title="Đăng nhập" split=true>
  <form id="kc-form-login" class="vm-auth-form" action="${url.loginAction}" method="post">
    <div class="vm-auth-field">
      <label for="username">Email <span class="vm-auth-required">*</span></label>
      <div class="vm-auth-input-shell">
        <i class="far fa-envelope" aria-hidden="true"></i>
        <input
          id="username"
          name="username"
          type="text"
          value="${((login.username)!'')}"
          placeholder="Email"
          autocomplete="username"
          autofocus
          <#if usernameEditDisabled?? && usernameEditDisabled>disabled</#if>
        >
        <#if usernameEditDisabled?? && usernameEditDisabled>
          <input type="hidden" name="username" value="${((login.username)!'')}">
        </#if>
      </div>
    </div>

    <div class="vm-auth-field">
      <label for="password">Mật khẩu <span class="vm-auth-required">*</span></label>
      <div class="vm-auth-input-shell">
        <i class="fas fa-lock" aria-hidden="true"></i>
        <input id="password" name="password" type="password" placeholder="Mật khẩu" autocomplete="current-password">
        <button class="vm-auth-eye" type="button" aria-label="Hiển thị mật khẩu" aria-pressed="false" data-password-toggle>
          <i class="far fa-eye" aria-hidden="true"></i>
        </button>
      </div>
    </div>

    <div class="vm-auth-options">
      <label class="vm-auth-checkbox">
        <input id="rememberMe" name="rememberMe" type="checkbox" <#if (login.rememberMe)?? && login.rememberMe>checked</#if>>
        <span>Ghi nhớ đăng nhập</span>
      </label>
      <a href="${layout.frontendForgotPasswordUrl}">Quên mật khẩu?</a>
    </div>

    <#if auth?? && auth.selectedCredential?? && auth.selectedCredential?has_content>
      <input type="hidden" name="credentialId" value="${auth.selectedCredential}">
    </#if>

    <button id="kc-login" class="vm-auth-submit" name="login" type="submit">
      Đăng nhập
    </button>
  </form>

  <div class="vm-auth-divider" aria-hidden="true">
    <span></span>
    <em>HOẶC</em>
    <span></span>
  </div>

  <#assign googleProviderUrl = "">
  <#if social?? && social.providers?? && social.providers?has_content>
    <#list social.providers as identityProvider>
      <#if ((identityProvider.alias)!'') == "google">
        <#assign googleProviderUrl = identityProvider.loginUrl>
      </#if>
    </#list>
  </#if>

  <#if googleProviderUrl?has_content>
    <a class="vm-auth-google" href="${googleProviderUrl}">
      <svg class="vm-auth-google-mark" viewBox="0 0 18 18" aria-hidden="true" focusable="false">
        <path fill="#4285F4" d="M17.64 9.2c0-.64-.06-1.25-.16-1.84H9v3.48h4.84a4.14 4.14 0 0 1-1.8 2.72v2.26h2.91c1.7-1.57 2.69-3.88 2.69-6.62Z"/>
        <path fill="#34A853" d="M9 18c2.43 0 4.47-.81 5.96-2.18l-2.91-2.26c-.81.54-1.84.86-3.05.86-2.35 0-4.34-1.59-5.05-3.72H.94v2.33A9 9 0 0 0 9 18Z"/>
        <path fill="#FBBC05" d="M3.95 10.7a5.41 5.41 0 0 1 0-3.4V4.97H.94a9 9 0 0 0 0 8.06l3.01-2.33Z"/>
        <path fill="#EA4335" d="M9 3.58c1.32 0 2.51.45 3.44 1.35l2.58-2.58A8.65 8.65 0 0 0 9 0 9 9 0 0 0 .94 4.97L3.95 7.3C4.66 5.17 6.65 3.58 9 3.58Z"/>
      </svg>
      <span>Đăng nhập bằng Google</span>
    </a>
  <#else>
    <button class="vm-auth-google" type="button" aria-disabled="true">
      <svg class="vm-auth-google-mark" viewBox="0 0 18 18" aria-hidden="true" focusable="false">
        <path fill="#4285F4" d="M17.64 9.2c0-.64-.06-1.25-.16-1.84H9v3.48h4.84a4.14 4.14 0 0 1-1.8 2.72v2.26h2.91c1.7-1.57 2.69-3.88 2.69-6.62Z"/>
        <path fill="#34A853" d="M9 18c2.43 0 4.47-.81 5.96-2.18l-2.91-2.26c-.81.54-1.84.86-3.05.86-2.35 0-4.34-1.59-5.05-3.72H.94v2.33A9 9 0 0 0 9 18Z"/>
        <path fill="#FBBC05" d="M3.95 10.7a5.41 5.41 0 0 1 0-3.4V4.97H.94a9 9 0 0 0 0 8.06l3.01-2.33Z"/>
        <path fill="#EA4335" d="M9 3.58c1.32 0 2.51.45 3.44 1.35l2.58-2.58A8.65 8.65 0 0 0 9 0 9 9 0 0 0 .94 4.97L3.95 7.3C4.66 5.17 6.65 3.58 9 3.58Z"/>
      </svg>
      <span>Đăng nhập bằng Google</span>
    </button>
  </#if>

  <div class="vm-auth-switch">
    <span>Chưa có tài khoản?</span>
    <a href="${layout.frontendRegisterUrl}">Đăng ký</a>
  </div>
</@layout.authLayout>
