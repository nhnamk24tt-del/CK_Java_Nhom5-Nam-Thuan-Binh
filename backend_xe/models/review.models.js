const sql = require('../db');

const Review = {};

Review.createTableIfNotExists = (callback) => {
  const query = `
    CREATE TABLE IF NOT EXISTS reviews (
      id INT AUTO_INCREMENT PRIMARY KEY,
      car_id INT NOT NULL,
      user_id INT NULL,
      user_name VARCHAR(255) NULL,
      user_email VARCHAR(255) NULL,
      rating INT NOT NULL,
      comment TEXT NULL,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
  `;

  sql.query(query, callback);
};

Review.create = (reviewData, result) => {
  // 🔥 TỐI ƯU: Đảm bảo lấy đúng trường, phòng hờ cả trường hợp bên React gửi ID viết hoa (user.ID)
  const car_id = reviewData.car_id;
  const user_id = reviewData.user_id || reviewData.userID || null;
  const user_name = reviewData.user_name || null;
  const user_email = reviewData.user_email || null;
  const rating = reviewData.rating;
  const comment = reviewData.comment || null;

  const insertSql = `INSERT INTO reviews (car_id, user_id, user_name, user_email, rating, comment) VALUES (?, ?, ?, ?, ?, ?)`;
  
  sql.query(insertSql, [car_id, user_id, user_name, user_email, rating, comment], (err, res) => {
    if (err) {
      console.error('Lỗi khi tạo review trong cơ sở dữ liệu:', err);
      result(err, null);
      return;
    }
    // Lấy lại dòng vừa chèn để trả về giao diện hiển thị ngay lập tức
    Review.findById(res.insertId, result);
  });
};

Review.findById = (id, result) => {
  const query = `SELECT * FROM reviews WHERE id = ? LIMIT 1`;
  sql.query(query, [id], (err, res) => {
    if (err) {
      console.error('Lỗi khi lấy review theo id:', err);
      result(err, null);
      return;
    }
    result(null, res[0]);
  });
};

Review.findByCarId = (carId, result) => {
  const query = `SELECT * FROM reviews WHERE car_id = ? ORDER BY created_at DESC`;
  sql.query(query, [carId], (err, res) => {
    if (err) {
      console.error('Lỗi khi lấy review theo car_id:', err);
      result(err, null);
      return;
    }
    result(null, res);
  });
};

module.exports = Review;