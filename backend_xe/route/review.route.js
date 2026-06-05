const reviewController = require('../controllers/review.controllers');
const router = require('express').Router();

module.exports = app => {
  router.get('/car/:carId', reviewController.findByCarId);
  router.post('/', reviewController.create);
  router.post('/recompute/:carId', reviewController.recomputeStats);

  app.use('/api/reviews', router);
};
