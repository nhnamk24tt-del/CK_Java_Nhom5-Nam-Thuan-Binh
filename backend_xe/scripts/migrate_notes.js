const db = require('../db');

const q = "SELECT id, notes FROM booking_requests WHERE (customer_name IS NULL OR customer_name = '') AND notes LIKE '%Tên khách:%SĐT:%'";

function parseNotes(notes) {
  if (!notes) return { name: null, phone: null };
  const parts = notes.split('|').map(p => p.trim());
  let name = null, phone = null;
  if (parts.length >= 1) {
    const m = parts[0].replace(/Tên khách[:\s]*/i, '').trim();
    if (m) name = m;
  }
  if (parts.length >= 2) {
    const m2 = parts[1].replace(/SĐT[:\s]*/i, '').trim();
    if (m2) phone = m2;
  }
  return { name, phone };
}

db.query(q, (err, rows) => {
  if (err) {
    console.error('Select error', err);
    db.end();
    process.exit(1);
  }
  if (!rows.length) {
    console.log('No rows to migrate.');
    db.end();
    return;
  }
  let updated = 0;
  const tasks = rows.map(row => {
    return new Promise((resolve, reject) => {
      const { name, phone } = parseNotes(row.notes);
      if (!name && !phone) return resolve();
      const sql = 'UPDATE booking_requests SET customer_name = ?, customer_phone = ? WHERE id = ?';
      db.query(sql, [name || '', phone || '', row.id], (uerr, ures) => {
        if (uerr) return reject(uerr);
        if (ures.affectedRows) updated++;
        resolve();
      });
    });
  });
  Promise.all(tasks).then(() => {
    console.log('Migration complete. Updated rows:', updated);
    db.query('SELECT id, customer_name, customer_phone, notes FROM booking_requests ORDER BY id DESC LIMIT 10', (serr, srows) => {
      if (serr) console.error('Select after update error', serr);
      else console.log(JSON.stringify(srows, null, 2));
      db.end();
    });
  }).catch(e => {
    console.error('Update error', e);
    db.end();
  });
});
