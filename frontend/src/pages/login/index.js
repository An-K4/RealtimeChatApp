import authService from '../../services/auth.service.js';
import { config } from '../../config/index.js';

// DOM Elements
const loginForm = document.getElementById('loginForm');
const signupForm = document.getElementById('signupForm');
const tabButtons = document.querySelectorAll('.tab-btn');
const loginFormDiv = document.getElementById('login-form');
const signupFormDiv = document.getElementById('signup-form');
const loginError = document.getElementById('login-error');
const signupError = document.getElementById('signup-error');
const signupSuccess = document.getElementById('signup-success');
const googleLoginBtn = document.getElementById('google-login-btn');
const googleSignupBtn = document.getElementById('google-signup-btn');
const switchToSignup = document.getElementById('switch-to-signup');
const switchToLogin = document.getElementById('switch-to-login');

function startGoogleOAuth(buttonEl) {
  if (buttonEl) {
    buttonEl.disabled = true;
    buttonEl.dataset.originalText = buttonEl.textContent;
    buttonEl.textContent = 'Đang chuyển tới Google...';
  }
  // Must be a top-level navigation (not fetch) to show Google's auth screen
  window.location.href = `${config.apiBaseUrl}/auth/google`;
}

googleLoginBtn?.addEventListener('click', (e) => {
  e.preventDefault();
  startGoogleOAuth(googleLoginBtn);
});

googleSignupBtn?.addEventListener('click', (e) => {
  e.preventDefault();
  startGoogleOAuth(googleSignupBtn);
});

function activateTab(tabName) {
  document.querySelector(`.tab-btn[data-tab="${tabName}"]`)?.click();
}

switchToSignup?.addEventListener('click', (e) => {
  e.preventDefault();
  activateTab('signup');
});

switchToLogin?.addEventListener('click', (e) => {
  e.preventDefault();
  activateTab('login');
});

// Tab switching
tabButtons.forEach((btn) => {
  btn.addEventListener('click', () => {
    const tab = btn.dataset.tab;

    tabButtons.forEach((b) => b.classList.remove('active'));
    btn.classList.add('active');
    const errorMessageLoginForm = loginFormDiv.querySelector('.error-message');
    const errorMessageSignupForm = signupFormDiv.querySelector('.error-message');

    if (tab === 'login') {
      loginFormDiv.classList.add('active');
      signupFormDiv.classList.remove('active');
      errorMessageLoginForm.classList.add('hidden');
      loginError.textContent = '';
      signupError.textContent = '';
      signupSuccess.textContent = '';
    } else {
      loginFormDiv.classList.remove('active');
      signupFormDiv.classList.add('active');
      errorMessageSignupForm.classList.add('hidden');
      loginError.textContent = '';
      signupError.textContent = '';
      signupSuccess.textContent = '';
    }
  });
});

// Login form handler
loginForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  loginError.textContent = '';

  const username = document.getElementById('login-username').value;
  const password = document.getElementById('login-password').value;

  const submitBtn = loginForm.querySelector('button[type="submit"]');
  submitBtn.disabled = true;
  submitBtn.textContent = 'Đang đăng nhập...';

  const result = await authService.login(username, password);

  submitBtn.disabled = false;
  submitBtn.textContent = 'Đăng nhập';

  if (result.success) {
    // Redirect to chat page
    window.location.href = '/chat.html';
  } else {
    loginError.classList.remove('hidden');
    loginError.textContent = result.message;
  }
});

// Signup form handler
signupForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  signupError.textContent = '';
  signupSuccess.textContent = '';

  const username = document.getElementById('signup-username').value;
  const password = document.getElementById('signup-password').value;
  const fullName = document.getElementById('signup-fullname').value;
  const email = document.getElementById('signup-email').value;

  const submitBtn = signupForm.querySelector('button[type="submit"]');
  submitBtn.disabled = true;
  submitBtn.textContent = 'Đang đăng ký...';

  const result = await authService.signup(username, password, fullName, email);

  submitBtn.disabled = false;
  submitBtn.textContent = 'Đăng ký';

  if (result.success) {
    signupSuccess.textContent = result.message;
    signupForm.reset();

    // Chuyển sang tab login sau 1 giây
    setTimeout(() => {
      document.querySelector('.tab-btn[data-tab="login"]').click();
      document.getElementById('login-username').value = username;
    }, 1000);
  } else {
    signupError.classList.remove('hidden');
    signupError.textContent = result.message;
  }
});

// Check authentication on page load
async function init() {
  const user = await authService.checkAuth();
  if (user) {
    // If already logged in, redirect to chat page
    window.location.href = '/chat.html';
  }
}

// Initialize
init();