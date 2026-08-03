document.addEventListener("DOMContentLoaded", () => {
  const links = [...document.querySelectorAll(".customer-sidebar a, .staff-sidebar a")];
  const path = window.location.pathname.replace(/\/$/, "") || "/";

  const matches = links
    .map((link) => ({ link, path: new URL(link.href, window.location.origin).pathname.replace(/\/$/, "") }))
    .filter((entry) => path === entry.path || path.startsWith(`${entry.path}/`))
    .sort((left, right) => right.path.length - left.path.length);

  if (matches.length > 0) {
    links.forEach((link) => {
      link.classList.remove("active");
      link.removeAttribute("aria-current");
    });
    matches[0].link.classList.add("active");
    matches[0].link.setAttribute("aria-current", "page");
  }

  enhanceSelects();
});

function enhanceSelects() {
  const selects = document.querySelectorAll(
    ".customer-shell select:not([multiple]):not([size]):not([data-native-select]), " +
    ".staff-shell select:not([multiple]):not([size]):not([data-native-select])"
  );

  let openSelect = null;

  const closeSelect = (component, restoreFocus = false) => {
    if (!component) return;
    component.classList.remove("is-open");
    component.trigger.setAttribute("aria-expanded", "false");
    component.menu.hidden = true;
    if (restoreFocus) component.trigger.focus();
    if (openSelect === component) openSelect = null;
  };

  const visibleOptions = (component) => component.optionButtons.filter((option) => !option.hidden && !option.disabled);

  const focusOption = (component, direction) => {
    const options = visibleOptions(component);
    if (!options.length) return;
    const current = options.indexOf(document.activeElement);
    const selected = options.findIndex((option) => option.dataset.index === String(component.select.selectedIndex));
    let next = current;

    if (direction === "first") next = 0;
    else if (direction === "last") next = options.length - 1;
    else if (current < 0) next = selected >= 0 ? selected : 0;
    else next = (current + direction + options.length) % options.length;

    options[next].focus();
  };

  const openMenu = (component, focusSelected = false) => {
    if (component.select.disabled) return;
    if (openSelect && openSelect !== component) closeSelect(openSelect);
    component.menu.hidden = false;
    const bounds = component.getBoundingClientRect();
    const menuHeight = Math.min(component.menu.scrollHeight, 320);
    component.classList.toggle("opens-up", window.innerHeight - bounds.bottom < menuHeight + 18 && bounds.top > menuHeight + 18);
    component.classList.add("is-open");
    component.trigger.setAttribute("aria-expanded", "true");
    openSelect = component;
    if (focusSelected) requestAnimationFrame(() => focusOption(component, 0));
  };

  selects.forEach((select, selectNumber) => {
    if (select.dataset.enhancedSelect === "true") return;
    select.dataset.enhancedSelect = "true";

    const component = document.createElement("div");
    component.className = "db-select";
    const trigger = document.createElement("button");
    trigger.type = "button";
    trigger.className = "db-select-trigger";
    trigger.setAttribute("aria-haspopup", "listbox");
    trigger.setAttribute("aria-expanded", "false");
    const value = document.createElement("span");
    value.className = "db-select-value";
    const chevron = document.createElement("span");
    chevron.className = "db-select-chevron";
    chevron.setAttribute("aria-hidden", "true");
    trigger.append(value, chevron);

    const menu = document.createElement("div");
    const menuId = `db-select-menu-${selectNumber}`;
    menu.id = menuId;
    menu.className = "db-select-menu";
    menu.setAttribute("role", "listbox");
    menu.hidden = true;
    trigger.setAttribute("aria-controls", menuId);

    select.parentNode.insertBefore(component, select);
    component.append(select, trigger, menu);
    select.classList.add("db-select-native");

    Object.assign(component, { select, trigger, value, menu, optionButtons: [] });

    const sync = () => {
      const selected = select.options[select.selectedIndex];
      value.textContent = selected?.textContent?.trim() || "Select an option";
      trigger.disabled = select.disabled;
      trigger.classList.toggle("is-placeholder", !select.value);
      component.optionButtons.forEach((option) => {
        const active = option.dataset.index === String(select.selectedIndex);
        option.classList.toggle("is-selected", active);
        option.setAttribute("aria-selected", String(active));
      });
    };

    const choose = (index) => {
      const option = select.options[index];
      if (!option || option.disabled || option.hidden) return;
      select.selectedIndex = index;
      select.dispatchEvent(new Event("change", { bubbles: true }));
      sync();
      closeSelect(component, true);
    };

    const rebuild = () => {
      menu.replaceChildren();
      component.optionButtons = [];

      [...select.children].forEach((child) => {
        if (child.tagName === "OPTGROUP") {
          const heading = document.createElement("div");
          heading.className = "db-select-group";
          heading.textContent = child.label;
          menu.append(heading);
        }

        const options = child.tagName === "OPTGROUP" ? [...child.children] : [child];
        options.filter((option) => option.tagName === "OPTION").forEach((option) => {
          const index = [...select.options].indexOf(option);
          const item = document.createElement("button");
          item.type = "button";
          item.className = "db-select-option";
          item.dataset.index = String(index);
          item.setAttribute("role", "option");
          item.disabled = option.disabled;
          item.hidden = option.hidden;

          const label = document.createElement("span");
          label.textContent = option.textContent.trim();
          const check = document.createElement("span");
          check.className = "db-select-check";
          check.textContent = "✓";
          check.setAttribute("aria-hidden", "true");
          item.append(label, check);
          item.addEventListener("click", () => choose(index));
          item.addEventListener("keydown", (event) => {
            if (event.key === "ArrowDown" || event.key === "ArrowUp") {
              event.preventDefault();
              focusOption(component, event.key === "ArrowDown" ? 1 : -1);
            } else if (event.key === "Home" || event.key === "End") {
              event.preventDefault();
              focusOption(component, event.key === "Home" ? "first" : "last");
            } else if (event.key === "Escape") {
              event.preventDefault();
              closeSelect(component, true);
            }
          });
          menu.append(item);
          component.optionButtons.push(item);
        });
      });
      sync();
    };

    trigger.addEventListener("click", () => {
      component.classList.contains("is-open") ? closeSelect(component) : openMenu(component);
    });
    trigger.addEventListener("keydown", (event) => {
      if (["ArrowDown", "ArrowUp", "Home", "End"].includes(event.key)) {
        event.preventDefault();
        openMenu(component, false);
        requestAnimationFrame(() => focusOption(
          component,
          event.key === "Home" ? "first" : event.key === "End" ? "last" : event.key === "ArrowUp" ? -1 : 1
        ));
      } else if (event.key === "Escape") {
        closeSelect(component);
      }
    });
    select.addEventListener("focus", () => trigger.focus());
    select.addEventListener("click", (event) => {
      event.preventDefault();
      trigger.focus();
      openMenu(component);
    });
    select.addEventListener("change", sync);
    select.addEventListener("invalid", (event) => {
      event.preventDefault();
      component.classList.add("is-invalid");
      trigger.focus();
    });
    select.addEventListener("change", () => component.classList.remove("is-invalid"));
    new MutationObserver(rebuild).observe(select, {
      childList: true,
      subtree: true,
      attributes: true,
      attributeFilter: ["disabled", "hidden", "label"]
    });
    select.form?.addEventListener("reset", () => requestAnimationFrame(sync));
    rebuild();
  });

  document.addEventListener("pointerdown", (event) => {
    if (openSelect && !openSelect.contains(event.target)) closeSelect(openSelect);
  });
  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape" && openSelect) closeSelect(openSelect, true);
  });
}
