const express = require("express");
require("dotenv").config();
const initSocket = require("./src/socket.io/index");

const databaseConfig = require("./src/configs/database.config");

const UserRouter = require("./src/routes/index.route")
const { createServer } = require("http");

const app = express();

// middleware 
app.use(express.urlencoded({extended: false}));
app.use(express.json());


databaseConfig.connect();

UserRouter(app)

const httpServer = createServer(app);
initSocket(httpServer);

const PORT = process.env.PORT || 3000;
httpServer.listen(PORT, () => {
  console.log("Server is listening on port " + PORT)
})