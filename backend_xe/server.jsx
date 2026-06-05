const express = require("express");
const bodyParser = require('body-parser');
const methodOverride = require('method-override');
const session = require('express-session');
const cors = require('cors'); // Cho phép gọi API chéo cổng (React 5173 -> Node 3000)
const fs = require('fs');
const path = require('path');
require('dotenv/config');
const db = require('./db');

const app = express();

// --- 1. CẤU HÌNH MIDDLEWARE ---
app.use(cors()); // Kích hoạt CORS đầu tiên để tránh lỗi chặn trình duyệt từ React
app.use(bodyParser.urlencoded({ extended: true }));
app.set('view engine', 'ejs');
app.set('views', './views');

app.use(express.static('public'));
// Serve admin UI files from /admin
app.use('/admin', express.static('admin'));
app.use(express.json()); // Đảm bảo nhận được dữ liệu JSON từ React gửi lên khi bấm đặt xe
app.use(express.urlencoded({ extended: true }));

// Cấu hình Method Override để xử lý PUT/DELETE cho các form HTML/EJS cũ
app.use(methodOverride('_method', { methods: ['POST', 'GET'] }));
app.use(methodOverride(function (req, res) {
    if (req.body && typeof req.body === 'object' && '_method' in req.body) {
        var method = req.body._method;
        delete req.body._method;
        return method;
    }
}));

// Cấu hình Session
app.use(session({
    secret: 'mySecretKey',
    resave: false,
    saveUninitialized: true
}));

// --- 2. KẾT NỐI DATABASE CHUNG ---
// Kết nối được quản lý bởi backend_xe/db.js để dùng chung cho tất cả các route

// --- 3. CÁC API TRUY VẤN XE TRỰC TIẾP ---

// API 1: Lấy danh sách toàn bộ xe
app.get('/api/cars', (req, res) => {
    const sql = "SELECT * FROM thong_tin_xe";
    db.query(sql, (err, data) => {
        if (err) return res.status(500).json({ error: err.message });
        return res.json(data);
    });
});

// API 2: Lấy thông tin chi tiết của 1 chiếc xe theo ID
app.get('/api/cars/:id', (req, res) => {
    const carId = req.params.id;
    const sql = "SELECT * FROM thong_tin_xe WHERE id = ?";
    
    db.query(sql, [carId], (err, data) => {
        if (err) return res.status(500).json({ error: err.message });
        if (data.length === 0) return res.status(404).json({ message: "Không tìm thấy xe" });
        return res.json(data[0]);
    });
});

// --- 4. ĐIỀU HƯỚNG ROUTER TRANG CHỦ ---
app.get('/', (req, res, next) => {
    res.redirect('/api/cars');
});

// --- 4.1. Khởi tạo bảng review nếu cần ---
const Review = require('./models/review.models');
Review.createTableIfNotExists((err) => {
    if (err) {
        console.error('Không thể khởi tạo bảng reviews:', err);
    } else {
        console.log('Bảng reviews đã sẵn sàng.');
    }
});

// --- 4.2. Khởi tạo bảng messages nếu cần ---
const Message = require('./models/message.models');
Message.createTableIfNotExists((err) => {
    if (err) {
        console.error('Không thể khởi tạo bảng messages:', err);
    } else {
        console.log('Bảng messages đã sẵn sàng.');
    }
});

// --- 4.3. Chuẩn bị thư mục lưu avatar và bảng favorites nếu cần ---
const avatarDir = path.join(__dirname, 'public', 'avatars');
if (!fs.existsSync(avatarDir)) {
  fs.mkdirSync(avatarDir, { recursive: true });
}

db.query(
  `CREATE TABLE IF NOT EXISTS favorites (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    car_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_user_car (user_id, car_id)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;`,
  (err) => {
    if (err) {
      console.error('Không thể khởi tạo bảng favorites:', err);
    } else {
      console.log('Bảng favorites đã sẵn sàng.');
    }
  }
);

// --- 5. NẠP CÁC FILE ROUTE HỆ THỐNG (MVC) ---
require('./route/auth.route.js')(app, db); // Kích hoạt tuyến đường auth
require('./route/car.route.js')(app); // Kích hoạt tuyến đường route xe mới
require('./route/review.route.js')(app); // Kích hoạt route review
require('./route/booking.route.js')(app); // Kích hoạt route booking (yêu cầu thuê xe)
require('./route/message.route.js')(app); // Kích hoạt route tin nhắn liên hệ
require('./route/favorites.route.js')(app, db); // Kích hoạt route favorites

// --- 6. KHỞI CHẠY SERVER BACKEND ---
const PORT = 3000; // Chạy trên cổng 3000 để các tính năng cũ hoạt động bình thường
app.listen(PORT, function() {
    console.log(`===================================================`);
    console.log(` Server đang chạy thành công tại: http://localhost:${PORT}`);
    console.log(`===================================================`);
});     