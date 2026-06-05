const sql = require('../db');

const Booking = {};

// 1. Lấy tất cả yêu cầu thuê xe
Booking.getAll = (result) => {
  sql.query(`
    SELECT 
      b.id, 
      b.user_id, 
      u.name as user_name, 
      u.email as user_email,
      u.phone as user_phone,
      b.car_id, 
      c.name as car_name,
      b.start_date, 
      b.end_date, 
      b.total_price,
      b.pickup_location,
      b.status, 
      b.created_at,
      b.customer_name,
      b.customer_phone,
      b.notes
    FROM booking_requests b
    LEFT JOIN users u ON b.user_id = u.id
    LEFT JOIN thong_tin_xe c ON b.car_id = c.id
    ORDER BY b.created_at DESC
  `, (err, res) => {
    if (err) {
      console.error('Lỗi lấy danh sách yêu cầu thuê xe:', err);
      result(err, null);
      return;
    }
    result(null, res);
  });
};

Booking.getByUserId = (userId, result) => {
  sql.query(`
    SELECT 
      b.id, 
      b.user_id, 
      u.name as user_name, 
      u.email as user_email,
      u.phone as user_phone,
      b.car_id, 
      c.name as car_name,
      b.start_date, 
      b.end_date, 
      b.total_price,
      b.pickup_location,
      b.status, 
      b.created_at,
      b.customer_name,
      b.customer_phone,
      b.notes
    FROM booking_requests b
    LEFT JOIN users u ON b.user_id = u.id
    LEFT JOIN thong_tin_xe c ON b.car_id = c.id
    WHERE b.user_id = ?
    ORDER BY b.created_at DESC
  `, [userId], (err, res) => {
    if (err) {
      console.error('Lỗi lấy lịch sử thuê xe của người dùng:', err);
      result(err, null);
      return;
    }
    result(null, res);
  });
};

// 2. Lấy chi tiết một yêu cầu
Booking.getById = (id, result) => {
  sql.query(
    `SELECT * FROM booking_requests WHERE id = ?`,
    [id],
    (err, res) => {
      if (err) {
        console.error('Lỗi lấy chi tiết yêu cầu:', err);
        result(err, null);
        return;
      }
      if (res.length) {
        result(null, res[0]);
        return;
      }
      result({ kind: 'not_found' }, null);
    }
  );
};

// 3. Tạo yêu cầu thuê xe mới
Booking.create = (bookingData, result) => {
  sql.query(
    `INSERT INTO booking_requests 
    (user_id, car_id, start_date, end_date, total_price, pickup_location, status, notes, customer_name, customer_phone, created_at)
    VALUES (?, ?, ?, ?, ?, ?, 'pending', ?, ?, ?, NOW())`,
    [
      bookingData.user_id,
      bookingData.car_id,
      bookingData.start_date,
      bookingData.end_date,
      bookingData.total_price,
      bookingData.pickup_location || '',
      bookingData.notes || '',
      bookingData.customer_name || '',
      bookingData.customer_phone || ''
    ],
    (err, res) => {
      if (err) {
        console.error('Lỗi tạo yêu cầu:', err);
        result(err, null);
        return;
      }
      result(null, { id: res.insertId, ...bookingData });
    }
  );
};

// 4. Cập nhật trạng thái yêu cầu
Booking.updateStatus = (id, status, result) => {
  sql.query(
    `UPDATE booking_requests SET status = ? WHERE id = ?`,
    [status, id],
    (err, res) => {
      if (err) {
        console.error('Lỗi cập nhật trạng thái:', err);
        result(err, null);
        return;
      }
      if (res.affectedRows === 0) {
        result({ kind: 'not_found' }, null);
        return;
      }
      result(null, { id: id, status: status });
    }
  );
};

// 5. Xóa yêu cầu
Booking.remove = (id, result) => {
  sql.query(`DELETE FROM booking_requests WHERE id = ?`, [id], (err, res) => {
    if (err) {
      console.error('Lỗi xóa yêu cầu:', err);
      result(err, null);
      return;
    }
    if (res.affectedRows === 0) {
      result({ kind: 'not_found' }, null);
      return;
    }
    result(null, { message: 'Yêu cầu đã được xóa' });
  });
};

module.exports = Booking;
