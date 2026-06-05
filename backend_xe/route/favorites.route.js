const crypto = require('crypto');
const router = require('express').Router();

const JWT_SECRET = process.env.JWT_SECRET || 'rentwheels_secret_key';

const verifyToken = (token) => {
  if (!token) return null;
  const parts = token.split('.');
  if (parts.length !== 2) return null;

  const [payloadBase64, signature] = parts;
  const expectedSignature = crypto
    .createHmac('sha256', JWT_SECRET)
    .update(payloadBase64)
    .digest('base64url');

  try {
    if (!crypto.timingSafeEqual(Buffer.from(signature), Buffer.from(expectedSignature))) {
      return null;
    }
    const payload = JSON.parse(Buffer.from(payloadBase64, 'base64url').toString('utf8'));
    if (!payload.exp || payload.exp < Date.now()) {
      return null;
    }
    return payload;
  } catch {
    return null;
  }
};

module.exports = (app, db) => {
  const Favorite = require('../models/favorites.models');

  router.get('/', (req, res) => {
    const authHeader = req.headers.authorization || '';
    const token = authHeader.startsWith('Bearer ') ? authHeader.slice(7) : null;
    const payload = verifyToken(token);

    if (!payload) {
      return res.status(401).json({ success: false, message: 'Token không hợp lệ.' });
    }

    Favorite.getByUserId(payload.id, (err, data) => {
      if (err) return res.status(500).json({ success: false, message: err.message });
      const favoriteIds = Array.isArray(data) ? data.map((row) => row.car_id) : [];
      return res.json(favoriteIds);
    });
  });

  router.post('/', (req, res) => {
    const authHeader = req.headers.authorization || '';
    const token = authHeader.startsWith('Bearer ') ? authHeader.slice(7) : null;
    const payload = verifyToken(token);

    if (!payload) {
      return res.status(401).json({ success: false, message: 'Token không hợp lệ.' });
    }

    const carId = req.body.car_id;
    if (!carId) {
      return res.status(400).json({ success: false, message: 'Thiếu car_id để thêm yêu thích.' });
    }

    Favorite.add(payload.id, carId, (err) => {
      if (err) return res.status(500).json({ success: false, message: err.message });
      return res.json({ success: true, message: 'Thêm vào yêu thích thành công.' });
    });
  });

  router.delete('/:carId', (req, res) => {
    const authHeader = req.headers.authorization || '';
    const token = authHeader.startsWith('Bearer ') ? authHeader.slice(7) : null;
    const payload = verifyToken(token);

    if (!payload) {
      return res.status(401).json({ success: false, message: 'Token không hợp lệ.' });
    }

    const carId = req.params.carId;
    Favorite.remove(payload.id, carId, (err) => {
      if (err) return res.status(500).json({ success: false, message: err.message });
      return res.json({ success: true, message: 'Xoá khỏi yêu thích thành công.' });
    });
  });

  app.use('/api/favorites', router);
};
