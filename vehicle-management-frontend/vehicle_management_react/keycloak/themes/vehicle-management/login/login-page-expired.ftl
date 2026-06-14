<#import "template.ftl" as layout>
<#assign restartUrl = layout.frontendLoginUrl>
<#if url.loginRestartFlowUrl?? && url.loginRestartFlowUrl?has_content>
  <#assign restartUrl = url.loginRestartFlowUrl>
</#if>
<@layout.authLayout title="Phiên đăng nhập đã hết hạn" description="Vui lòng bắt đầu lại để tiếp tục thao tác bảo mật." cardClassName="vm-auth-card-reset">
  <div class="vm-auth-form">
    <div class="vm-auth-note">
      <i class="fas fa-shield-alt" aria-hidden="true"></i>
      <span>Phiên hiện tại không còn hiệu lực để bảo vệ tài khoản của bạn.</span>
    </div>

    <a class="vm-auth-submit" href="${restartUrl}">
      Bắt đầu lại
    </a>
  </div>
</@layout.authLayout>
