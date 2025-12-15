const { now } = require("mongoose");
const Message = require("../../models/message.model");

module.exports =  (io, socket) => {
  socket.on("send-message", async (data, updateStatus) => {
    try {
      const savedMessage = await Message.create({
        senderId: socket.user._id,
        receiverId: data.receiverId,
        content: data.content
      });
     
      console.log(savedMessage.createdAt)
      updateStatus({success: true});
      io.to(data.receiverId.toString()).emit("receive-message", {
        senderId: socket.user._id.toString(), // Thêm senderId để người nhận biết ai gửi
        content: data.content,
        sentAt: savedMessage.createdAt
      });

    } catch (error) {
      console.log(error);
      updateStatus({success: false})
    }
    
  })

  // Xử lý khi người dùng xem tin nhắn
  socket.on("seen-message", (data) => {
    const { senderId } = data;
    if (!senderId) return;

    // Gửi thông báo về cho người gửi tin rằng tin nhắn đã được xem
    io.to(senderId.toString()).emit("seen-message", {
      viewerId: socket.user._id.toString(), // người xem tin nhắn
      seenAt: new Date()
    });
  })

  // Xử lý khi user bắt đầu gõ
  socket.on("typing-start", (data) => {
    const receiverId = data.receiverId;
    if(!receiverId) return;

    // Gửi thông báo tới người nhận rằng user này đang gõ
    io.to(receiverId.toString()).emit("user-typing", {
      userId: socket.user._id.toString(),
      isTyping: true
    })
  })
  
  // Xử lý khi user dừng gõ
  socket.on("typing-stop", (data) => {
    const receiverId = data.receiverId;
    if(!receiverId) return;

    // Gửi thông báo tới người nhận rằng user này đã dừng gõ
    io.to(receiverId.toString()).emit("user-typing", {
      userId: socket.user._id.toString(),
      isTyping: false
    })
  })
} 