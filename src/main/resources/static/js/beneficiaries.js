(function () {
  const typeSelect = document.querySelector('[data-beneficiary-type]');
  const externalFields = document.querySelectorAll('.external-fields, .external-help');
  const internalHelp = document.querySelector('.internal-help');

  function updateTypeMode() {
    const internal = typeSelect && typeSelect.value === 'INTERNAL';
    externalFields.forEach((element) => {
      element.hidden = internal;
    });
    if (internalHelp) {
      internalHelp.hidden = !internal;
    }
  }

  if (typeSelect) {
    typeSelect.addEventListener('change', updateTypeMode);
    updateTypeMode();
  }

  document.querySelectorAll('form[data-confirm]').forEach((form) => {
    form.addEventListener('submit', (event) => {
      if (!window.confirm(form.dataset.confirm)) {
        event.preventDefault();
      }
    });
  });
})();
