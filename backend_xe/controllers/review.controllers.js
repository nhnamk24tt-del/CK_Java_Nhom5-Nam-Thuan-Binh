const Review = require('../models/review.models');
const db = require('../db');

exports.create = (req, res) => {
  // ĐÃ SỬA: Lấy thêm user_id, user_name, user_email trực tiếp từ req.body do Frontend gửi lên
  const { car_id, rating, comment, user_id, user_name, user_email } = req.body;
  const loggedInUser = req.user || null;

  if (!car_id || !rating) {
    return res.status(400).json({ message: 'Thiếu car_id hoặc rating.' });
  }

  // Ưu tiên lấy thông tin từ req.body (Frontend gửi lên), nếu không có mới dùng req.user (Middleware)
  const reviewData = {
    car_id: Number(car_id),
    rating: Number(rating),
    comment: comment || null,
    user_id: user_id || loggedInUser?.id || null,
    user_name: user_name || loggedInUser?.name || null,
    user_email: user_email || loggedInUser?.email || null
  };

  Review.create(reviewData, (err, data) => {
    if (err) {
      console.error('Lỗi tạo review:', err);
      return res.status(500).json({ message: err.message || 'Lỗi khi lưu đánh giá.' });
    }

    // Sau khi tạo review thành công, tiến hành tính toán đồng bộ ngay lập tức sang bảng thong_tin_xe
    const aggSql = 'SELECT AVG(rating) AS avg_rating, COUNT(*) AS cnt FROM reviews WHERE car_id = ? AND (rating IS NOT NULL)';
    db.query(aggSql, [car_id], (aggErr, aggRes) => {
      if (aggErr) {
        console.error('Lỗi khi tính toán thống kê review:', aggErr);
        return res.status(201).json({ success: true, review: data });
      }

      const avg = aggRes && aggRes[0] && aggRes[0].avg_rating !== null ? Number(aggRes[0].avg_rating).toFixed(1) : '0.0';
      const cnt = aggRes && aggRes[0] ? Number(aggRes[0].cnt || 0) : 0;

      const updSql = 'UPDATE thong_tin_xe SET rating = ?, reviews = ? WHERE id = ?';
      db.query(updSql, [avg, cnt, car_id], (updErr) => {
        if (updErr) {
          console.error('Lỗi khi cập nhật thống kê xe trong thong_tin_xe:', updErr);
          return res.status(201).json({ success: true, review: data, stats: { avg, count: cnt } });
        }

        // Trả về thành công mỹ mãn cho Client
        return res.status(201).json({ success: true, review: data, stats: { avg, count: cnt } });
      });
    });
  });
};

exports.findByCarId = (req, res) => {
  const carId = req.params.carId;
  Review.findByCarId(carId, (err, data) => {
    if (err) {
      console.error('Lỗi lấy review theo carId:', err);
      return res.status(500).json({ message: err.message || 'Lỗi khi tải đánh giá.' });
    }
    return res.json(data);
  });
};

// Tái tính điểm trung bình và số lượng review cho một chiếc xe (có thể gọi thủ công)
exports.recomputeStats = (req, res) => {
  const carId = req.params.carId;
  if (!carId) return res.status(400).json({ message: 'Thiếu carId' });

  const aggSql = 'SELECT AVG(rating) AS avg_rating, COUNT(*) AS cnt FROM reviews WHERE car_id = ? AND (rating IS NOT NULL)';
  db.query(aggSql, [carId], (aggErr, aggRes) => {
    if (aggErr) {
      console.error('Lỗi khi tính toán thống kê review:', aggErr);
      return res.status(500).json({ message: 'Lỗi tính toán thống kê' });
    }

    const avg = aggRes && aggRes[0] && aggRes[0].avg_rating !== null ? Number(aggRes[0].avg_rating).toFixed(1) : '0.0';
    const cnt = aggRes && aggRes[0] ? Number(aggRes[0].cnt || 0) : 0;

    const updSql = 'UPDATE thong_tin_xe SET rating = ?, reviews = ? WHERE id = ?';
    db.query(updSql, [avg, cnt, carId], (updErr) => {
      if (updErr) {
        console.error('Lỗi khi cập nhật thống kê xe trong thong_tin_xe:', updErr);
        return res.status(500).json({ message: 'Lỗi khi cập nhật thống kê' });
      }

      return res.json({ success: true, stats: { avg, count: cnt } });
    });
  });
};