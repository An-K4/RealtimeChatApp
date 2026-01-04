const Message = require("../models/message.model");
const User = require("../models/user.model");
const mongoose = require("mongoose");

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


// lấy danh sách người dùng đã từng nhắn tin với tôi
module.exports.getUsers = async (req, res) => {
  try {
    const userId = req.user.id;
    const userObjectId = new mongoose.Types.ObjectId(userId);

    const users = await Message.aggregate([
      // 1. Lọc tin nhắn liên quan đến userId
      {
        $match: {
          $or: [
            { senderId: userObjectId },
            { receiverId: userObjectId }
          ]
        }
      },
      
      // 2. Sắp xếp theo thời gian (mới nhất trước)
      { $sort: { createdAt: -1 } },
      
      // 3. Group theo otherUserId
      {
        $group: {
          _id: {
            $cond: [
              { $eq: ['$senderId', userObjectId] },
              '$receiverId',
              '$senderId'
            ]
          },
          lastMessage: { $first: '$$ROOT' },  // Tin nhắn cuối cùng
          unreadCount: {
            $sum: {
              $cond: [
                {
                  $and: [
                    { $eq: ['$receiverId', userObjectId] },  // Tin gửi cho mình
                    { $not: { $in: [userObjectId, '$seenBy'] } }  // Mình chưa xem
                  ]
                },
                1,
                0
              ]
            }
          }
        }
      },
      
      // 4. Lookup thông tin user
      {
        $lookup: {
          from: 'users',
          localField: '_id',
          foreignField: '_id',
          as: 'userInfo'
        }
      },
      { $unwind: '$userInfo' },
      
      // 5. Project các field cần thiết
      {
        $project: {
          _id: '$userInfo._id',
          username: '$userInfo.username',
          fullName: '$userInfo.fullName',
          email: '$userInfo.email',
          avatar: '$userInfo.avatar',
          unreadCount: 1,
          lastMessage: {
            content: '$lastMessage.content',
            createdAt: '$lastMessage.createdAt',
            isMine: { $eq: ['$lastMessage.senderId', userObjectId] }
          },
          lastMessageTime: '$lastMessage.createdAt'
        }
      },
      
      // 6. Sắp xếp theo tin nhắn mới nhất
      { $sort: { lastMessageTime: -1 } }
    ]);

    return res.status(200).json({ 
      users,
    })
  } catch (error) {
    console.log(error);
    return res.status(500).json({message: "Lỗi server khi lấy danh sách người dùng"});
  }
}