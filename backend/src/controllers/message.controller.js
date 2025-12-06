const Message = require("../models/message.model");
const User = require("../models/user.model");

// lấy messages giữa tôi và người này
module.exports.getMessages = async (req, res) => {
  try {
    const { id: friendId } = req.params;
    const userId = req.user.id;

    console.log(userId, friendId);

    // lấy tất cả messages giữa tôi và người này
    const messages = await Message.find({
      senderId: { $in: [userId, friendId] },
      receiverId: { $in: [userId, friendId] }
    }).sort({ createdAt: 1 }).select('-updatedAt');    

    return res.status(200).json({
      messages,
    })


  } catch (error) {
    console.log(error);
    return res.status(500).json({message: "Lỗi server khi lấy messages"});
  }
}


// lấy danh sách người dùng khác tôi
module.exports.getUsers = async (req, res) => {
  try {
    const userId = req.user.id;

    const users = await User.find({
      _id: { $ne: userId}
    }).select('-password -createdAt -updatedAt -__v');

    return res.status(200).json({ 
      users,
    })
  } catch (error) {
    console.log(error);
    return res.status(500).json({message: "Lỗi server khi lấy danh sách người dùng"});
  }
}