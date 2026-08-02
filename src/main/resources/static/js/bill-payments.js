document.addEventListener('DOMContentLoaded', () => {
  const typeInputs = document.querySelectorAll('input[name="selectionType"]');
  const savedFields = document.getElementById('savedBillerFields');
  const newFields = document.getElementById('newBillerFields');
  const saveToggle = document.getElementById('saveBiller');
  const nicknameField = document.getElementById('nicknameField');
  const provider = document.getElementById('provider');
  const referenceLabel = document.getElementById('referenceLabel');
  const categories = document.querySelectorAll('[data-category]');

  const updateType = () => {
    const selected = document.querySelector('input[name="selectionType"]:checked')?.value;
    if (savedFields) savedFields.hidden = selected !== 'SAVED_BILLER';
    if (newFields) newFields.hidden = selected === 'SAVED_BILLER';
  };
  const updateSave = () => { if (nicknameField) nicknameField.hidden = !saveToggle?.checked; };
  const updateReferenceLabel = () => {
    const option = provider?.selectedOptions[0];
    if (referenceLabel) referenceLabel.textContent = option?.dataset.referenceLabel || 'Consumer / service reference';
  };
  typeInputs.forEach(input => input.addEventListener('change', updateType));
  saveToggle?.addEventListener('change', updateSave);
  provider?.addEventListener('change', updateReferenceLabel);
  categories.forEach(button => button.addEventListener('click', () => {
    categories.forEach(item => item.classList.remove('active'));
    button.classList.add('active');
    const category = button.dataset.category;
    Array.from(provider.options).forEach((option, index) => { option.hidden = index > 0 && option.dataset.category !== category; });
    if (provider.selectedOptions[0]?.hidden) provider.value = '';
    updateReferenceLabel();
  }));
  updateType(); updateSave(); updateReferenceLabel();
});
