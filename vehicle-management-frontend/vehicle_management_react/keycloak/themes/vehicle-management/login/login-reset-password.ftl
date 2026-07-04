<#import "template.ftl" as layout>
<#assign attemptedUsername = "">
<#if auth?? && auth.attemptedUsername?? && auth.attemptedUsername?has_content>
  <#assign attemptedUsername = auth.attemptedUsername>
<#elseif login?? && login.username?? && login.username?has_content>
  <#assign attemptedUsername = login.username>
</#if>
<@layout.authLayout title="Quên mật khẩu?" cardClassName="vm-auth-card-reset">
  <form id="kc-reset-password-form" class="vm-auth-form" action="${url.loginAction}" method="post">
    <div class="vm-auth-field">
      <label for="username">Email nhận OTP</label>
      <div class="vm-auth-input-shell">
        <i class="far fa-envelope" aria-hidden="true"></i>
        <input id="username" name="username" type="text" value="${attemptedUsername}" placeholder="customer@example.com" autocomplete="email" autofocus>
      </div>
    </div>

    <p class="vm-auth-field-note">Mã OTP sẽ được gửi đến email dùng cho việc đặt lại mật khẩu mới.</p>

    <button class="vm-auth-submit" type="submit">
      Nhận OTP
    </button>
  </form>

  <div class="vm-auth-back">
    <a href="${layout.frontendLoginUrl}">
      <i class="fas fa-arrow-left" aria-hidden="true"></i>
      Quay lại đăng nhập
    </a>
  </div>
</@layout.authLayout>
