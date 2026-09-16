'use strict';

document.querySelectorAll('[data-account-number]').forEach(account => {
  const number = account.querySelector('[data-account-number-text]');
  const button = account.querySelector('[data-account-number-toggle]');

  function setVisible(visible) {
    number.textContent = visible ? account.dataset.number : account.dataset.maskedNumber;
    button.setAttribute('aria-pressed', String(visible));
    const label = visible ? 'Hide full account number' : 'Show full account number';
    button.setAttribute('aria-label', label);
    button.title = label;
  }

  button.addEventListener('click', () => {
    setVisible(button.getAttribute('aria-pressed') !== 'true');
  });

  // Restore masking when leaving the page, including browser back/forward navigation.
  window.addEventListener('pagehide', () => setVisible(false));
  window.addEventListener('pageshow', () => setVisible(false));
  document.addEventListener('visibilitychange', () => {
    if (document.hidden) setVisible(false);
  });
});
