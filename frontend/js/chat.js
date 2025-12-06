import { messageAPI } from './api.js';
import authService from './auth.js';

class ChatService {
  constructor() {
    this.selectedUserId = null;
    this.selectedUserName = null;
    this.users = [];
    this.messages = {};
  }

  // Lấy danh sách người dùng
  async loadUsers() {
    try {
      const response = await messageAPI.getUsers();
      this.users = response.data.users;
      return { success: true, users: this.users };
    } catch (error) {
      const message = error.response?.data?.message || error.response?.data?.error || 'Không thể tải danh sách người dùng';
      return { success: false, message };
    }
  }

  // Lấy tin nhắn với một người dùng
  async loadMessages(userId) {
    try {
      const response = await messageAPI.getMessages(userId);
      const messages = response.data.messages || [];
      this.messages[userId] = messages;
      return { success: true, messages };
    } catch (error) {
      const message = error.response?.data?.message || error.response?.data?.error || 'Không thể tải tin nhắn';
      return { success: false, message };
    }
  }

  // Chọn người dùng để chat
  selectUser(userId, userName) {
    this.selectedUserId = userId;
    this.selectedUserName = userName;
  }

  // // Thêm tin nhắn vào cache
  // addMessage(userId, message) {
  //   if (!this.messages[userId]) {
  //     this.messages[userId] = [];
  //   }
  //   this.messages[userId].push(message);
  // }

  // Lấy tin nhắn từ cache
  // getCachedMessages(userId) {
  //   return this.messages[userId] || [];
  // }

  // Làm mới tin nhắn
  // clearMessages(userId) {
  //   if (userId) {
  //     delete this.messages[userId];
  //   } else {
  //     this.messages = {};
  //   }
  // }

  // Reset
  reset() {
    this.selectedUserId = null;
    this.selectedUserName = null;
    this.users = [];
    this.messages = {};
  }
}

export default new ChatService();

