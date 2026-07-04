<#import "template.ftl" as layout>
<@layout.authLayout title="Đặt lại mật khẩu" description="Nhập mật khẩu mới cho tài khoản của bạn." cardClassName="vm-auth-card-reset">
  <form id="kc-passwd-update-form" class="vm-auth-form" action="${url.loginAction}" method="post">
    <div class="vm-auth-field">
      <label for="password-new">Nhập mật khẩu mới</label>
      <div class="vm-auth-input-shell">
        <i class="fas fa-lock" aria-hidden="true"></i>
        <input id="password-new" name="password-new" type="password" placeholder="••••••••" autocomplete="new-password" autofocus>
        <button class="vm-auth-eye" type="button" aria-label="Hiển thị mật khẩu" aria-pressed="false" data-password-toggle>
          <i class="far fa-eye" aria-hidden="true"></i>
        </button>
      </div>
    </div>

    <div class="vm-auth-field">
      <label for="password-confirm">Nhập lại mật khẩu mới</label>
      <div class="vm-auth-input-shell">
        <i class="fas fa-lock" aria-hidden="true"></i>
        <input id="password-confirm" name="password-confirm" type="password" placeholder="••••••••" autocomplete="new-password">
        <button class="vm-auth-eye" type="button" aria-label="Hiển thị mật khẩu" aria-pressed="false" data-password-toggle>
          <i class="far fa-eye" aria-hidden="true"></i>
        </button>
      </div>
    </div>

    <div class="vm-auth-note">
      <i class="fas fa-shield-alt" aria-hidden="true"></i>
      <span>Nên dùng ít nhất 8 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt.</span>
    </div>

    <button class="vm-auth-submit" type="submit">
      Đặt lại mật khẩu
    </button>
  </form>

  <div class="vm-auth-back">
    <a href="${layout.frontendLoginUrl}">
      <i class="fas fa-arrow-left" aria-hidden="true"></i>
      Quay lại đăng nhập
    </a>
  </div>
</@layout.authLayout>
