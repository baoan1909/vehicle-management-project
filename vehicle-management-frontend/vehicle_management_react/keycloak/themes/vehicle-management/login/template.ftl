<#assign frontendBase = "">
<#if client?? && client.baseUrl?? && client.baseUrl?has_content>
  <#assign frontendBase = client.baseUrl>
<#elseif properties.frontendUrl?? && properties.frontendUrl?has_content>
  <#assign frontendBase = properties.frontendUrl>
<#else>
  <#assign frontendBase = "http://localhost:5173">
</#if>
<#if frontendBase?ends_with("/")>
  <#assign frontendBase = frontendBase?substring(0, frontendBase?length - 1)>
</#if>
<#assign frontendLoginUrl = frontendBase + "/login">
<#assign frontendRegisterUrl = frontendBase + "/register">
<#assign frontendForgotPasswordUrl = frontendBase + "/forgot-password">
<#assign frontendPricingUrl = frontendBase + "/pricing">
<#assign frontendContactUrl = frontendBase + "/contact">
<#macro authLayout title description="" cardClassName="" compactHeader=false split=false>
<!DOCTYPE html>
<html lang="${((locale.currentLanguageTag)!'vi')}">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="robots" content="noindex, nofollow">
  <title>${title} - CoParking</title>
  <link rel="icon" href="${url.resourcesPath}/img/AdminLTELogo.png">
  <link rel="stylesheet" href="${url.resourcesPath}/vendor/fontawesome-free/css/all.min.css">
  <link rel="stylesheet" href="${url.resourcesPath}/css/coparking-auth.css">
</head>
<body class="login-page <#if split>vm-auth-page-split</#if>">
  <div class="vm-auth-shell">
    <#if !split>
      <header class="vm-auth-topbar">
        <a class="vm-auth-brand" href="${frontendPricingUrl}">
          <span class="vm-auth-brand-mark">
            <img src="${url.resourcesPath}/img/AdminLTELogo.png" alt="CoParking">
          </span>
          <span>CoParking</span>
        </a>
        <a class="vm-auth-help" href="${frontendContactUrl}" aria-label="Trợ giúp">
          <i class="far fa-question-circle" aria-hidden="true"></i>
        </a>
      </header>
    </#if>

    <main class="vm-auth-main <#if split>vm-auth-main-split</#if>">
      <#if split>
        <section class="vm-auth-split-card ${cardClassName}">
          <aside class="vm-auth-brand-panel" aria-label="CoParking">
            <div class="vm-auth-panel-copy">
              <h2>Bãi gửi xe thông minh</h2>
              <p>Đăng nhập để tiếp tục sử dụng các tiện ích bãi xe, từ vận hành hệ thống đến theo dõi phương tiện và thẻ xe của bạn.</p>
            </div>

            <div class="vm-auth-panel-stats" aria-label="Tính năng bãi xe">
              <div>
                <span class="vm-auth-stat-icon vm-auth-stat-icon-clock" aria-hidden="true"></span>
                <span class="vm-auth-stat-copy">
                  <strong>24/7</strong>
                  <span>Vận hành</span>
                </span>
              </div>
              <div>
                <span class="vm-auth-stat-icon vm-auth-stat-icon-rfid" aria-hidden="true"></span>
                <span class="vm-auth-stat-copy">
                  <strong>RFID</strong>
                  <span>Kiểm soát</span>
                </span>
              </div>
            </div>
          </aside>

          <section class="vm-auth-form-panel">
            <div class="vm-auth-form-panel-inner">
              <div class="vm-auth-header <#if compactHeader>vm-auth-header-compact</#if>">
                <div class="vm-auth-logo">
                  <img src="${url.resourcesPath}/img/AdminLTELogo.png" alt="CoParking">
                </div>
                <h1>${title}</h1>
                <#if description?has_content>
                  <p>${description}</p>
                </#if>
              </div>

              <#if message?has_content && message.summary?has_content>
                <#assign alertType = ((message.type)!'info')>
                <div class="vm-auth-alert vm-auth-alert-${alertType}" role="alert">
                  <#if alertType == "success">
                    <i class="fas fa-check-circle" aria-hidden="true"></i>
                  <#elseif alertType == "warning">
                    <i class="fas fa-exclamation-triangle" aria-hidden="true"></i>
                  <#elseif alertType == "error">
                    <i class="fas fa-exclamation-circle" aria-hidden="true"></i>
                  <#else>
                    <i class="fas fa-info-circle" aria-hidden="true"></i>
                  </#if>
                  <span>${message.summary}</span>
                </div>
              </#if>

              <#nested>
            </div>
          </section>
        </section>
      <#else>
        <section class="vm-auth-card ${cardClassName}">
          <div class="vm-auth-header <#if compactHeader>vm-auth-header-compact</#if>">
            <div class="vm-auth-logo">
              <img src="${url.resourcesPath}/img/AdminLTELogo.png" alt="CoParking">
            </div>
            <h1>${title}</h1>
            <#if description?has_content>
              <p>${description}</p>
            </#if>
          </div>

          <#if message?has_content && message.summary?has_content>
            <#assign alertType = ((message.type)!'info')>
            <div class="vm-auth-alert vm-auth-alert-${alertType}" role="alert">
              <#if alertType == "success">
                <i class="fas fa-check-circle" aria-hidden="true"></i>
              <#elseif alertType == "warning">
                <i class="fas fa-exclamation-triangle" aria-hidden="true"></i>
              <#elseif alertType == "error">
                <i class="fas fa-exclamation-circle" aria-hidden="true"></i>
              <#else>
                <i class="fas fa-info-circle" aria-hidden="true"></i>
              </#if>
              <span>${message.summary}</span>
            </div>
          </#if>

          <#nested>
        </section>
      </#if>
    </main>

    <#if !split>
      <footer class="vm-auth-footer">
        <span>© 2026 Hệ thống quản lý bãi xe. All rights reserved.</span>
        <nav aria-label="Liên kết phụ">
          <a href="${frontendPricingUrl}">Điều khoản</a>
          <a href="${frontendPricingUrl}">Bảo mật</a>
          <a href="${frontendContactUrl}">Liên hệ</a>
        </nav>
      </footer>
    </#if>
  </div>
  <script src="${url.resourcesPath}/js/coparking-auth.js"></script>
</body>
</html>
</#macro>
