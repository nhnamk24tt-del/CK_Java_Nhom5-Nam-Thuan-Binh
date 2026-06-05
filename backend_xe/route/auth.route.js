const crypto = require('crypto');
const fs = require('fs');
const path = require('path');
const router = require('express').Router();

const JWT_SECRET = process.env.JWT_SECRET || 'rentwheels_secret_key';
const TOKEN_EXPIRE_MS = 7 * 24 * 60 * 60 * 1000; // 7 ngày

const AVATAR_DIR = path.join(__dirname, '..', 'public', 'avatars');
if (!fs.existsSync(AVATAR_DIR)) {
  fs.mkdirSync(AVATAR_DIR, { recursive: true });
}

const saveAvatarDataUrl = async (dataUrl, userId) => {
  const matches = /^data:(image\/(png|jpeg|jpg|gif));base64,(.+)$/.exec(dataUrl);
  if (!matches) {
    throw new Error('Dữ liệu avatar không hợp lệ');
  }

  const mime = matches[1];
  const base64Data = matches[3];
  const extension = mime.split('/')[1] === 'jpeg' ? 'jpg' : mime.split('/')[1];
  const fileName = `avatar_${userId}_${Date.now()}.${extension}`;
  const filePath = path.join(AVATAR_DIR, fileName);

  await fs.promises.writeFile(filePath, Buffer.from(base64Data, 'base64'));
  return `/avatars/${fileName}`;
};

// ======================
// HASH PASSWORD
// ======================
const hashPassword = (password) => {
  return crypto
    .createHmac('sha256', JWT_SECRET)
    .update(password)
    .digest('hex');
};

// ======================
// CREATE TOKEN
// ======================
const signToken = (user) => {
  const payload = {
    id: user.id,
    email: user.email,
    role: user.role,
    exp: Date.now() + TOKEN_EXPIRE_MS
  };

  const payloadBase64 = Buffer
    .from(JSON.stringify(payload))
    .toString('base64url');

  const signature = crypto
    .createHmac('sha256', JWT_SECRET)
    .update(payloadBase64)
    .digest('base64url');

  return `${payloadBase64}.${signature}`;
};

// ======================
// VERIFY TOKEN
// ======================
const verifyToken = (token) => {

  if (!token) return null;

  const parts = token.split('.');

  if (parts.length !== 2) return null;

  const [payloadBase64, signature] = parts;

  const expectedSignature = crypto
    .createHmac('sha256', JWT_SECRET)
    .update(payloadBase64)
    .digest('base64url');

  if (
    !crypto.timingSafeEqual(
      Buffer.from(signature),
      Buffer.from(expectedSignature)
    )
  ) {
    return null;
  }

  try {

    const payload = JSON.parse(
      Buffer.from(payloadBase64, 'base64url').toString('utf8')
    );

    if (!payload.exp || payload.exp < Date.now()) {
      return null;
    }

    return payload;

  } catch {
    return null;
  }
};

const normalizeEmail = (email) => {
  return String(email || '').trim().toLowerCase();
};

// ======================
// EXPORT ROUTE
// ======================
module.exports = (app, db) => {

  // ======================
  // REGISTER
  // ======================
  router.post('/register', (req, res) => {

    const { fullName, name, email, phone, password } = req.body;

    const userName = (fullName || name || '').trim();

    const normalizedEmail = normalizeEmail(email);

    if (!userName || !normalizedEmail || !password) {
      return res.status(400).json({
        message: 'Vui lòng nhập đầy đủ thông tin.'
      });
    }

    const hashedPassword = hashPassword(password);

    const checkSql =
      'SELECT id FROM users WHERE email = ? LIMIT 1';

    db.query(checkSql, [normalizedEmail], (err, existingUsers) => {

      if (err) {
        return res.status(500).json({
          message: err.message
        });
      }

      if (existingUsers.length > 0) {
        return res.status(409).json({
          message: 'Email đã tồn tại.'
        });
      }

      const insertSql = `
        INSERT INTO users
        (name, email, password, phone, created_at)
        VALUES (?, ?, ?, ?, NOW())
      `;

      db.query(
        insertSql,
        [
          userName,
          normalizedEmail,
          hashedPassword,
          phone || null
        ],
        (insertErr, result) => {

          if (insertErr) {
            return res.status(500).json({
              message: insertErr.message
            });
          }

          const user = {
            id: result.insertId,
            name: userName,
            email: normalizedEmail,
            phone,
            avatar: null,
            role: 'user'
          };

          const accessToken = signToken(user);

          return res.json({
            success: true,
            message: 'Đăng ký thành công.',
            accessToken,
            user
          });
        }
      );
    });
  });

  // ======================
  // LOGIN USER
  // ======================
  router.post('/login', (req, res) => {

    const { email, password } = req.body;

    const normalizedEmail = normalizeEmail(email);

    if (!normalizedEmail || !password) {
      return res.status(400).json({
        message: 'Vui lòng nhập email và mật khẩu.'
      });
    }

    const selectSql = `
      SELECT *
      FROM users
      WHERE email = ?
      LIMIT 1
    `;

    db.query(selectSql, [normalizedEmail], (err, results) => {

      if (err) {
        return res.status(500).json({
          message: err.message
        });
      }

      if (results.length === 0) {
        return res.status(401).json({
          message: 'Sai email hoặc mật khẩu.'
        });
      }

      const userRecord = results[0];

      const hashedPassword = hashPassword(password);

      if (userRecord.password !== hashedPassword) {
        return res.status(401).json({
          message: 'Sai email hoặc mật khẩu.'
        });
      }

      const user = {
        id: userRecord.id,
        name: userRecord.name,
        email: userRecord.email,
        phone: userRecord.phone,
        avatar: userRecord.avatar || null,
        role: 'user'
      };

      const accessToken = signToken(user);

      return res.json({
        success: true,
        message: 'Đăng nhập thành công.',
        accessToken,
        user
      });
    });
  });

  // ======================
  // LOGIN ADMIN
  // ======================
  router.post('/login-admin', (req, res) => {

  const { email, password } = req.body;

  const normalizedEmail = normalizeEmail(email);

  if (!normalizedEmail || !password) {
    return res.status(400).json({
      message: 'Vui lòng nhập email và mật khẩu'
    });
  }

  const sql = `
    SELECT *
    FROM users
    WHERE email = ?
    AND role = 'admin'
    LIMIT 1
  `;

  db.query(sql, [normalizedEmail], (err, results) => {

    if (err) {
      return res.status(500).json({
        message: err.message
      });
    }

    if (results.length === 0) {
      return res.status(401).json({
        success: false,
        message: 'Tài khoản admin không tồn tại'
      });
    }

    const admin = results[0];

    const hashedPassword = hashPassword(password);

    if (admin.password !== hashedPassword) {
      return res.status(401).json({
        success: false,
        message: 'Sai mật khẩu admin'
      });
    }

    const accessToken = signToken({
      id: admin.id,
      email: admin.email,
      role: admin.role
    });

    return res.json({
      success: true,
      message: 'Đăng nhập admin thành công',
      accessToken,
      user: {
        id: admin.id,
        name: admin.name,
        email: admin.email,
        role: admin.role
      }
    });

  });
});

  // ======================
  // LOGOUT
  // ======================
  router.post('/logout', (_req, res) => {

    return res.json({
      success: true,
      message: 'Đăng xuất thành công.'
    });
  });

  // ======================
  // GET CURRENT USER
  // ======================
  router.get('/me', (req, res) => {

    const authHeader = req.headers.authorization || '';

    const token = authHeader.startsWith('Bearer ')
      ? authHeader.slice(7)
      : null;

    const payload = verifyToken(token);

    if (!payload) {
      return res.status(401).json({
        message: 'Token không hợp lệ.'
      });
    }

    const selectSql = `
      SELECT id, name, email, phone, address, gender, birthdate, drivers_license, avatar, role
      FROM users
      WHERE id = ?
      LIMIT 1
    `;

    db.query(selectSql, [payload.id], (err, results) => {
      if (err || results.length === 0) {
        return res.status(500).json({ message: 'Không thể lấy thông tin người dùng.' });
      }
      const user = results[0];
      return res.json(user);
    });
  });

  // ======================
  // UPDATE USER PROFILE
  // ======================
  router.put('/update-profile', async (req, res) => {
    console.log('=== UPDATE PROFILE REQUEST ===');
    console.log('Headers:', req.headers);
    console.log('Body:', req.body);
    
    const authHeader = req.headers.authorization || '';
    const token = authHeader.startsWith('Bearer ')
      ? authHeader.slice(7)
      : null;

    console.log('Token:', token ? 'Present' : 'Missing');

    const payload = verifyToken(token);
    console.log('Payload:', payload);

    if (!payload) {
      return res.status(401).json({
        message: 'Token không hợp lệ.'
      });
    }

    const {
      name,
      phone,
      address,
      gender,
      birthdate,
      drivers_license,
      avatar
    } = req.body;

    console.log('Update data:', { name, phone, address, gender, birthdate, drivers_license, avatar: avatar ? 'provided' : 'none' });

    let avatarPath = avatar;
    if (avatar && typeof avatar === 'string' && avatar.startsWith('data:')) {
      try {
        avatarPath = await saveAvatarDataUrl(avatar, payload.id);
      } catch (avatarError) {
        console.error('Avatar save error:', avatarError);
        return res.status(400).json({ success: false, message: 'Không thể lưu ảnh đại diện.' });
      }
    }

    const updateSql = `
      UPDATE users
      SET
        name = COALESCE(?, name),
        phone = COALESCE(?, phone),
        address = COALESCE(?, address),
        gender = COALESCE(?, gender),
        birthdate = COALESCE(?, birthdate),
        drivers_license = COALESCE(?, drivers_license),
        avatar = COALESCE(?, avatar)
      WHERE id = ?
    `;

    db.query(
      updateSql,
      [name, phone, address, gender, birthdate, drivers_license, avatarPath, payload.id],
      (err, result) => {
        if (err) {
          console.error('Update error:', err);
          return res.status(500).json({
            success: false,
            message: err.message
          });
        }

        console.log('Update result:', result);

        const selectSql = `
          SELECT id, name, email, phone, address, gender, birthdate, drivers_license, avatar, role
          FROM users
          WHERE id = ?
          LIMIT 1
        `;

        db.query(selectSql, [payload.id], (selectErr, results) => {
          if (selectErr || results.length === 0) {
            console.error('Select error:', selectErr);
            return res.status(500).json({
              success: false,
              message: 'Không thể lấy thông tin cập nhật.'
            });
          }

          const updatedUser = results[0];
          console.log('Updated user:', updatedUser);

          return res.json({
            success: true,
            message: 'Cập nhật thông tin thành công.',
            user: updatedUser
          });
        });
      }
    );
  });

  // ======================
  // USE ROUTER
  // ======================
  app.use('/api/auth', router);
};