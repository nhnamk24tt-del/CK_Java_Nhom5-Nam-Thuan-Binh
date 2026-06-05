const messageController = require('../controllers/message.controllers');
const { isAdmin } = require('../middleware/auth.middleware.js');
const router = require('express').Router();

module.exports = app => {
  router.post('/create', messageController.create);
  // Admin endpoints: allow POST-based access from admin UI (sends activeUser in body)
  router.post('/list', isAdmin, messageController.findAll);
  router.get('/my', messageController.findByEmail);
  router.post('/:id', isAdmin, messageController.findOne);
  router.put('/:id/reply', isAdmin, messageController.reply);
  router.put('/:id/mark-read', isAdmin, messageController.markRead);

  app.use('/api/messages', router);
};
