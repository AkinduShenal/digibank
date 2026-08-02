'use strict';

const formatCardNumber = value => value.replace(/\D/g, '').replace(/(.{4})/g, '$1 ').trim();

document.querySelectorAll('[data-card-reveal]').forEach(button => {
  let hideTimer;
  button.addEventListener('click', async event => {
    event.preventDefault();
    event.stopPropagation();
    const card = button.closest('.payment-card-tile');
    const number = card?.querySelector('[data-card-number-text]');
    if (!number) return;

    if (button.dataset.visible === 'true') {
      clearTimeout(hideTimer);
      number.textContent = number.dataset.maskedNumber;
      button.dataset.visible = 'false';
      button.classList.remove('visible');
      button.setAttribute('aria-label', 'Show full card number');
      return;
    }

    button.disabled = true;
    button.classList.add('loading');
    try {
      const body = new URLSearchParams();
      body.set(button.dataset.csrfName, button.dataset.csrfToken);
      const response = await fetch(button.dataset.revealUrl, {
        method: 'POST',
        credentials: 'same-origin',
        headers: {'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'},
        body
      });
      const payload = await response.json();
      if (!response.ok) throw new Error(payload.message || 'Card number could not be displayed.');
      number.textContent = formatCardNumber(payload.cardNumber);
      button.dataset.visible = 'true';
      button.classList.add('visible');
      button.setAttribute('aria-label', 'Hide full card number');
      clearTimeout(hideTimer);
      hideTimer = setTimeout(() => {
        number.textContent = number.dataset.maskedNumber;
        button.dataset.visible = 'false';
        button.classList.remove('visible');
        button.setAttribute('aria-label', 'Show full card number');
      }, 10000);
    } catch (error) {
      window.alert(error.message);
    } finally {
      button.disabled = false;
      button.classList.remove('loading');
    }
  });
});

const accountSelect = document.querySelector('#accountNumber');
const previewDigits = document.querySelector('#previewAccountDigits');
const previewType = document.querySelector('#previewCardType');

const updateCardPreview = () => {
  if (accountSelect && previewDigits) {
    const digits = accountSelect.value.replace(/\D/g, '');
    previewDigits.textContent = digits.length >= 4 ? digits.slice(-4) : '0000';
  }
  if (previewType) {
    const selected = document.querySelector('.card-type-radio:checked');
    previewType.textContent = selected ? `${selected.value} CARD` : 'YOUR CARD';
  }
};

accountSelect?.addEventListener('change', updateCardPreview);
document.querySelectorAll('.card-type-radio').forEach(input => input.addEventListener('change', updateCardPreview));
updateCardPreview();
