# Realtime Chat App - Frontend

Frontend đơn giản cho ứng dụng chat realtime sử dụng HTML, CSS, JavaScript và Vite.

## Cài đặt

```bash
cd frontend
npm install
```

## Chạy ứng dụng

```bash
npm run dev
```

Ứng dụng sẽ chạy tại `http://localhost:5173`

## Cấu trúc dự án

### Pages (Trang)
- `index.html` - Trang đăng nhập/đăng ký
- `chat.html` - Trang chat chính

### Styles
- `styles/main.css` - Styles cho toàn bộ ứng dụng

### JavaScript Modules

#### Core Services
- `js/api.js` - Cấu hình axios và API calls
  - Axios instance với interceptors
  - Auth API functions
  - Message API functions

- `js/auth.js` - Xử lý authentication
  - Login
  - Signup
  - Logout
  - Check authentication status

- `js/chat.js` - Xử lý chat logic
  - Load danh sách users
  - Load messages
  - Quản lý state của chat

#### Page Scripts
- `js/login.js` - Logic cho trang login/signup
  - Tab switching
  - Form handlers
  - Redirect sau khi login thành công

- `js/chatPage.js` - Logic cho trang chat
  - Load users list
  - Render messages
  - Send message handler
  - User selection

## Luồng hoạt động

1. **Trang Login (`index.html`)**:
   - Người dùng đăng nhập hoặc đăng ký
   - Sau khi đăng nhập thành công → Redirect đến `/chat.html`

2. **Trang Chat (`chat.html`)**:
   - Kiểm tra authentication
   - Nếu chưa đăng nhập → Redirect về `/index.html`
   - Hiển thị sidebar với danh sách users
   - Chọn user để chat

## Tính năng

- ✅ Đăng nhập / Đăng ký
- ✅ Hiển thị danh sách người dùng trong sidebar
- ✅ Chat với từng người dùng
- ✅ Hiển thị tin nhắn theo thời gian
- ✅ Routing đơn giản giữa các trang

## Lưu ý

- Backend cần chạy tại `http://localhost:3000`
- Vite proxy đã được cấu hình để forward requests từ `/api` đến backend
- Token được lưu trong localStorage
- Mỗi trang có file JavaScript riêng để quản lý logic
