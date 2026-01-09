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
    },
    groupId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "Group"
    },
    content: {
      type: String,
    },
    replyTo: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "Message"
    },
    attachments: {
      type: String
    },
    seenBy: {
      type: [{ type: mongoose.Schema.Types.ObjectId, ref: "User" }],
      default: [],
    },
  },
  { timestamps: true }
);

const Message = mongoose.model("Message", messageSchema);

module.exports =  Message;
