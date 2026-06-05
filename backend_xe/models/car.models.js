const sql = require('../db');

const Car = {};

// Helper: pick first matching candidate from keys
const pickColumn = (keys, candidates) => candidates.find(c => keys.includes(c));

// 1. Lấy toàn bộ danh sách xe
Car.getAll = (result) => {
  sql.query('SELECT * FROM thong_tin_xe', (err, res) => {
    if (err) {
      console.error('Lỗi lấy danh sách xe:', err);
      result(err, null);
      return;
    }
    result(null, res);
  });
};

// 2. Lấy chi tiết một xe
Car.getById = (id, result) => {
  sql.query('SELECT * FROM thong_tin_xe WHERE id = ?', [id], (err, res) => {
    if (err) {
      console.error('Lỗi lấy chi tiết xe:', err);
      result(err, null);
      return;
    }
    if (res.length) {
      result(null, res[0]);
      return;
    }
    result({ kind: 'not_found' }, null);
  });
};

// 3. Đặt xe (giảm số lượng và cập nhật trạng thái available)
Car.bookCar = (id, result) => {
  sql.query('SELECT * FROM thong_tin_xe WHERE id = ?', [id], (err, rows) => {
    if (err) {
      console.error('Lỗi MySQL khi lấy chi tiết xe trước khi đặt:', err);
      result(err, null);
      return;
    }

    if (!rows || rows.length === 0) {
      result({ kind: 'not_found' }, null);
      return;
    }

    const row = rows[0];
    const keys = Object.keys(row);

    const qtyCandidates = ['quantity', 'so_luong', 'soluong', 'sl', 'so_luong_xe'];
    const availCandidates = ['available', 'trang_thai', 'tinh_trang', 'status'];

    const qtyCol = pickColumn(keys, qtyCandidates);
    const availCol = pickColumn(keys, availCandidates) || 'available';

    if (!qtyCol) {
      const msg = 'Không tìm thấy cột quantity/so_luong trong bảng thong_tin_xe';
      console.error(msg, 'columns=', keys);
      result(new Error(msg), null);
      return;
    }

    const currentQty = Number(row[qtyCol] || 0);
    if (currentQty <= 0) {
      result({ kind: 'not_available' }, null);
      return;
    }

    const updateSql = `UPDATE thong_tin_xe SET \`${qtyCol}\` = \`${qtyCol}\` - 1, \`${availCol}\` = IF(\`${qtyCol}\` - 1 > 0, 1, 0) WHERE id = ? AND \`${qtyCol}\` > 0`;
    sql.query(updateSql, [id], (err2, res2) => {
      if (err2) {
        console.error('Lỗi MySQL khi trừ số lượng xe:', err2);
        result(err2, null);
        return;
      }

      if (res2.affectedRows === 0) {
        result({ kind: 'not_available' }, null);
        return;
      }

      const selectSql = `SELECT \`${availCol}\` AS available, \`${qtyCol}\` AS quantity FROM thong_tin_xe WHERE id = ?`;
      sql.query(selectSql, [id], (errSelect, currentCar) => {
        if (errSelect) {
          console.error('Lỗi MySQL khi lấy kết quả sau update:', errSelect);
          result(errSelect, null);
          return;
        }

        if (currentCar && currentCar.length > 0) {
          const qty = Number(currentCar[0].quantity || 0);
          const availFlag = currentCar[0].available === 1 || currentCar[0].available === true || currentCar[0].available === 'Còn xe';
          result(null, {
            id: id,
            available: qty > 0 || availFlag,
            quantity: qty
          });
        } else {
          result(null, { id: id });
        }
      });
    });
  });
};

// 4. Thêm xe mới
Car.create = (newCar, result) => {
  sql.query("INSERT INTO thong_tin_xe SET ?", newCar, (err, res) => {
    if (err) {
      console.error("Lỗi thêm xe:", err);
      result(err, null);
      return;
    }
    result(null, { id: res.insertId, ...newCar });
  });
};

// 5. Cập nhật xe theo ID
Car.updateById = (id, car, result) => {
  const updateData = { ...car };
  delete updateData.activeUser;

  sql.query("UPDATE thong_tin_xe SET ? WHERE id = ?", [updateData, id], (err, res) => {
    if (err) {
      console.error("Lỗi cập nhật xe:", err);
      result(err, null);
      return;
    }
    if (res.affectedRows === 0) {
      result({ kind: "not_found" }, null);
      return;
    }
    result(null, { id: id, ...updateData });
  });
};

// 6. Xóa xe theo ID
Car.remove = (id, result) => {
  sql.query("DELETE FROM thong_tin_xe WHERE id = ?", [id], (err, res) => {
    if (err) {
      console.error("Lỗi xóa xe:", err);
      result(err, null);
      return;
    }
    if (res.affectedRows === 0) {
      result({ kind: "not_found" }, null);
      return;
    }
    result(null, res);
  });
};

module.exports = Car;
