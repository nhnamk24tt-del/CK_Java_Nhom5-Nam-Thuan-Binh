const mysql = require('mysql2');
require('dotenv/config');

// Helper: strip surrounding quotes and trim
const strip = (v) => {
  if (!v || typeof v !== 'string') return v;
  const t = v.trim();
  return t.replace(/^"|"$/g, '').replace(/^'|'$/g, '');
};

const db = mysql.createConnection({
  host: strip(process.env.DB_HOST) || 'localhost',
  user: strip(process.env.DB_USER) || 'root',
  password: strip(process.env.DB_PASSWORD) || '',
  database: strip(process.env.DB_NAME) || 'xe',
});

db.connect((err) => {
  if (err) {
    console.error('Lỗi kết nối DB:', err.message || err);
    return;
  }
  console.log(`Đã kết nối MySQL thành công với cơ sở dữ liệu [${strip(process.env.DB_NAME) || 'xe'}]!`);
});

module.exports = db;
