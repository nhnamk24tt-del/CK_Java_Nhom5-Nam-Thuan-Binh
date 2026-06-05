const bookingController = require("../controllers/booking.controllers.js");
const { isAuthenticated, isAdmin } = require("../middleware/auth.middleware.js");
const router = require("express").Router();

module.exports = app => {
  // Public routes
  router.get("/", bookingController.findAll);
  router.get("/my-bookings", bookingController.findByUserId);
  router.get("/:id", bookingController.findOne);
  
  // User can create booking
  router.post("/create", isAuthenticated, bookingController.create);
  
  // Admin can approve/reject bookings
  router.put("/:id/approve", isAdmin, bookingController.approve);
  router.put("/:id/reject", isAdmin, bookingController.reject);
  
  // Admin can delete bookings
  router.delete("/:id", isAdmin, bookingController.delete);

  app.use('/api/bookings', router);
};
