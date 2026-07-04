<#import "template.ftl" as layout>
<#assign continueUrl = layout.frontendLoginUrl>
<#if pageRedirectUri?? && pageRedirectUri?has_content>
  <#assign continueUrl = pageRedirectUri>
<#elseif actionUri?? && actionUri?has_content>
  <#assign continueUrl = actionUri>
<#elseif client?? && client.baseUrl?? && client.baseUrl?has_content>
  <#assign continueUrl = client.baseUrl>
</#if>
<@layout.authLayout title="Hoàn tất" description="Thao tác bảo mật của bạn đã được xử lý." cardClassName="vm-auth-card-reset">
  <div class="vm-auth-form">
    <div class="vm-auth-note vm-auth-note-success">
      <i class="fas fa-check-circle" aria-hidden="true"></i>
      <span>Bạn có thể quay lại CoParking để tiếp tục sử dụng hệ thống.</span>
    </div>

    <a class="vm-auth-submit" href="${continueUrl}">
      Tiếp tục
      <i class="fas fa-arrow-right" aria-hidden="true"></i>
    </a>
  </div>
</@layout.authLayout>
