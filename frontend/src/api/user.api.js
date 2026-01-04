import api from './http.js';

export const userAPI = {
  updateAccount: (payload) => api.patch('/users/update', payload),

  // Upload avatar (multipart/form-data)
  uploadAvatar: (file) => {
    const formData = new FormData();
    formData.append('avatar', file);
    return api.patch('/users/upload-avatar', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
  },

  // Search users by username
  searchUsers: (username) => api.get(`/users/search?username=${encodeURIComponent(username)}`),
};
