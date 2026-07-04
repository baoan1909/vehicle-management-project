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
})();
