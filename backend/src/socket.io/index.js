const authMiddleware = require("./middlewares/auth.middleware");
const chatHandler = require("./handlers/chat.handler");

module.exports = (httpServer) => {
  const {
    Server
  } = require("socket.io");
  const io = new Server(httpServer, {
    cors: {
      origin: ["http://localhost:5173"]
    },
    method: ["GET", "POST"],
  });


  // middleware toàn cục (chạy 1 lần khi kết nối).
  io.use(authMiddleware);

  // lưu những người đang online
  let onlineUsers = new Map(); // userId

  io.on("connection", (socket) => {
    const currentUserId = socket.user._id.toString();
    console.log("A user connected", socket.id, "User ID:", currentUserId);
    // Join room với userId dạng string để nhận tin nhắn
    socket.join(currentUserId);


    // nếu đây là tab đầu tiên của tôi -> thông báo tới tất cả mọi người
    if (!onlineUsers.has(currentUserId)) {
      // thông báo với tất cả ngoài trừ tôi là tôi đang on
      socket.broadcast.emit("noti-online", {
        id: currentUserId
      });

      // thêm
      onlineUsers.set(currentUserId, 1);
    } else {
      // thêm
      onlineUsers.set(currentUserId, onlineUsers.get(currentUserId) + 1)
    }


    // thông báo cho tôi những người đang trong danh sách online
    socket.emit("noti-onlineList-toMe", [...onlineUsers.keys()]);


    socket.on("disconnect", () => {
      // nếu count của userId đó = 0 -> offline -> loại
      onlineUsers.set(currentUserId, onlineUsers.get(currentUserId) - 1);

      if (onlineUsers.get(currentUserId) === 0) {
        onlineUsers.delete(currentUserId);
        socket.broadcast.emit("noti-offline", {
          id: currentUserId
        })
        // thông báo tới tất cả ngoại trừ tôi là tôi đang off
      }
    })
    chatHandler(io, socket);
  })
}