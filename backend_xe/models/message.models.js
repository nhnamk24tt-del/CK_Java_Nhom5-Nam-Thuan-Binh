const sql = require('../db');

const Message = {};

Message.createTableIfNotExists = (callback) => {
  const query = `
    CREATE TABLE IF NOT EXISTS messages (
      id INT AUTO_INCREMENT PRIMARY KEY,
      user_id INT NULL,
      sender_name VARCHAR(255) NOT NULL,
      sender_email VARCHAR(255) NOT NULL,
      sender_phone VARCHAR(50) NULL,
      subject VARCHAR(255) NOT NULL,
      message TEXT NOT NULL,
      status ENUM('new','read','replied') NOT NULL DEFAULT 'new',
      admin_reply TEXT NULL,
      admin_reply_at DATETIME NULL,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
  `;

  sql.query(query, callback);
};

Message.create = (messageData, result) => {
  const insertSql = `
    INSERT INTO messages (user_id, sender_name, sender_email, sender_phone, subject, message)
    VALUES (?, ?, ?, ?, ?, ?)
  `;
  const params = [
    messageData.user_id || null,
    messageData.name,
    messageData.email,
    messageData.phone || null,
    messageData.subject,
    messageData.message
  ];

  console.log('[messages] INSERT SQL:', insertSql.replace(/\s+/g,' '));
  console.log('[messages] params:', params);

  sql.query(insertSql, params, (err, res) => {
    if (err) {
      console.error('Lỗi khi tạo tin nhắn liên hệ:', err);
      // Attach params to error message temporarily for debugging
      const debugErr = new Error(err.message + ' -- params: ' + JSON.stringify(params));
      debugErr.code = err.code;
      return result(debugErr, null);
      return;
    }
    Message.getById(res.insertId, result);
  });
};

Message.getAll = (result) => {
  const query = `SELECT * FROM messages ORDER BY created_at DESC`;
  sql.query(query, (err, res) => {
    if (err) {
      console.error('Lỗi lấy danh sách tin nhắn:', err);
      result(err, null);
      return;
    }
    result(null, res);
  });
};

Message.getById = (id, result) => {
  const query = `SELECT * FROM messages WHERE id = ? LIMIT 1`;
  sql.query(query, [id], (err, res) => {
    if (err) {
      console.error('Lỗi lấy tin nhắn theo id:', err);
      result(err, null);
      return;
    }
    if (!res.length) {
      result({ kind: 'not_found' }, null);
      return;
    }
    result(null, res[0]);
  });
};

Message.getByEmail = (email, result) => {
  const query = `SELECT * FROM messages WHERE sender_email = ? ORDER BY created_at DESC`;
  sql.query(query, [email], (err, res) => {
    if (err) {
      console.error('Lỗi lấy tin nhắn theo email:', err);
      result(err, null);
      return;
    }
    result(null, res);
  });
};

Message.replyToMessage = (id, responseText, result) => {
  const query = `
    UPDATE messages
    SET admin_reply = ?, status = 'replied', admin_reply_at = NOW()
    WHERE id = ?
  `;
  sql.query(query, [responseText, id], (err, res) => {
    if (err) {
      console.error('Lỗi khi cập nhật phản hồi tin nhắn:', err);
      result(err, null);
      return;
    }
    if (res.affectedRows === 0) {
      result({ kind: 'not_found' }, null);
      return;
    }
    Message.getById(id, result);
  });
};

Message.markRead = (id, result) => {
  const query = `UPDATE messages SET status = 'read' WHERE id = ?`;
  sql.query(query, [id], (err, res) => {
    if (err) {
      console.error('Lỗi khi đánh dấu đã đọc:', err);
      result(err, null);
      return;
    }
    if (res.affectedRows === 0) {
      result({ kind: 'not_found' }, null);
      return;
    }
    Message.getById(id, result);
  });
};

module.exports = Message;
