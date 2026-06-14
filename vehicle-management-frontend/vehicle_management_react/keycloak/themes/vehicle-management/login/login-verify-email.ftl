<#import "template.ftl" as layout>
<@layout.authLayout title="Xác minh email" description="Vui lòng kiểm tra email và hoàn tất bước xác minh tài khoản CoParking." cardClassName="vm-auth-card-reset">
  <div class="vm-auth-form">
    <div class="vm-auth-note vm-auth-note-success">
      <i class="far fa-clock" aria-hidden="true"></i>
      <span>Liên kết xác minh đã được gửi đến email của bạn. Nếu chưa nhận được, bạn có thể yêu cầu gửi lại.</span>
    </div>

    <#if url.loginAction?? && url.loginAction?has_content>
      <form class="vm-auth-form" action="${url.loginAction}" method="post">
        <button class="vm-auth-submit" type="submit">
          Gửi lại email xác minh
        </button>
      </form>
    </#if>
  </div>

  <div class="vm-auth-back">
    <a href="${layout.frontendLoginUrl}">
      <i class="fas fa-arrow-left" aria-hidden="true"></i>
      Quay lại đăng nhập
    </a>
  </div>
</@layout.authLayout>
