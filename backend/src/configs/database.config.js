const mongoose = require('mongoose');

module.exports.connect = async () => {
  try {
    const result = await mongoose.connect(process.env.MONGO_URL);
    console.log("database connected successfully");

  } catch (error) {
    console.error(error.message);
  }
}
