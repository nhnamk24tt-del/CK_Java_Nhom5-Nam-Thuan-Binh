const carController = require("../controllers/car.controllers.js");
const { isAuthenticated, isAdmin } = require("../middleware/auth.middleware.js");
const router = require("express").Router();

module.exports = app => {
  router.get("/", carController.findAll);
  router.get("/:id", carController.findOne);
  router.put("/:id/book", isAuthenticated, carController.book);

  router.post("/admin/add", isAdmin, carController.create);
  router.put("/admin/edit/:id", isAdmin, carController.update);
  router.delete("/admin/delete/:id", isAdmin, carController.delete);

  app.use('/api/cars', router);
};
