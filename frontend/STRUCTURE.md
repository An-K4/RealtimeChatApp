# Cấu trúc Frontend

## Tổng quan

Frontend được chia thành 2 trang riêng biệt:

### 1. Trang Login (`index.html`)
- **File**: `index.html`
- **Script**: `js/login.js`
- **Chức năng**:
  - Đăng nhập
  - Đăng ký
  - Chuyển hướng đến trang chat sau khi đăng nhập thành công

### 2. Trang Chat (`chat.html`)
- **File**: `chat.html`
- **Script**: `js/chatPage.js`
- **Chức năng**:
  - Hiển thị sidebar với danh sách users
  - Chat với từng user
  - Load và hiển thị messages
  - Logout

## Modules được chia sẻ

### Services
- **`js/api.js`**: Axios instance và API calls
- **`js/auth.js`**: Authentication service (login, signup, logout)
- **`js/chat.js`**: Chat service (load users, messages)

### Styles
- **`styles/main.css`**: Styles cho cả 2 trang

## Luồng dữ liệu

```
index.html (login.js)
    ↓ [Login thành công]
    ↓ Redirect to /chat.html
chat.html (chatPage.js)
    ↓ [Sử dụng]
    ├── auth.js (check auth, get user)
    ├── chat.js (load users, messages)
    └── api.js (HTTP requests)
```

## Navigation

- Login thành công → `window.location.href = '/chat.html'`
- Logout → `window.location.href = '/index.html'`
- Không có auth ở chat page → Redirect về `/index.html`

## File Structure

```
frontend/
├── index.html          # Trang login/signup
├── chat.html          # Trang chat
├── js/
│   ├── api.js         # API configuration
│   ├── auth.js        # Auth service
│   ├── chat.js        # Chat service
│   ├── login.js       # Login page logic
│   └── chatPage.js    # Chat page logic
├── styles/
│   └── main.css       # Shared styles
├── package.json
├── vite.config.js
└── README.md
```

