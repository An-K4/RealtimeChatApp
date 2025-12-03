
const authMiddleware = require("./middlewares/auth.middleware");

module.exports = (httpServer) => {
  const { Server } = require("socket.io");
  const io = new Server(httpServer, {
    cors: {
      origin: ['*']
    },
    method: ["GET", "POST"],
  });


  // middleware toàn cục (chạy 1 lần khi kết nối).
  io.use(authMiddleware);

  io.on("connection", (socket) => {
    console.log("A user connected", socket.id);
  })


}