document.addEventListener("DOMContentLoaded", () => {
  document.querySelectorAll("[data-account-text-visibility]").forEach((container) => {
    const number = container.querySelector("[data-account-number-text]");
    const button = container.querySelector("[data-account-text-toggle]");
    if (!number || !button) return;

    button.addEventListener("click", () => {
      const visible = button.getAttribute("aria-pressed") === "true";
      number.textContent = visible ? number.dataset.maskedNumber : number.dataset.fullNumber;
      button.setAttribute("aria-pressed", String(!visible));
      button.setAttribute("aria-label", visible ? "Show full account number" : "Hide full account number");
      button.title = visible ? "Show account number" : "Hide account number";
    });
  });

  document.querySelectorAll("[data-account-input-toggle]").forEach((button) => {
    const wrapper = button.closest(".account-input-wrap");
    const input = wrapper?.querySelector("[data-account-number-input]");
    if (!input) return;

    button.addEventListener("click", () => {
      const visible = input.type === "text";
      input.type = visible ? "password" : "text";
      button.setAttribute("aria-pressed", String(!visible));
      button.setAttribute("aria-label", visible ? "Show recipient account number" : "Hide recipient account number");
      button.title = visible ? "Show account number" : "Hide account number";
      input.focus({ preventScroll: true });
      input.setSelectionRange(input.value.length, input.value.length);
    });
  });

  document.querySelectorAll("[data-account-select-visibility]").forEach((field) => {
    const select = field.querySelector("select");
    const display = field.querySelector("[data-account-display]");
    const button = field.querySelector("[data-account-select-toggle]");
    if (!select || !display || !button) return;

    const sync = (keepVisible = false) => {
      const option = select.options[select.selectedIndex];
      const full = option?.dataset.fullNumber;
      const masked = option?.dataset.maskedNumber;
      const visible = keepVisible && button.getAttribute("aria-pressed") === "true";
      button.disabled = !full;
      display.textContent = full ? (visible ? full : masked) : "Select an account to view its number";
      if (!full || !keepVisible) {
        button.setAttribute("aria-pressed", "false");
        button.setAttribute("aria-label", "Show selected account number");
        button.title = "Show account number";
      }
    };

    select.addEventListener("change", () => sync(false));
    button.addEventListener("click", () => {
      if (button.disabled) return;
      const visible = button.getAttribute("aria-pressed") === "true";
      button.setAttribute("aria-pressed", String(!visible));
      button.setAttribute("aria-label", visible ? "Show selected account number" : "Hide selected account number");
      button.title = visible ? "Show account number" : "Hide account number";
      sync(true);
    });
    sync(false);
  });
});
