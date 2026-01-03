import authService from './auth.js';
import chatService from './chat.js';
import { userAPI } from './api.js';
import { io } from 'socket.io-client';
import { config } from './config.js';
// DOM Elements
const logoutBtn = document.getElementById('logout-btn');
const usersList = document.getElementById('users-list');
const noChatSelected = document.getElementById('no-chat-selected');
const chatMessagesContainer = document.getElementById('chat-messages-container');
const chatWithName = document.getElementById('chat-with-name');
const chatNameText = chatWithName?.querySelector('.chat-name-text');
const chatOnlineIndicator = document.getElementById('chat-online-indicator');
const messagesList = document.getElementById('messages-list');
const messageInput = document.getElementById('message-input');
const sendBtn = document.getElementById('send-btn');
const profileName = document.getElementById('profile-name');
const profileEmail = document.getElementById('profile-email');
const profileAvatar = document.getElementById('profile-avatar');
const profileCard = document.getElementById('profile-card');
const profileModal = document.getElementById('profile-modal');
const profileModalOverlay = document.getElementById('profile-modal-overlay');
const profileModalClose = document.getElementById('profile-modal-close');
const profileModalAvatar = document.getElementById('profile-modal-avatar');
const profileModalName = document.getElementById('profile-modal-name');
const profileModalUsername = document.getElementById('profile-modal-username');
const profileModalEmail = document.getElementById('profile-modal-email');
const profileModalFullName = document.getElementById('profile-modal-fullname');
const profileModalUsernameDetail = document.getElementById('profile-modal-username-detail');
const profileModalEmailDetail = document.getElementById('profile-modal-email-detail');
const profileEditForm = document.getElementById('profile-edit-form');
const profileEditFullName = document.getElementById('profile-edit-fullname');
const profileEditUsername = document.getElementById('profile-edit-username');
const profileEditEmail = document.getElementById('profile-edit-email');
const profileSaveBtn = document.getElementById('profile-save-btn');
const profileUpdateMessage = document.getElementById('profile-update-message');
const avatarUploadBtn = document.getElementById('avatar-upload-btn');
const avatarFileInput = document.getElementById('avatar-file-input');
const avatarUploadStatus = document.getElementById('avatar-upload-status');
const typingIndicator = document.getElementById('typing-indicator');
let currentProfile = null;
// Map to store online status of users: userId -> boolean
const onlineUsers = new Map();
// Typing timeout reference
let typingTimeout = null;
const TYPING_TIMEOUT = 2000; // 2 giây

if (profileEditForm) {
  setProfileFormDisabled(true);
  profileEditForm.addEventListener('submit', handleProfileUpdate);
}

if (avatarUploadBtn && avatarFileInput) {
  avatarUploadBtn.addEventListener('click', () => {
    avatarFileInput.click();
  });
  avatarFileInput.addEventListener('change', handleAvatarFileChange);
}

if (profileCard) {
  profileCard.addEventListener('click', (event) => {
    event.preventDefault();
    openProfileModal();
  });
}

profileModalOverlay?.addEventListener('click', () => closeProfileModal());
profileModalClose?.addEventListener('click', () => closeProfileModal());

document.addEventListener('keydown', (event) => {
  if (event.key === 'Escape' && profileModal?.classList.contains('show')) {
    closeProfileModal();
  }
});

// Logout handler
logoutBtn.addEventListener('click', async () => {
  await authService.logout();
  window.location.href = '/index.html';
});

// Load users list
async function loadUsers() {
  usersList.innerHTML = '<div class="loading">Đang tải...</div>';
  
  const result = await chatService.loadUsers();
  
  if (result.success) {
    renderUsers(result.users);
  } else {
    usersList.innerHTML = `<div class="error-message">${result.message}</div>`;
  }
}

// Render users list
function renderUsers(users) {
  if (users.length === 0) {
    usersList.innerHTML = '<div class="loading">Không có người dùng nào</div>';
    return;
  }
  
  usersList.innerHTML = users.map(user => {
    const displayName = user.fullName || user.username || 'Người dùng';
    const username = user.username || '';
    const initials = displayName
      .split(' ')
      .filter(Boolean)
      .map(part => part[0])
      .join('')
      .slice(0, 2)
      .toUpperCase() || 'U';
    const avatarUrl = user.avatar || '';
    
    const isOnline = onlineUsers.get(user._id.toString()) || false;
    const avatarClass = avatarUrl 
      ? 'user-item-avatar has-image' 
      : 'user-item-avatar';
    const avatarStyle = avatarUrl ? `style="background-image: url('${avatarUrl}');"` : '';
    const itemClass = isOnline ? 'user-item online' : 'user-item';

    return `
      <div class="${itemClass}" data-user-id="${user._id}" data-user-name="${displayName}">
        <div class="${avatarClass}" ${avatarStyle}>
          ${avatarUrl ? '' : initials}
        </div>
        <div class="user-item-meta">
          <h4>
            ${displayName}
            <span class="user-item-online-dot"></span>
          </h4>
          <p>${username ? '@' + username : ''}</p>
        </div>
      </div>
    `;
  }).join('');
  
  // Add click event to user items
  document.querySelectorAll('.user-item').forEach(item => {
    item.addEventListener('click', () => {
      const userId = item.dataset.userId;
      const userName = item.dataset.userName;
      
      // Remove active class from all items
      document.querySelectorAll('.user-item').forEach(i => i.classList.remove('active'));
      // Add active class to clicked item
      item.classList.add('active');
      
      selectUser(userId, userName);
    });
  });
}

// Select user and load messages
async function selectUser(userId, userName) {
  chatService.selectUser(userId, userName);
  
  noChatSelected.style.display = 'none';
  chatMessagesContainer.style.display = 'flex';
  
  // Ensure video call button is visible
  const videoCallBtn = document.getElementById("video-call-btn");
  if (videoCallBtn) {
    videoCallBtn.style.display = 'flex';
    videoCallBtn.style.visibility = 'visible';
    videoCallBtn.style.opacity = '1';
    console.log('Video call button found and made visible');
  } else {
    console.error('Video call button not found!');
  }
  
  // Update chat header with name and online status
  if (chatNameText) {
    chatNameText.textContent = userName;
  } else {
    chatWithName.textContent = userName;
  }
  
  // Update online indicator in header
  updateChatHeaderOnlineStatus(userId);
  
  messagesList.innerHTML = '<div class="loading">Đang tải tin nhắn...</div>';
  
  const result = await chatService.loadMessages(userId);
  
  if (result.success) {
    renderMessages(result.messages);
    
    // Emit seen-message event để thông báo đã xem tin nhắn
    if (result.messages.length > 0) {
      socket.emit('seen-message', {
        senderId: userId // người gửi tin (người mình đang chat)
      });
    }
  } else {
    messagesList.innerHTML = `<div class="error-message">${result.message}</div>`;
  }
  
  // Scroll to bottom
  messagesList.scrollTop = messagesList.scrollHeight;
}

// Scroll to the latest message
function scrollToLatestMessage() {
  if (!messagesList) return;
  messagesList.scrollTop = messagesList.scrollHeight;
}

function setProfileFormDisabled(isDisabled) {
  [profileEditFullName, profileEditUsername, profileEditEmail].forEach((input) => {
    if (input) {
      input.disabled = isDisabled;
    }
  });
  if (profileSaveBtn) {
    profileSaveBtn.disabled = isDisabled;
  }
}

function populateProfileForm(user) {
  if (!profileEditFullName || !profileEditUsername || !profileEditEmail) return;
  profileEditFullName.value = user?.fullName || '';
  profileEditUsername.value = user?.username || '';
  profileEditEmail.value = user?.email || '';
}

function showProfileUpdateMessage(message, type = '') {
  if (!profileUpdateMessage) return;
  profileUpdateMessage.textContent = message;
  if (!message) {
    profileUpdateMessage.removeAttribute('data-type');
  } else {
    profileUpdateMessage.dataset.type = type || 'info';
  }
}

function resetProfileUpdateMessage() {
  showProfileUpdateMessage('', '');
}

function showAvatarStatus(message, type = '') {
  if (!avatarUploadStatus) return;
  avatarUploadStatus.textContent = message;
  if (!message) {
    avatarUploadStatus.removeAttribute('data-type');
  } else {
    avatarUploadStatus.dataset.type = type || 'info';
  }
}

function resetAvatarStatus() {
  showAvatarStatus('', '');
}

function applyAvatarVisual(avatarUrl, fallbackInitials = '?') {
  const elements = [profileAvatar, profileModalAvatar];
  elements.forEach((el) => {
    if (!el) return;
    if (avatarUrl) {
      el.classList.add('has-image');
      el.style.backgroundImage = `url("${avatarUrl}")`;
      el.textContent = '';
    } else {
      el.classList.remove('has-image');
      el.style.backgroundImage = '';
      el.textContent = fallbackInitials;
    }
  });
}

function normalizeDate(value) {
  const date = value instanceof Date ? value : new Date(value);
  return Number.isNaN(date.getTime()) ? new Date() : date;
}

function formatShortTime(date) {
  return normalizeDate(date).toLocaleTimeString('vi-VN', {
    hour: '2-digit',
    minute: '2-digit'
  });
}

function formatFullTime(date) {
  return normalizeDate(date).toLocaleString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  });
}

function buildStatusTooltip(action, date) {
  return `${action} lúc ${formatFullTime(date)}`;
}

function applyStatusMetadata(statusDiv, labelText, action, timestamp) {
  const safeDate = normalizeDate(timestamp || statusDiv.dataset.baseTimestamp);
  statusDiv.dataset.baseTimestamp = safeDate.toISOString();
  statusDiv.dataset.action = action;
  statusDiv.title = buildStatusTooltip(action, safeDate);

  let labelEl = statusDiv.querySelector('.message-status-label');
  if (labelText) {
    if (!labelEl) {
      labelEl = document.createElement('span');
      labelEl.classList.add('message-status-label');
      statusDiv.appendChild(labelEl);
    }
    labelEl.textContent = labelText;
  } else if (labelEl) {
    labelEl.remove();
  }

  let timeEl = statusDiv.querySelector('.message-status-time');
  if (!timeEl) {
    timeEl = document.createElement('span');
    timeEl.classList.add('message-status-time');
    statusDiv.appendChild(timeEl);
  }
  const separator = labelText ? ' · ' : '';
  timeEl.textContent = `${separator}${formatShortTime(safeDate)}`;
}

function removeRetryButton(statusDiv) {
  const retryBtn = statusDiv.querySelector('.retry-btn');
  if (retryBtn) {
    retryBtn.remove();
  }
}

function buildStatusHtml(labelText, action, timestamp) {
  const safeDate = normalizeDate(timestamp);
  const tooltip = buildStatusTooltip(action, safeDate);
  const shortTime = formatShortTime(safeDate);
  const isoString = safeDate.toISOString();
  const hasLabel = Boolean(labelText);
  const separator = hasLabel ? ' · ' : '';
  const labelSpan = hasLabel 
    ? `<span class="message-status-label">${labelText}</span>`
    : '';

  return `
    <div class="message-status" data-base-timestamp="${isoString}" data-action="${action}" title="${tooltip}">
      ${labelSpan}
      <span class="message-status-time">${separator}${shortTime}</span>
    </div>
  `;
}

// Clear "Đã gửi" và "Đã xem" status from old messages, keep "Đang gửi..." và "Gửi thất bại"
function clearDeliveredStatuses() {
  const statusElements = messagesList?.querySelectorAll('.message-status');
  if (!statusElements) return;

  statusElements.forEach((statusEl) => {
    const labelText = statusEl.querySelector('.message-status-label')?.textContent?.trim();
    if (labelText === 'Đã gửi' || labelText === 'Đã xem') {
      statusEl.innerHTML = '';
      statusEl.removeAttribute('title');
      delete statusEl.dataset.baseTimestamp;
      delete statusEl.dataset.action;
    }
  });
}

// Render messages
function renderMessages(messages) {
  const currentUser = authService.getUser();
  const currentUserId = currentUser?._id;
  
  if (messages.length === 0) {
    messagesList.innerHTML = '<div class="loading">Chưa có tin nhắn nào</div>';
    return;
  }
  messagesList.innerHTML = messages.map(message => {
    // Handle both string and object senderId
    const senderId = typeof message.senderId === 'object' 
      ? (message.senderId._id || message.senderId.id || message.senderId.toString())
      : message.senderId.toString();
    
    const isSent = senderId === currentUserId?.toString() || senderId === currentUserId;
    const statusLabel = isSent ? 'Đã gửi' : '';
    const statusAction = isSent ? 'Gửi' : 'Nhận';
    
    return `
      <div class="message ${isSent ? 'sent' : 'received'}">
        <div class="message-content">${escapeHtml(message.content)}</div>
        ${buildStatusHtml(statusLabel, statusAction, message.createdAt)}
      </div>
    `;
  }).join('');

  // Scroll to bottom after rendering history
  scrollToLatestMessage();
}

// Send message handler
sendBtn.addEventListener('click', sendMessage);
messageInput.addEventListener('keypress', (e) => {
  if (e.key === 'Enter') {
    sendMessage();
  }
});

// Typing indicator handler
messageInput.addEventListener('input', () => {
  if (!chatService.selectedUserId) return;
  
  // Emit typing-start event
  socket.emit('typing-start', {
    receiverId: chatService.selectedUserId
  });
  
  // Clear previous timeout
  if (typingTimeout) {
    clearTimeout(typingTimeout);
  }
  
  // Set timeout to emit typing-stop after 2 seconds of inactivity
  typingTimeout = setTimeout(() => {
    socket.emit('typing-stop', {
      receiverId: chatService.selectedUserId
    });
  }, TYPING_TIMEOUT);
});

async function sendMessage() {
  const content = messageInput.value.trim();
  if (!content || !chatService.selectedUserId) return;

  // Khi gửi tin mới, ẩn status "Đã gửi" của các tin cũ
  clearDeliveredStatuses();

  const messElement = displaceSendingMessage(content);

  // Always scroll to the latest message after sending
  scrollToLatestMessage();

  // Emit typing-stop khi gửi tin nhắn
  if (typingTimeout) {
    clearTimeout(typingTimeout);
  }
  socket.emit('typing-stop', {
    receiverId: chatService.selectedUserId
  });

  // Implement send message via socket.io or API
  socket.emit("send-message", {
    receiverId: chatService.selectedUserId,
    content: content
  }, (res) => {
    if (res.success) {
      updateMessageStatus(messElement, "success");
    } else {
      updateMessageStatus(messElement, "fail");
    }
  })


  // For now, just clear the input
  messageInput.value = '';
  
  // You can add API call to send message here
  // For now, we'll just show a placeholder
  console.log('Send message:', content, 'to:', chatService.selectedUserId);
}

function displaceSendingMessage(content) {
  const mess = document.createElement("div");

  // Giữ layout giống tin nhắn đã gửi (sent), thêm class "sending" để tuỳ styling nếu cần
  mess.classList.add("message", "sent", "sending");

  const contentMess = document.createElement("div");
  contentMess.classList.add("message-content");
  contentMess.textContent = content;

  // phần hiển thị trạng thái bên dưới tin nhắn
  const statusDiv = document.createElement("div");
  statusDiv.classList.add("message-status");
  applyStatusMetadata(statusDiv, "Đang gửi...", "Gửi", new Date());

  mess.appendChild(contentMess);
  mess.appendChild(statusDiv);

  messagesList.appendChild(mess);

  // Ensure we see the newest message whenever one is rendered
  scrollToLatestMessage();

  return mess; // trả về để cập nhật trạng thái sau
}


function updateMessageStatus(messElement, status, seenAt = null) {
  const statusDiv = messElement.querySelector(".message-status");

  if (!statusDiv) return;

  if (status === "sending") {
    removeRetryButton(statusDiv);
    applyStatusMetadata(statusDiv, "Đang gửi...", "Gửi", new Date());
    return;
  }

  if (status === "success") {
    removeRetryButton(statusDiv);
    applyStatusMetadata(statusDiv, "Đã gửi", "Gửi", statusDiv.dataset.baseTimestamp);
    messElement.classList.remove("sending");
    return;
  }

  if (status === "seen") {
    removeRetryButton(statusDiv);
    applyStatusMetadata(statusDiv, "Đã xem", "Gửi", seenAt || new Date());
    messElement.classList.remove("sending");
    return;
  }

  if (status === "fail") {
    applyStatusMetadata(statusDiv, "Gửi thất bại", "Gửi", statusDiv.dataset.baseTimestamp);
    removeRetryButton(statusDiv);

    const retryBtn = document.createElement("span");
    retryBtn.textContent = " 🔄";
    retryBtn.classList.add("retry-btn");
    retryBtn.style.cursor = "pointer";

    retryBtn.addEventListener("click", () => {
      retrySendMessage(
        messElement.querySelector(".message-content").textContent,
        messElement
      );
    });

    statusDiv.appendChild(retryBtn);
  }
}

function retrySendMessage(content, messElement) {
  // Khi retry cũng coi như gửi tin mới → ẩn status "Đã gửi" của các tin cũ khác
  clearDeliveredStatuses();
  updateMessageStatus(messElement, "sending"); // chuyển sang đang gửi...

  socket.emit("sendMessage", { content }, (res) => {
    if (res.success) {
      updateMessageStatus(messElement, "success");
    } else {
      updateMessageStatus(messElement, "fail");
    }
  });
}

async function handleAvatarFileChange(event) {
  const file = event.target.files && event.target.files[0];
  console.log(file);
  if (!file) {
    resetAvatarStatus();
    return;
  }

  // Validate loại file
  if (!file.type.startsWith('image/')) {
    showAvatarStatus('Vui lòng chọn một file hình ảnh.', 'error');
    avatarFileInput.value = '';
    return;
  }

  // Validate kích thước (2MB)
  const maxSize = 2 * 1024 * 1024;
  if (file.size > maxSize) {
    showAvatarStatus('Ảnh quá lớn. Vui lòng chọn ảnh nhỏ hơn 2MB.', 'error');
    avatarFileInput.value = '';
    return;
  }

  // Preview tạm thời
  const previewUrl = URL.createObjectURL(file);
  applyAvatarVisual(previewUrl);

  showAvatarStatus('Đang tải ảnh lên...', 'info');
  avatarUploadBtn.disabled = true;

  try {
    const response = await userAPI.uploadAvatar(file);
    const avatarUrl = response.data?.avatarUrl || response.data?.avatar || previewUrl;

    const updatedUser = authService.updateStoredUser({ avatar: avatarUrl });
    renderProfile(updatedUser);
    showAvatarStatus('Cập nhật avatar thành công.', 'success');
  } catch (error) {
    console.error('Upload avatar error:', error);
    showAvatarStatus(error.response?.data?.message || 'Tải ảnh thất bại, vui lòng thử lại.', 'error');
  } finally {
    avatarUploadBtn.disabled = false;
    avatarFileInput.value = '';
    // Giải phóng object URL tạm
    URL.revokeObjectURL(previewUrl);
  }
}

async function handleProfileUpdate(event) {
  event.preventDefault();
  if (!profileEditFullName || !profileEditUsername || !profileEditEmail) return;

  const payload = {
    fullName: profileEditFullName.value.trim(),
    username: profileEditUsername.value.trim(),
    email: profileEditEmail.value.trim()
  };

  if (!payload.fullName || !payload.username || !payload.email) {
    showProfileUpdateMessage("Vui lòng nhập đầy đủ thông tin", "error");
    return;
  }

  setProfileFormDisabled(true);
  showProfileUpdateMessage("Đang lưu thay đổi...", "info");

  try {
    await userAPI.updateAccount(payload);
    const updatedUser = authService.updateStoredUser(payload);
    renderProfile(updatedUser);
    showProfileUpdateMessage("Cập nhật thành công", "success");
  } catch (error) {
    const message = error.response?.data?.message || "Cập nhật thất bại, vui lòng thử lại";
    showProfileUpdateMessage(message, "error");
  } finally {
    setProfileFormDisabled(false);
  }
}


// Escape HTML to prevent XSS
function escapeHtml(text) {
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}

function renderProfile(user) {
  currentProfile = user;
  if (!user) {
    profileName.textContent = 'Khách';
    profileEmail.textContent = '';
    applyAvatarVisual(null, '?');
    profileModalName.textContent = 'Khách';
    profileModalUsername.textContent = '@guest';
    profileModalEmail.textContent = '';
    profileModalFullName.textContent = '-';
    profileModalUsernameDetail.textContent = '-';
    profileModalEmailDetail.textContent = '-';
    applyAvatarVisual(null, '?');
    populateProfileForm(null);
    setProfileFormDisabled(true);
    return;
  }

  const displayName = user.fullName || user.username || 'Người dùng';
  const username = user.username || '';
  const displayEmail = user.email || (username ? `@${username}` : '');
  const initials = displayName
    .split(' ')
    .filter(Boolean)
    .map(part => part[0])
    .join('')
    .slice(0, 2)
    .toUpperCase() || 'U';

  profileName.textContent = displayName;
  profileEmail.textContent = displayEmail;
  applyAvatarVisual(user.avatar || null, initials);
  profileModalAvatar.textContent = initials;
  profileModalName.textContent = displayName;
  profileModalUsername.textContent = username ? `@${username}` : '';
  profileModalEmail.textContent = displayEmail;
  profileModalFullName.textContent = displayName;
  profileModalUsernameDetail.textContent = username || '-';
  profileModalEmailDetail.textContent = user.email || '-';
  populateProfileForm(user);
  setProfileFormDisabled(false);
}

function openProfileModal() {
  if (!currentProfile || !profileModal) return;
  populateProfileForm(currentProfile);
  resetProfileUpdateMessage();
  profileModal.classList.add('show');
  profileModal.setAttribute('aria-hidden', 'false');
}

function closeProfileModal() {
  if (!profileModal) return;
  profileModal.classList.remove('show');
  profileModal.setAttribute('aria-hidden', 'true');
}

// Initialize chat page
async function init() {
  // Check authentication
  const user = await authService.checkAuth();
  if (!user) {
    // Not authenticated, redirect to login
    window.location.href = '/index.html';
    return;
  }
  
  renderProfile(user);
  
  // Load users list
  await loadUsers();
}

const socket = io(config.socketUrl, {
  auth: {
    token: localStorage.getItem("token")
  }
});


socket.on("receive-message", (message) => {
  const mess = document.createElement('div');
  mess.classList.add('message', 'received');
  
  const content = document.createElement('div');
  content.classList.add('message-content');
  const messageContent = typeof message === 'string' ? message : message?.content || '';
  content.textContent = messageContent; // tránh XSS

  const statusDiv = document.createElement('div');
  statusDiv.classList.add('message-status');
  applyStatusMetadata(statusDiv, '', 'Nhận', new Date());
  
  mess.appendChild(content);
  mess.appendChild(statusDiv);
  messagesList.appendChild(mess);

  // Tự động cuộn xuống tin nhắn mới nhất khi nhận tin
  scrollToLatestMessage();

  // Emit seen-message nếu đang xem chat với người này
  const senderId = message.senderId || message.sender;
  if (senderId && chatService.selectedUserId && senderId.toString() === chatService.selectedUserId.toString()) {
    // Gửi thông báo đã xem tin nhắn về cho người gửi (senderId)
    socket.emit('seen-message', {
      senderId: senderId.toString()
    });
  }
})

socket.on("connect_error", (err) => {
  console.log("Auth error:", err.message);
});

socket.on("connect", () => {
  console.log("Connected to server");
  // thông báo tôi đang on
  socket.emit("entering")
})

socket.on("disconnect", () => {
  console.log("Disconnected from server");
  // thông báo tôi đang off
  socket.emit("leaving")
})


// Update online status for a specific user in the UI
function updateUserOnlineStatus(userId, isOnline) {
  if (!userId) return;
  
  const userIdStr = userId.toString();
  onlineUsers.set(userIdStr, isOnline);
  
  // Find the user item in the sidebar
  const userItem = document.querySelector(`.user-item[data-user-id="${userIdStr}"]`);
  if (userItem) {
    if (isOnline) {
      userItem.classList.add('online');
    } else {
      userItem.classList.remove('online');
    }
  }
  
  // Update chat header if this is the currently selected user
  const currentSelectedUserId = chatService.selectedUserId?.toString();
  if (currentSelectedUserId === userIdStr) {
    updateChatHeaderOnlineStatus(userIdStr);
  }
}

// Update online indicator in chat header
function updateChatHeaderOnlineStatus(userId) {
  if (!chatOnlineIndicator || !userId) return;
  
  const userIdStr = userId.toString();
  const isOnline = onlineUsers.get(userIdStr) || false;
  
  if (isOnline) {
    chatOnlineIndicator.classList.add('show');
  } else {
    chatOnlineIndicator.classList.remove('show');
  }
}

socket.on("noti-offline", user => {
  const userId = user?.id || user?._id || user;
  console.log(`user with ${userId} is offline`);
  updateUserOnlineStatus(userId, false);
})

socket.on("noti-online", user => {
  const userId = user?.id || user?._id || user;
  console.log(`user with ${userId} is online`);
  updateUserOnlineStatus(userId, true);
})

socket.on("noti-onlineList-toMe", array => {
  console.log("the list of online users: " + array);
  
  // Clear all online statuses first
  onlineUsers.clear();
  document.querySelectorAll('.user-item').forEach(item => {
    item.classList.remove('online');
  });
  
  // Set online status for users in the list
  if (Array.isArray(array)) {
    array.forEach(user => {
      const userId = user?.id || user?._id || user;
      if (userId) {
        updateUserOnlineStatus(userId, true);
      }
    });
  }
})

// Lắng nghe typing indicator từ người khác
socket.on("user-typing", (data) => {
  const { userId, isTyping } = data;
  
  // Chỉ hiển thị nếu đang chat với người đó
  if (chatService.selectedUserId && userId === chatService.selectedUserId.toString()) {
    if (typingIndicator) {
      if (isTyping) {
        // Lấy tên người dùng từ chat header
        const userName = chatNameText ? chatNameText.textContent : chatWithName.textContent;
        const typingText = typingIndicator.querySelector('.typing-text');
        if (typingText) {
          typingText.innerHTML = `<strong>${userName}</strong> đang nhập tin nhắn<span class="typing-dots"><span>.</span><span>.</span><span>.</span></span>`;
        }
        typingIndicator.style.display = 'flex';
      } else {
        typingIndicator.style.display = 'none';
      }
    }
  }
});

// Lắng nghe khi tin nhắn được xem
socket.on("seen-message", (data) => {
  const { viewerId, seenAt } = data;
  
  // Cập nhật tất cả tin nhắn đã gửi cho người này thành "Đã xem"
  if (chatService.selectedUserId && viewerId === chatService.selectedUserId.toString()) {
    const sentMessages = messagesList.querySelectorAll('.message.sent');
    sentMessages.forEach((message) => {
      const statusDiv = message.querySelector('.message-status');
      if (statusDiv) {
        const labelText = statusDiv.querySelector('.message-status-label')?.textContent?.trim();
        // Chỉ cập nhật những tin có trạng thái "Đã gửi"
        if (labelText === 'Đã gửi') {
          updateMessageStatus(message, 'seen', seenAt);
        }
      }
    });
  }
});
const videoCallBtn = document.getElementById("video-call-btn");

if (videoCallBtn) {
  videoCallBtn.addEventListener("click", () => {
    if (!chatService.selectedUserId) {
      alert("Vui lòng chọn người để gọi");
      return;
    }

    // Mở cửa sổ call (WebRTC)
    startVideoCall(chatService.selectedUserId);
  });
}
function startVideoCall(friendId) {
  // mở popup / tab / modal cho WebRTC
  const callWindow = window.open(
      `/call.html?to=${friendId}`,
      "_blank",
      "width=900,height=600"
  );

  if (!callWindow) {
    alert("Trình duyệt chặn popup");
  }
}




// Initialize
init();



