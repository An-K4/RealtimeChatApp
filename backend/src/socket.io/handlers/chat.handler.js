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
        content: data.content,
        sentAt: savedMessage.createdAt
      });

    } catch (error) {
      console.log(error);
      updateStatus({success: false})
    }
    
  })
} 