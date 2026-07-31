'use strict';

const loginForm = document.getElementById('loginForm');
const loginInput = document.getElementById('username');
const passwordInput = document.getElementById('password');
const passwordToggle = document.getElementById('pwToggle');
const loginToast = document.getElementById('loginToast');

if (passwordToggle && passwordInput) {
	passwordToggle.addEventListener('click', () => {
		const showingPassword = passwordInput.type === 'text';
		passwordInput.type = showingPassword ? 'password' : 'text';
		passwordToggle.setAttribute('aria-label', showingPassword ? 'Show password' : 'Hide password');
		passwordToggle.textContent = showingPassword ? 'Show' : 'Hide';
	});
}

if (loginForm) {
	loginForm.addEventListener('submit', (event) => {
		const missingLogin = !loginInput.value.trim();
		const missingPassword = !passwordInput.value;
		loginInput.classList.toggle('err', missingLogin);
		passwordInput.classList.toggle('err', missingPassword);
		if (missingLogin || missingPassword) {
			event.preventDefault();
			loginToast.className = 'toast show';
			loginToast.textContent = 'Enter your username or email and password to continue.';
		}
	});
}
