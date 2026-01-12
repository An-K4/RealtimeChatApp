const express = require("express");
const router = express.Router();
const authMiddleware = require("../middlewares/auth.middleware");
const controller = require("../controllers/message.controller");
const uploadMulter = require("../configs/multer.config");
const uploadCloud = require("../middlewares/uploadClound.middleware");

router.get('/users', authMiddleware, controller.getUsers);
router.get('/:id', authMiddleware, controller.getMessages);
router.post('/upload', authMiddleware, uploadMulter.single('file'), uploadCloud.upload, controller.upload)


module.exports = router;