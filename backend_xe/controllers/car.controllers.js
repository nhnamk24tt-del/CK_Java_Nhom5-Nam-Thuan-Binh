const Car = require("../models/car.models.js");

// Lấy toàn bộ danh sách xe
exports.findAll = (req, res) => {
  Car.getAll((err, data) => {
    if (err) return res.status(500).json({ success: false, message: err.message });
    return res.json(data);
  });
};

// Lấy chi tiết 1 xe
exports.findOne = (req, res) => {
  Car.getById(req.params.id, (err, data) => {
    if (err) {
      if (err.kind === "not_found") return res.status(404).json({ success: false, message: "Không tìm thấy xe yêu cầu" });
      return res.status(500).json({ success: false, message: err.message });
    }
    return res.json(data);
  });
};

// Đặt xe
exports.book = (req, res) => {
  Car.bookCar(req.params.id, (err, data) => {
    if (err) {
      if (err.kind === "not_available") {
        return res.status(400).json({ success: false, message: "Mẫu xe này hiện tại đã hết sạch xe trong kho!" });
      }
      return res.status(500).json({ success: false, message: err.message || "Lỗi nội bộ server" });
    }
    return res.status(200).json({ success: true, message: "Đặt xe thành công!", data: data });
  });
};

// Thêm xe mới
exports.create = (req, res) => {
  if (!req.body || Object.keys(req.body).length === 0) {
    return res.status(400).json({ success: false, message: "Dữ liệu thêm xe không được để trống!" });
  }

  const rawAvailable = req.body.available;
  const available =
    rawAvailable === false ||
    rawAvailable === 'false' ||
    rawAvailable === 0 ||
    rawAvailable === '0'
      ? false
      : rawAvailable === true ||
        rawAvailable === 'true'
      ? true
      : true;

  const newCar = {
  name: req.body.name,
  type: req.body.type,
  brand: req.body.brand,
  price: req.body.price,
  seats: req.body.seats || 5,
  transmission: req.body.transmission || "Tự động",
  fuel: req.body.fuel || "Xăng",
  available,
  year: req.body.year || 2026,
  color: req.body.color || "",
  description: req.body.description || "",
  features: req.body.features || "",
  quantity: req.body.quantity || 10,
  rating: 0.0,
  reviews: 0,
  image_url: req.body.image_url || null   
  };


  Car.create(newCar, (err, data) => {
    if (err) return res.status(500).json({ success: false, message: err.message || "Có lỗi xảy ra khi thêm xe mới." });
    return res.status(201).json({ success: true, message: "Thêm xe mới thành công!", data: data });
  });
};

// Sửa xe
exports.update = (req, res) => {
  if (!req.body || Object.keys(req.body).length === 0) {
    return res.status(400).json({ success: false, message: "Dữ liệu cập nhật không được để trống!" });
  }

  const updateData = { ...req.body };
  delete updateData.activeUser;

  Car.updateById(req.params.id, updateData, (err, data) => {
    if (err) {
      if (err.kind === "not_found") {
        return res.status(404).json({ success: false, message: `Không tìm thấy xe với ID: ${req.params.id}` });
      }
      return res.status(500).json({ success: false, message: err.message || "Lỗi cập nhật dữ liệu xe." });
    }
    return res.json({ success: true, message: "Cập nhật thông tin xe thành công!", data: data });
  });
};

// Xóa xe
exports.delete = (req, res) => {
  Car.remove(req.params.id, (err, data) => {
    if (err) {
      if (err.kind === "not_found") {
        return res.status(404).json({ success: false, message: `Không tìm thấy xe với ID: ${req.params.id}` });
      }
      return res.status(500).json({ success: false, message: err.message || "Không thể xóa chiếc xe này." });
    }
    return res.json({ success: true, message: "Chiếc xe đã được xóa thành công!" });
  });
};
