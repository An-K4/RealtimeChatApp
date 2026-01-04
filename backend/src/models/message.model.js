const mongoose = require("mongoose");

const messageSchema = new mongoose.Schema(
  {
    senderId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "User",
      required: true,
    },

    receiverId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "User",
      required: true,
    },

    content: {
      type: String,
    },

    seenBy: [
      { type: mongoose.Schema.Types.ObjectId, ref: 'User' }
    ]
  },
  { timestamps: true }
);

const Message = mongoose.model("Message", messageSchema);

module.exports =  Message;
