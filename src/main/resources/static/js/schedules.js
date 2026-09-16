'use strict';

document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('[data-schedule-form]').forEach(form => {
    const choice = form.querySelector('[data-schedule-choice]');
    const recurrence = form.querySelector('[name="recurrence"]');
    const endGroup = form.querySelector('[data-monthly-end]');
    const nextDate = form.querySelector('[name="nextExecutionAt"]');

    function showFields(group, visible) {
      group.hidden = !visible;
      group.querySelectorAll('input, select').forEach(input => {
        input.disabled = !visible;
        if (input.hasAttribute('data-required')) input.required = visible;
      });
    }

    function syncChoice() {
      form.querySelectorAll('[data-choice]').forEach(group => showFields(group, group.dataset.choice === choice.value));
    }

    function syncRecurrence() {
      if (endGroup) showFields(endGroup, recurrence.value === 'MONTHLY');
    }

    function syncEndDate() {
      if (endGroup && nextDate.value) endGroup.querySelector('input').min = nextDate.value.slice(0, 10);
    }

    if (choice) {
      choice.addEventListener('change', syncChoice);
      syncChoice();
    }
    recurrence?.addEventListener('change', syncRecurrence);
    syncRecurrence();
    nextDate?.addEventListener('change', syncEndDate);
    syncEndDate();

    // Match the server's local banking time, even when the browser is elsewhere.
    if (nextDate) {
      const parts = Object.fromEntries(new Intl.DateTimeFormat('en-GB', {
        timeZone: 'Asia/Colombo', year: 'numeric', month: '2-digit', day: '2-digit',
        hour: '2-digit', minute: '2-digit', hourCycle: 'h23'
      }).formatToParts(new Date(Date.now() + 60000)).map(part => [part.type, part.value]));
      nextDate.min = `${parts.year}-${parts.month}-${parts.day}T${parts.hour}:${parts.minute}`;
    }

    const provider = form.querySelector('[name="billerProvider"]');
    const referenceLabel = form.querySelector('[data-consumer-label]');
    function syncReferenceLabel() {
      referenceLabel.textContent = provider.selectedOptions[0]?.dataset.referenceLabel || 'Consumer / service reference';
    }
    if (provider && referenceLabel) {
      provider.addEventListener('change', syncReferenceLabel);
      syncReferenceLabel();
    }
  });

  document.querySelectorAll('[data-cancel-schedule]').forEach(form => {
    form.addEventListener('submit', event => {
      if (!window.confirm('Cancel this schedule? Future payments will stop. Completed payments will remain in your history.')) {
        event.preventDefault();
      }
    });
  });
});
