(function () {
  function updatePasswordToggle(button, input, isVisible) {
    input.type = isVisible ? "text" : "password";
    input.toggleAttribute("data-password-visible", isVisible);
    button.setAttribute("aria-pressed", String(isVisible));
    button.setAttribute("aria-label", isVisible ? "Ẩn mật khẩu" : "Hiển thị mật khẩu");

    var icon = button.querySelector("i");
    if (icon) {
      icon.classList.toggle("fa-eye", !isVisible);
      icon.classList.toggle("fa-eye-slash", isVisible);
    }
  }

  document.querySelectorAll("[data-password-toggle]").forEach(function (button) {
    var shell = button.closest(".vm-auth-input-shell");
    if (!shell) return;

    var input = shell.querySelector("input");
    if (!input) return;

    button.addEventListener("click", function () {
      updatePasswordToggle(button, input, input.type === "password");
      input.focus();
    });
  });

  var forgotPasswordLink = document.querySelector("[data-forgot-password-link]");
  var usernameInput = document.querySelector("#username");

  if (forgotPasswordLink && usernameInput) {
    var forgotPasswordBaseUrl = forgotPasswordLink.getAttribute("data-forgot-password-base-url") || forgotPasswordLink.href;

    function syncForgotPasswordLinkEmail() {
      var email = (usernameInput.value || "").trim();
      var targetUrl = new URL(forgotPasswordBaseUrl, window.location.href);

      if (email) {
        targetUrl.searchParams.set("email", email);
      } else {
        targetUrl.searchParams.delete("email");
      }

      forgotPasswordLink.href = targetUrl.toString();
    }

    usernameInput.addEventListener("input", syncForgotPasswordLinkEmail);
    forgotPasswordLink.addEventListener("click", syncForgotPasswordLinkEmail);
    syncForgotPasswordLinkEmail();
  }
})();
