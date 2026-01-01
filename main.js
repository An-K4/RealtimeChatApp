const { app, BrowserWindow } = require('electron');
const { spawn } = require('child_process');
const path = require('path');

let mainWindow;
let backendProcess;
let frontendProcess;

const BACKEND_PORT = 3000;
const FRONTEND_PORT = 5173;
const BACKEND_URL = `http://localhost:${BACKEND_PORT}`;
const FRONTEND_URL = `http://localhost:${FRONTEND_PORT}`;

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1200,
    height: 800,
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true
    },
    // icon: path.join(__dirname, 'icon.png'), // Optional icon - uncomment if you have icon.png
    title: 'Realtime Chat App'
  });

  // Đợi frontend khởi động xong rồi mới load
  const checkFrontend = setInterval(() => {
    const http = require('http');
    const req = http.get(FRONTEND_URL, (res) => {
      if (res.statusCode === 200) {
        clearInterval(checkFrontend);
        mainWindow.loadURL(FRONTEND_URL);
      }
    });
    req.on('error', () => {
      // Frontend chưa sẵn sàng, tiếp tục đợi
    });
  }, 1000);

  // Mở DevTools trong development (có thể xóa sau)
  // mainWindow.webContents.openDevTools();

  mainWindow.on('closed', () => {
    mainWindow = null;
  });
}

function startBackend() {
  const backendPath = path.join(__dirname, 'backend');
  backendProcess = spawn('node', ['index.js'], {
    cwd: backendPath,
    stdio: 'inherit',
    shell: true
  });

  backendProcess.on('error', (error) => {
    console.error('Backend error:', error);
  });
}

function startFrontend() {
  const frontendPath = path.join(__dirname, 'frontend');
  frontendProcess = spawn('npm', ['run', 'dev'], {
    cwd: frontendPath,
    stdio: 'inherit',
    shell: true
  });

  frontendProcess.on('error', (error) => {
    console.error('Frontend error:', error);
  });
}

app.whenReady().then(() => {
  console.log('Starting backend and frontend servers...');
  startBackend();
  
  // Đợi một chút để backend khởi động
  setTimeout(() => {
    startFrontend();
    // Đợi frontend khởi động rồi mới tạo window
    setTimeout(() => {
      createWindow();
    }, 2000);
  }, 1000);

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow();
    }
  });
});

app.on('window-all-closed', () => {
  // Dừng các process khi đóng app
  if (backendProcess) {
    backendProcess.kill();
  }
  if (frontendProcess) {
    frontendProcess.kill();
  }

  if (process.platform !== 'darwin') {
    app.quit();
  }
});

app.on('before-quit', () => {
  // Đảm bảo dừng tất cả process trước khi thoát
  if (backendProcess) {
    backendProcess.kill();
  }
  if (frontendProcess) {
    frontendProcess.kill();
  }
});

