const Message = require('../models/message.models');

exports.create = (req, res) => {
  const { name, email, phone, subject, message, user_id } = req.body;

  if (!name || !email || !subject || !message) {
    return res.status(400).json({ success: false, message: 'Vui lòng cung cấp đầy đủ tên, email, chủ đề và nội dung.' });
  }

  const newMessage = {
    user_id: user_id || null,
    name: String(name).trim(),
    email: String(email).trim(),
    phone: phone ? String(phone).trim() : null,
    subject: String(subject).trim(),
    message: String(message).trim()
  };

  Message.create(newMessage, (err, data) => {
    if (err) {
      return res.status(500).json({ success: false, message: err.message || 'Có lỗi khi lưu tin nhắn.' });
    }
    return res.status(201).json({ success: true, message: 'Tin nhắn của bạn đã được gửi.', data });
  });
};

exports.findAll = (req, res) => {
  Message.getAll((err, data) => {
    if (err) {
      return res.status(500).json({ success: false, message: err.message || 'Lỗi khi tải tin nhắn.' });
    }
    return res.json({ success: true, data });
  });
};

exports.findOne = (req, res) => {
  const id = req.params.id;
  Message.getById(id, (err, data) => {
    if (err) {
      if (err.kind === 'not_found') {
        return res.status(404).json({ success: false, message: 'Không tìm thấy tin nhắn.' });
      }
      return res.status(500).json({ success: false, message: err.message });
    }
    return res.json({ success: true, data });
  });
};

exports.findByEmail = (req, res) => {
  const email = String(req.query.email || '').trim();
  if (!email) {
    return res.status(400).json({ success: false, message: 'Vui lòng cung cấp email để truy vấn tin nhắn.' });
  }

  Message.getByEmail(email, (err, data) => {
    if (err) {
      return res.status(500).json({ success: false, message: err.message || 'Lỗi khi tải tin nhắn.' });
    }
    return res.json({ success: true, data });
  });
};

exports.reply = (req, res) => {
  const id = req.params.id;
  const responseText = req.body.response_text || req.body.reply;

  if (!responseText || !String(responseText).trim()) {
    return res.status(400).json({ success: false, message: 'Vui lòng cung cấp nội dung phản hồi.' });
  }

  Message.replyToMessage(id, String(responseText).trim(), (err, data) => {
    if (err) {
      if (err.kind === 'not_found') {
        return res.status(404).json({ success: false, message: 'Không tìm thấy tin nhắn để phản hồi.' });
      }
      return res.status(500).json({ success: false, message: err.message });
    }
    return res.json({ success: true, message: 'Phản hồi đã được lưu.', data });
  });
};

exports.markRead = (req, res) => {
  const id = req.params.id;
  Message.markRead(id, (err, data) => {
    if (err) {
      if (err.kind === 'not_found') {
        return res.status(404).json({ success: false, message: 'Không tìm thấy tin nhắn.' });
      }
      return res.status(500).json({ success: false, message: err.message });
    }
    return res.json({ success: true, message: 'Tin nhắn đã được đánh dấu đã đọc.', data });
  });
};
