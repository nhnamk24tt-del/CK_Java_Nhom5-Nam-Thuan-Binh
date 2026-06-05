exports.isAuthenticated = (req, res, next) => {
  // Kiểm tra nếu người dùng đã đăng nhập
  const activeUser = req.body.activeUser;

  if (activeUser && activeUser.id) {
    delete req.body.activeUser;
    next(); // Nếu có activeUser với id -> Cho phép đi tiếp
  } else {
    return res.status(401).json({ success: false, message: 'Yêu cầu đăng nhập để thực hiện hành động này.' });
  }
};

exports.isAdmin = (req, res, next) => {
  // Lấy thông tin user đăng nhập được truyền lên từ Frontend thông qua body
  const activeUser = req.body.activeUser; 

  if (activeUser && activeUser.role === 'admin') {
    delete req.body.activeUser;
    next(); // Nếu đúng là admin trong DB -> Cho phép đi tiếp
  } else {
    return res.status(403).json({ success: false, message: 'Từ chối truy cập! Bạn không phải Admin.' });
  }
};