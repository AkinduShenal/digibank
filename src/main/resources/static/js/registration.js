'use strict';

const totalSteps = 5;
let currentStep = 1;

const stepsRow = document.getElementById('stepsRow');
const progressFill = document.getElementById('progressFill');
const toast = document.getElementById('toast');
const form = document.getElementById('regForm');
const dobInput = document.getElementById('dateOfBirth');
const accountTypeInput = document.getElementById('accountType');
const initialDepositInput = document.getElementById('initialDeposit');
const depositHint = document.getElementById('depositHint');
const passwordInput = document.getElementById('password');
const strengthFill = document.getElementById('strengthFill');
const strengthLabel = document.getElementById('strengthLabel');

function latestAdultDate() {
	const today = new Date();
	return new Date(today.getFullYear() - 18, today.getMonth(), today.getDate());
}

function toDateInputValue(date) {
	const year = date.getFullYear();
	const month = String(date.getMonth() + 1).padStart(2, '0');
	const day = String(date.getDate()).padStart(2, '0');
	return `${year}-${month}-${day}`;
}

function updateProgress() {
	progressFill.style.width = `${((currentStep - 1) / (totalSteps - 1)) * 100}%`;
	stepsRow.querySelectorAll('.step').forEach((stepEl) => {
		const n = Number.parseInt(stepEl.dataset.step, 10);
		stepEl.classList.remove('active', 'done');
		if (n < currentStep) {
			stepEl.classList.add('done');
		} else if (n === currentStep) {
			stepEl.classList.add('active');
		}
	});
}

function goToStep(n, keepToast = false) {
	document.querySelectorAll('.step-panel').forEach((panel) => {
		panel.hidden = Number.parseInt(panel.dataset.panel, 10) !== n;
	});
	currentStep = n;
	updateProgress();
	if (!keepToast) {
		toast.className = 'toast';
	}
	window.scrollTo({ top: document.querySelector('.panel').offsetTop - 20, behavior: 'smooth' });
}

function openFirstErrorStep() {
	const firstPanelWithError = document.querySelector('.step-panel[data-has-error="true"]');
	if (!firstPanelWithError) {
		return;
	}
	document.querySelectorAll('.step-panel').forEach((panel) => {
		panel.hidden = panel !== firstPanelWithError;
	});
	currentStep = Number.parseInt(firstPanelWithError.dataset.panel, 10);
	updateProgress();
	const firstErrorField = firstPanelWithError.querySelector('.err');
	if (firstErrorField) {
		firstErrorField.focus({ preventScroll: true });
	}
}

function showToast(message) {
	toast.className = 'toast show';
	toast.textContent = message;
}

function fieldValue(id) {
	const field = document.getElementById(id);
	return field ? field.value.trim() : '';
}

function isAdultDate(value) {
	if (!value) {
		return false;
	}
	const selected = new Date(`${value}T00:00:00`);
	return selected <= latestAdultDate();
}

function passwordScore(password) {
	let score = 0;
	if (password.length >= 8 && password.length <= 72) score++;
	if (/[A-Z]/.test(password)) score++;
	if (/[a-z]/.test(password)) score++;
	if (/\d/.test(password)) score++;
	if (/[^A-Za-z0-9]/.test(password)) score++;
	return score;
}

function updatePasswordStrength() {
	if (!passwordInput || !strengthFill || !strengthLabel) {
		return;
	}
	const score = passwordScore(passwordInput.value);
	const widths = ['0%', '20%', '40%', '60%', '80%', '100%'];
	const labels = ['Enter a password', 'Very weak', 'Weak', 'Fair', 'Good', 'Strong'];
	const colors = ['#d64545', '#d64545', '#d98428', '#d9b428', '#4d9a60', '#1e8e5a'];
	strengthFill.style.width = widths[score];
	strengthFill.style.background = colors[score];
	strengthLabel.textContent = labels[score];
}

function updateDepositHint() {
	if (!accountTypeInput || !depositHint) {
		return;
	}
	if (accountTypeInput.value === 'CURRENT') {
		depositHint.textContent = 'Current accounts require an initial deposit of at least LKR 5,000.';
		initialDepositInput.min = '5000';
		return;
	}
	depositHint.textContent = 'Savings accounts require an initial deposit of at least LKR 1,000.';
	initialDepositInput.min = '1000';
}

function validateStep(n) {
	if (n === 1) {
		if (!fieldValue('firstName') || !fieldValue('lastName') || !fieldValue('email') || !fieldValue('mobileNumber')) {
			showToast('Please complete your personal details before continuing.');
			return false;
		}
		if (!fieldValue('dateOfBirth')) {
			showToast('Please enter your date of birth before continuing.');
			return false;
		}
		if (!isAdultDate(fieldValue('dateOfBirth'))) {
			showToast('You must be at least 18 years old to open an account.');
			return false;
		}
		if (!fieldValue('gender')) {
			showToast('Please select your gender before continuing.');
			return false;
		}
		return true;
	}
	if (n === 2) {
		if (!fieldValue('addressLine1') || !fieldValue('city') || !fieldValue('country')) {
			showToast('Please complete the required address details.');
			return false;
		}
		return true;
	}
	if (n === 3) {
		if (!fieldValue('identityType') || !fieldValue('identityNumber') || !fieldValue('accountType')
				|| !fieldValue('branchCode') || !fieldValue('initialDeposit')) {
			showToast('Please complete the account and identity details.');
			return false;
		}
		const minimum = accountTypeInput.value === 'CURRENT' ? 5000 : 1000;
		if (Number.parseFloat(initialDepositInput.value) < minimum) {
			showToast(accountTypeInput.value === 'CURRENT'
				? 'Current accounts require at least LKR 5,000.'
				: 'Savings accounts require at least LKR 1,000.');
			return false;
		}
		return true;
	}
	if (n === 4) {
		return true;
	}
	if (n === 5) {
		if (!fieldValue('username') || !fieldValue('password') || !fieldValue('confirmPassword')) {
			showToast('Please complete your login details.');
			return false;
		}
		if (passwordInput.value !== document.getElementById('confirmPassword').value) {
			showToast('Passwords do not match.');
			return false;
		}
		if (!/^\d{4}$/.test(fieldValue('transactionPin')) || fieldValue('transactionPin') !== fieldValue('confirmTransactionPin')) {
			showToast('Please enter matching 4-digit transaction PINs.');
			return false;
		}
		if (!document.getElementById('termsAccepted').checked || !document.getElementById('privacyAccepted').checked) {
			showToast('Please accept the terms and privacy policy.');
			return false;
		}
		return true;
	}
	return true;
}

document.querySelectorAll('[data-next]').forEach((btn) => {
	btn.addEventListener('click', () => {
		if (validateStep(currentStep)) {
			goToStep(Number.parseInt(btn.dataset.next, 10));
		}
	});
});

document.querySelectorAll('[data-prev]').forEach((btn) => {
	btn.addEventListener('click', () => goToStep(Number.parseInt(btn.dataset.prev, 10)));
});

if (dobInput) {
	dobInput.max = toDateInputValue(latestAdultDate());
}

if (accountTypeInput) {
	accountTypeInput.addEventListener('change', updateDepositHint);
	updateDepositHint();
}

if (passwordInput) {
	passwordInput.addEventListener('input', updatePasswordStrength);
	updatePasswordStrength();
}

document.querySelectorAll('[data-password-toggle]').forEach((button) => {
	button.addEventListener('click', () => {
		const input = document.getElementById(button.dataset.passwordToggle);
		if (!input) {
			return;
		}
		const showValue = input.type === 'password';
		input.type = showValue ? 'text' : 'password';
		button.setAttribute('aria-pressed', String(showValue));
		button.setAttribute('aria-label', `${showValue ? 'Hide' : 'Show'} ${input.id.toLowerCase().includes('pin') ? 'transaction PIN' : 'password'}`);
	});
});

if (form) {
	form.addEventListener('submit', (event) => {
		for (let step = 1; step <= totalSteps; step++) {
			if (!validateStep(step)) {
				event.preventDefault();
				goToStep(step, true);
				return;
			}
		}
	});
}

updateProgress();
openFirstErrorStep();
