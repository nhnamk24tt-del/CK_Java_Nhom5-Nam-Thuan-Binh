const Booking = require("../models/booking.models.js");
const Car = require("../models/car.models.js");

// Lấy tất cả yêu cầu thuê xe
exports.findAll = (req, res) => {
  Booking.getAll((err, data) => {
    if (err) return res.status(500).json({ success: false, message: err.message });
    return res.json(data);
  });
};

// Lấy lịch sử đặt xe của người dùng
exports.findByUserId = (req, res) => {
  const userId = req.query.user_id || req.body.user_id;
  if (!userId) {
    return res.status(400).json({ success: false, message: 'Thiếu user_id để truy xuất lịch sử thuê xe.' });
  }

  Booking.getByUserId(userId, (err, data) => {
    if (err) return res.status(500).json({ success: false, message: err.message });
    return res.json(data);
  });
};

// Lấy chi tiết một yêu cầu
exports.findOne = (req, res) => {
  Booking.getById(req.params.id, (err, data) => {
    if (err) {
      if (err.kind === "not_found") {
        return res.status(404).json({ success: false, message: "Không tìm thấy yêu cầu" });
      }
      return res.status(500).json({ success: false, message: err.message });
    }
    return res.json(data);
  });
};

// Tạo yêu cầu thuê xe mới
exports.create = (req, res) => {
  if (!req.body || Object.keys(req.body).length === 0) {
    return res.status(400).json({ success: false, message: "Dữ liệu yêu cầu không được để trống!" });
  }

  const newBooking = {
    user_id: req.body.user_id,
    car_id: req.body.car_id,
    start_date: req.body.start_date,
    end_date: req.body.end_date,
    total_price: req.body.total_price || 0,
    pickup_location: req.body.pickup_location || req.body.pickupLocation || "",
    customer_name: req.body.customer_name || req.body.name || "",
    customer_phone: req.body.customer_phone || req.body.phone || "",
    notes: req.body.notes || ""
  };

  Booking.create(newBooking, (err, data) => {
    if (err) {
      return res.status(500).json({ success: false, message: err.message || "Có lỗi xảy ra khi tạo yêu cầu." });
    }
    return res.status(201).json({ success: true, message: "Tạo yêu cầu thành công!", booking: data, data: data });
  });
};

// Duyệt yêu cầu (approve)
exports.approve = (req, res) => {
  const id = req.params.id;

  Booking.getById(id, (err, booking) => {
    if (err) {
      if (err.kind === "not_found") {
        return res.status(404).json({ success: false, message: `Không tìm thấy yêu cầu với ID: ${id}` });
      }
      return res.status(500).json({ success: false, message: err.message });
    }

    if (!booking || !booking.car_id) {
      return res.status(400).json({ success: false, message: 'Dữ liệu yêu cầu không hợp lệ.' });
    }

    Car.bookCar(booking.car_id, (carErr, carData) => {
      if (carErr) {
        if (carErr.kind === 'not_available') {
          return res.status(400).json({ success: false, message: 'Xe không còn sẵn trong kho để duyệt yêu cầu.' });
        }
        return res.status(500).json({ success: false, message: carErr.message || 'Lỗi khi cập nhật trạng thái xe.' });
      }

      Booking.updateStatus(id, 'approved', (statusErr, statusData) => {
        if (statusErr) {
          if (statusErr.kind === "not_found") {
            return res.status(404).json({ success: false, message: `Không tìm thấy yêu cầu với ID: ${id}` });
          }
          return res.status(500).json({ success: false, message: statusErr.message });
        }

        return res.json({
          success: true,
          message: "Duyệt yêu cầu thành công!",
          data: {
            booking: statusData,
            car: carData
          }
        });
      });
    });
  });
};

// Từ chối yêu cầu (reject)
exports.reject = (req, res) => {
  const id = req.params.id;
  
  Booking.updateStatus(id, 'rejected', (err, data) => {
    if (err) {
      if (err.kind === "not_found") {
        return res.status(404).json({ success: false, message: `Không tìm thấy yêu cầu với ID: ${id}` });
      }
      return res.status(500).json({ success: false, message: err.message });
    }
    return res.json({ success: true, message: "Từ chối yêu cầu thành công!", data: data });
  });
};

// Xóa yêu cầu
exports.delete = (req, res) => {
  Booking.remove(req.params.id, (err, data) => {
    if (err) {
      if (err.kind === "not_found") {
        return res.status(404).json({ success: false, message: `Không tìm thấy yêu cầu với ID: ${req.params.id}` });
      }
      return res.status(500).json({ success: false, message: err.message });
    }
    return res.json({ success: true, message: "Yêu cầu đã được xóa!" });
  });
};
