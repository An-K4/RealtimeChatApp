const express = require("express");
require("dotenv").config();

const databaseConfig = require("./src/configs/database.config");

const UserRouter = require("./src/routes/index.route")

const app = express();

// middleware 
app.use(express.urlencoded({extended: false}));
app.use(express.json());


databaseConfig.connect();

UserRouter(app)

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log("Server is listening on port " + PORT)
})