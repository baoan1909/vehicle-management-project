<#import "template.ftl" as layout>
<@layout.authLayout title="Liên kết không còn hiệu lực" description="Liên kết này đã hết hạn hoặc không hợp lệ. Bạn có thể quay lại CoParking để yêu cầu gửi lại email." cardClassName="vm-auth-card-reset">
  <div class="vm-auth-form">
    <div class="vm-auth-note vm-auth-note-danger">
      <i class="fas fa-exclamation-circle" aria-hidden="true"></i>
      <span>Nếu bạn vừa thực hiện thao tác này, hãy thử yêu cầu một liên kết mới từ trang đăng nhập.</span>
    </div>

    <a class="vm-auth-submit" href="${layout.frontendLoginUrl}">
      Quay lại đăng nhập
    </a>
  </div>
</@layout.authLayout>
