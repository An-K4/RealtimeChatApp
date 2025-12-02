const authRouter = require("./auth.route");

module.exports = (app) => {
  app.use('/auth', authRouter);
}

