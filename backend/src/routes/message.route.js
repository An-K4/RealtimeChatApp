const express = require("express");
const router = express.Router();
const authMiddleware = require("../middlewares/auth.middleware");
const controller = require("../controllers/message.controller")

router.get('/users', authMiddleware, controller.getUsers);
router.get('/:id', authMiddleware, controller.getMessages);


module.exports = router;