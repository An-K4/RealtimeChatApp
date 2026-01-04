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
        createdAt: savedMessage.createdAt,

        // thêm trường mới
        _id: savedMessage._id,
        senderId: socket.user._id
      });

    } catch (error) {
      console.log(error);
      updateStatus({success: false})
    }
    
  })

  // Xử lý khi người dùng xem tin nhắn
  socket.on("seen-message", async (data) => {
    try {
      const { senderId } = data;
      const viewerId = socket.user._id;
      
      if (!senderId || !viewerId) return;

      // Update DB: Mark seen cho tất cả messages từ senderId đến viewerId
      const result = await Message.updateMany(
        { senderId, receiverId: viewerId, seenBy: { $nin: [viewerId] } },
        { $push: { seenBy: viewerId } }
      );

      // Chỉ emit nếu có messages được update
      if (result.modifiedCount > 0) {
        // Gửi thông báo về cho người gửi tin rằng tin nhắn đã được xem
        io.to(senderId.toString()).emit("seen-message", {
          viewerId: viewerId.toString(), // người xem tin nhắn
          seenAt: new Date()
        });
      }
    } catch (error) {
      console.error("Error marking seen:", error);
    }
  })

  // Xử lý khi user bắt đầu gõ
  socket.on("typing-start", (data) => {
    const receiverId = data.receiverId;
    if(!receiverId) return;

    // Gửi thông báo tới người nhận rằng user này đang gõ
    io.to(receiverId.toString()).emit("typing-start", {
      senderId: socket.user._id.toString(),
      senderName: socket.user.fullName || socket.user.username
    })
  })
  
  // Xử lý khi user dừng gõ
  socket.on("typing-stop", (data) => {
    const receiverId = data.receiverId;
    if(!receiverId) return;

    // Gửi thông báo tới người nhận rằng user này đã dừng gõ
    io.to(receiverId.toString()).emit("typing-stop", {
      senderId: socket.user._id.toString()
    })
  })
} 