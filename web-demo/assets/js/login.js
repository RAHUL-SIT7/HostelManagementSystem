'use strict';

const loginForm = document.querySelector('#login-form');
const emailInput = document.querySelector('#email');
const passwordInput = document.querySelector('#password');
const togglePasswordButton = document.querySelector('#toggle-password');
const message = document.querySelector('#form-message');

// Show/hide the password. This is a visual convenience, not authentication.
togglePasswordButton.addEventListener('click', () => {
  const showPassword = passwordInput.type === 'password';
  passwordInput.type = showPassword ? 'text' : 'password';
  togglePasswordButton.textContent = showPassword ? 'Hide' : 'Show';
  togglePasswordButton.setAttribute('aria-pressed', String(showPassword));
  togglePasswordButton.setAttribute(
    'aria-label',
    showPassword ? 'Hide password' : 'Show password'
  );
});

// Front-end demo validation only. No password is stored or sent to a server.
loginForm.addEventListener('submit', (event) => {
  event.preventDefault();
  message.classList.remove('error');

  const email = emailInput.value.trim();
  const password = passwordInput.value;

  if (!emailInput.validity.valid || email.length === 0) {
    message.textContent = 'Please enter a valid email address.';
    message.classList.add('error');
    emailInput.focus();
    return;
  }

  if (password.trim().length < 6) {
    message.textContent = 'For this demo, enter at least 6 characters in the password field.';
    message.classList.add('error');
    passwordInput.focus();
    return;
  }

  const role = document.querySelector('#role').value;
  message.textContent = `Front-end demo only: ${role.toLowerCase()} form validated. Real sign-in is not connected.`;
});
