const User = require("../models/user.model");
const Group = require("../models/group.model");
const bcrypt = require("bcrypt");

module.exports.changePassword = async (req, res) => {
  try {
    const { oldPassword, newPassword } = req.body;
    const userId = req.user.id;

    if (!oldPassword || !newPassword) {
      return res.status(400).json({ message: "Vui lòng nhập đầy đủ thông tin" });
    }

    const user = await User.findById(userId);
    if (!user) {
      return res.status(404).json({ message: "Người dùng không tồn tại" });
    }

    const isMatch = await bcrypt.compare(oldPassword, user.password);
    if (!isMatch) {
      return res.status(400).json({ message: "Bạn đã nhập sai mật khẩu cũ" });
    }

    const salt = await bcrypt.genSalt(10);
    user.password = await bcrypt.hash(newPassword, salt);
    await user.save();

    return res.status(200).json({ message: "Đổi mật khẩu thành công" });
  } catch (error) {
    console.log("Change password error:", error);
    return res.status(500).json({ message: "Lỗi server khi đổi mật khẩu" });
  }
};

module.exports.editAccount = async (req, res) => {
  try {
    const {
      username,
      fullName,
      email
    } = req.body;
    const userId = req.user.id;

    // Kiểm tra username đã tồn tại ở user khác
    const usernameExist = await User.findOne({
      _id: {
        $ne: userId
      },
      username
    });

    if (usernameExist) {
      return res.status(400).json({
        message: "Username này đã tồn tại"
      });
    }

    // Kiểm tra email đã tồn tại ở user khác
    const emailExist = await User.findOne({
      _id: {
        $ne: userId
      },
      email
    });

    if (emailExist) {
      return res.status(400).json({
        message: "Email này đã tồn tại"
      });
    }

    // Cập nhật user
    const updated = await User.updateOne({
      _id: userId
    }, {
      username,
      fullName,
      email
    });

    return res.status(200).json({
      message: "Cập nhật tài khoản thành công",
      updated
    });

  } catch (error) {
    console.log(error);
    return res.status(500).json({
      message: "Lỗi server khi cập nhật tài khoản"
    });
  }
};


module.exports.uploadAvatar = async (req, res) => {
  try {
    const userId = req.user.id;
    const avatar = req.body.avatar;
    if (!avatar) {
      return res.status(400).json({
        message: "Bạn chưa upload avatar"
      })
    }
    // save avatar
    const user = await User.findByIdAndUpdate(
      userId, {
      avatar
    }, {
      new: true
    } // trả về user sau khi update
    );

    return res.status(200).json({
      message: "Avatar updated",
      avatarUrl: user.avatar
    })
  } catch (error) {
    console.log(error);
    return res.status(500).json({
      message: "Lỗi server"
    })
  }
}

module.exports.search = async (req, res) => {
  try {
    const { keyword } = req.query;

    if (!keyword) {
      return res.status(400).json({ message: "Vui lòng cung cấp keyword để tìm kiếm!" });
    }

    const userId = req.user.id;
    const users = await User.find({
      _id: { $ne: userId }, // Loại trừ user hiện tại
      username: { $regex: keyword, $options: 'i' }
    }).select('-password')

    const groups = await Group.find({
      'members.userId': userId,
      name: { $regex: keyword, $options: 'i' }
    })

    return res.status(200).json({
      message: "Tìm kiếm thành công!",
      users,
      groups
    })

  } catch (error) {
    console.log("Search error:", error);
    return res.status(500).json({ message: "Lỗi server khi tìm kiếm!" });
  }
}