const sql = require('../db');

const Favorite = {};

Favorite.getByUserId = (userId, result) => {
  sql.query(
    `SELECT car_id FROM favorites WHERE user_id = ? ORDER BY created_at DESC`,
    [userId],
    (err, res) => {
      if (err) {
        console.error('Lỗi lấy danh sách yêu thích:', err);
        result(err, null);
        return;
      }
      result(null, res);
    }
  );
};

Favorite.add = (userId, carId, result) => {
  sql.query(
    `INSERT IGNORE INTO favorites (user_id, car_id, created_at) VALUES (?, ?, NOW())`,
    [userId, carId],
    (err, res) => {
      if (err) {
        console.error('Lỗi thêm yêu thích:', err);
        result(err, null);
        return;
      }
      result(null, { user_id: userId, car_id: carId });
    }
  );
};

Favorite.remove = (userId, carId, result) => {
  sql.query(
    `DELETE FROM favorites WHERE user_id = ? AND car_id = ?`,
    [userId, carId],
    (err, res) => {
      if (err) {
        console.error('Lỗi xóa yêu thích:', err);
        result(err, null);
        return;
      }
      result(null, { user_id: userId, car_id: carId });
    }
  );
};

module.exports = Favorite;
