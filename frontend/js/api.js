import axios from 'axios';

// Tạo axios instance với base URL
const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Interceptor để thêm token vào mỗi request
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Interceptor để xử lý lỗi 401 (unauthorized)
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Token không hợp lệ hoặc hết hạn
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      // Chỉ reload nếu đang ở trang chat
      if (document.getElementById('chat-page')?.style.display !== 'none') {
        window.location.reload();
      }
    }
    return Promise.reject(error);
  }
);

// Auth API
export const authAPI = {
  login: (username, password) => 
    api.post('/auth/login', { username, password }),
  
  signup: (username, password, fullName, email) => 
    api.post('/auth/signup', { username, password, fullName, email }),
  
  logout: () => 
    api.post('/auth/logout'),
  
  getMe: () => 
    api.get('/auth/me'),
};

// Message API
export const messageAPI = {
  getUsers: () => 
    api.get('/messages/users'),
  
  getMessages: (friendId) => 
    api.get(`/messages/${friendId}`),
};

// User API
export const userAPI = {
  updateAccount: (payload) =>
    api.patch('/users/update', payload),

  // Upload avatar (multipart/form-data)
  uploadAvatar: (file) => {
    const formData = new FormData();
    formData.append('avatar', file);
    return api.patch('/users/upload-avatar', formData, {
      headers: {
        "Content-Type": "multipart/form-data"
      }
    });
  },
};

export default api;

