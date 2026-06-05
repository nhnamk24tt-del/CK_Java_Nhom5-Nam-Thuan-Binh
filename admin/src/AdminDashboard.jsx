import React, { useState, useEffect } from 'react';
import './AdminDashboard.css'; 

const AdminDashboard = () => {
  const [isAdmin, setIsAdmin] = useState(false);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [cars, setCars] = useState([]);
  const [bookings, setBookings] = useState([]);
  const [messages, setMessages] = useState([]);
  const [selectedMessage, setSelectedMessage] = useState(null);
  const [replyText, setReplyText] = useState('');
  const [error, setError] = useState('');
  const [activeTab, setActiveTab] = useState('cars'); // 'cars' hoặc 'bookings' hoặc 'messages'
  
  // State form Thêm/Sửa xe
  const [formData, setFormData] = useState({ 
    name: '', 
    brand: '', 
    type: '', 
    price: '', 
    seats: 5, 
    transmission: 'Tự động', 
    fuel: 'Xăng',
    available: true,
    year: new Date().getFullYear(),
    color: '',
    quantity: 10,
    image_url: '',
    description: '' 
  });
  const [editingId, setEditingId] = useState(null);

  // 1. Kiểm tra nếu đã đăng nhập Admin từ trước (F5 lại trang không bị mất)
  useEffect(() => {
    const user = JSON.parse(localStorage.getItem('activeUser'));
    if (user && user.role === 'admin') {
      setIsAdmin(true);
      fetchCars();
      fetchBookings();
      fetchMessages();
    }
  }, []);

  // 2. Hàm xử lý ĐĂNG NHẬP ADMIN - Gọi trực tiếp sang Backend Node.js
  const handleAdminLogin = async (e) => {
    e.preventDefault();
    try {
      const response = await fetch('http://localhost:3000/api/auth/login-admin', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password })
      });
      const result = await response.json();

      if (result.success) {
        // Backend xác nhận đúng quyền admin -> Lưu vào localStorage
        localStorage.setItem('activeUser', JSON.stringify(result.user));
        setIsAdmin(true);
        fetchCars();
        fetchBookings();
        fetchMessages();
        setError('');
      } else {
        setError(result.message); // Hiển thị lỗi từ backend (Ví dụ: Không có quyền admin)
      }
    } catch (err) {
      setError('Lỗi kết nối đến Backend Node.js!');
    }
  };

  // 3. Hàm lấy danh sách xe từ Backend
  const fetchCars = async () => {
    try {
      const res = await fetch('http://localhost:3000/api/cars');
      const data = await res.json();
      setCars(data);
    } catch (err) {
      console.error("Lỗi lấy danh sách xe:", err);
    }
  };

  // 3.5. Hàm lấy danh sách yêu cầu thuê xe từ Backend
  const fetchBookings = async () => {
    try {
      const res = await fetch('http://localhost:3000/api/bookings');
      const data = await res.json();
      setBookings(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error("Lỗi lấy danh sách yêu cầu thuê xe:", err);
    }
  };

  const fetchMessages = async () => {
    try {
      const activeUser = JSON.parse(localStorage.getItem('activeUser'));
      if (!activeUser) return;
      const res = await fetch('http://localhost:3000/api/messages/list', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ activeUser })
      });
      const result = await res.json();
      if (result.success) {
        setMessages(Array.isArray(result.data) ? result.data : []);
      } else {
        console.error('Fetch messages failed', result.message);
      }
    } catch (err) {
      console.error("Lỗi lấy tin nhắn:", err);
    }
  };

  const openMessageDetail = async (id) => {
    try {
      const activeUser = JSON.parse(localStorage.getItem('activeUser'));
      const res = await fetch(`http://localhost:3000/api/messages/${id}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ activeUser })
      });
      const result = await res.json();
      if (result.success) {
        setSelectedMessage(result.data);
        setReplyText('');
      } else {
        alert(result.message || 'Không thể mở tin nhắn');
      }
    } catch (err) {
      console.error(err);
      alert('Lỗi khi mở tin nhắn');
    }
  };

  const handleSendReply = async () => {
    if (!selectedMessage) return;
    if (!replyText.trim()) {
      alert('Vui lòng nhập nội dung trả lời.');
      return;
    }

    try {
      const activeUser = JSON.parse(localStorage.getItem('activeUser'));
      const res = await fetch(`http://localhost:3000/api/messages/${selectedMessage.id}/reply`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ activeUser, response_text: replyText })
      });
      const result = await res.json();
      if (result.success) {
        alert('Reply saved');
        fetchMessages();
        openMessageDetail(selectedMessage.id);
      } else {
        alert(result.message || 'Error');
      }
    } catch (err) {
      console.error(err);
      alert('Lỗi khi gửi trả lời');
    }
  };

  const handleMarkRead = async () => {
    if (!selectedMessage) return;
    try {
      const activeUser = JSON.parse(localStorage.getItem('activeUser'));
      const res = await fetch(`http://localhost:3000/api/messages/${selectedMessage.id}/mark-read`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ activeUser })
      });
      const result = await res.json();
      if (result.success) {
        alert('Marked read');
        fetchMessages();
        openMessageDetail(selectedMessage.id);
      } else {
        alert(result.message || 'Error');
      }
    } catch (err) {
      console.error(err);
      alert('Lỗi khi đánh dấu đã đọc');
    }
  };

  // 4. Hàm Thêm hoặc Cập nhật thông tin xe thông qua API Backend
  const handleSaveCar = async (e) => {
    e.preventDefault();
    const activeUser = JSON.parse(localStorage.getItem('activeUser'));
    
    // Đường dẫn chuẩn cổng 3000 hệ thống đã bọc verifyAdmin
    const url = editingId 
      ? `http://localhost:3000/api/cars/admin/edit/${editingId}`
      : `http://localhost:3000/api/cars/admin/add`;
    const method = editingId ? 'PUT' : 'POST';

    try {
      const res = await fetch(url, {
        method: method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ ...formData, activeUser }) // Gửi kèm thông tin admin để backend kiểm tra
      });
      
      const result = await res.json();
      if (result.success) {
        alert(result.message);
        setFormData({ 
          name: '', 
          brand: '', 
          type: '', 
          price: '', 
          seats: 5, 
          transmission: 'Tự động', 
          fuel: 'Xăng',
          available: true,
          year: new Date().getFullYear(),
          color: '',
          quantity: 10,
          image_url: '',
          description: '' 
        });
        setEditingId(null);
        fetchCars(); // Reset lại bảng danh sách xe
      } else {
        alert(result.message);
      }
    } catch (err) {
      alert("Lỗi thao tác dữ liệu xe!");
    }
  };

  // 5. Hàm Xóa xe thông qua API Backend
  const handleDeleteCar = async (id) => {
    if (window.confirm('Bạn có chắc chắn muốn xóa chiếc xe này không?')) {
      const activeUser = JSON.parse(localStorage.getItem('activeUser'));
      try {
        const res = await fetch(`http://localhost:3000/api/cars/admin/delete/${id}`, {
          method: 'DELETE',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ activeUser })
        });
        const result = await res.json();
        if (result.success) {
          alert(result.message);
          fetchCars();
        } else {
          alert(result.message);
        }
      } catch (err) {
        alert("Lỗi khi xóa xe!");
      }
    }
  };

  // 6. Hàm duyệt yêu cầu thuê xe
  const handleApproveBooking = async (id) => {
    const activeUser = JSON.parse(localStorage.getItem('activeUser'));
    try {
      const res = await fetch(`http://localhost:3000/api/bookings/${id}/approve`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ activeUser, status: 'approved' })
      });
      const result = await res.json();
      if (result.success) {
        alert('Duyệt yêu cầu thành công!');
        fetchBookings();
      } else {
        alert(result.message);
      }
    } catch (err) {
      alert("Lỗi khi duyệt yêu cầu!");
    }
  };

  // 7. Hàm từ chối yêu cầu thuê xe
  const handleRejectBooking = async (id) => {
    const activeUser = JSON.parse(localStorage.getItem('activeUser'));
    try {
      const res = await fetch(`http://localhost:3000/api/bookings/${id}/reject`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ activeUser, status: 'rejected' })
      });
      const result = await res.json();
      if (result.success) {
        alert('Từ chối yêu cầu thành công!');
        fetchBookings();
      } else {
        alert(result.message);
      }
    } catch (err) {
      alert("Lỗi khi từ chối yêu cầu!");
    }
  };

  // --- GIAO DIỆN 1: FORM ĐĂNG NHẬP ADMIN ---
  if (!isAdmin) {
    return (
      <div className="login-admin-container">
        <div className="login-admin-card">
          <h2>🔒 HỆ THỐNG QUẢN TRỊ VIÊN</h2>
          <p>Vui lòng đăng nhập tài khoản CSDL có quyền Admin</p>
          {error && <div className="error-alert" style={{color: 'red', marginBottom: '10px'}}>{error}</div>}
          <form onSubmit={handleAdminLogin}>
            <div className="input-group">
              <label>Email Quản Trị</label>
              <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required placeholder="admin@example.com" />
            </div>
            <div className="input-group">
              <label>Mật Khẩu</label>
              <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required placeholder="••••••••" />
            </div>
            <button type="submit" className="btn-login-admin">Đăng Nhập Hệ Thống</button>
          </form>
        </div>
      </div>
    );
  }

  // --- GIAO DIỆN 2: DASHBOARD QUẢN LÝ XE ---
  return (
    <div className="admin-dashboard-layout">
      <div className="admin-sidebar">
        <div className="admin-logo">🚗 RENTCAR ADMIN</div>
        <ul className="sidebar-menu">
          <li 
            className={activeTab === 'cars' ? 'active' : ''} 
            onClick={() => setActiveTab('cars')}
          >
            📦 Quản lý xe
          </li>
          <li 
            className={activeTab === 'bookings' ? 'active' : ''} 
            onClick={() => setActiveTab('bookings')}
          >
            📋 Duyệt yêu cầu ({bookings.filter(b => b.status === 'pending').length})
          </li>
          <li 
            className={activeTab === 'messages' ? 'active' : ''} 
            onClick={() => { setActiveTab('messages'); fetchMessages(); }}
          >
            💬 Tin nhắn ({messages.length})
          </li>
          <li onClick={() => { localStorage.clear(); setIsAdmin(false); }}>🚪 Đăng xuất</li>
        </ul>
      </div>

      <div className="admin-main-content">
        <header className="main-header">
          <h1>{activeTab === 'cars' ? 'Quản Lý Phương Tiện' : activeTab === 'bookings' ? 'Duyệt Yêu Cầu Thuê Xe' : 'Tin Nhắn Khách Hàng'}</h1>
          <span className="badge-admin">Chào Admin, {JSON.parse(localStorage.getItem('activeUser'))?.name}</span>
        </header>

        {/* TAB QUẢN LÝ XE */}
        {activeTab === 'cars' && (
          <div className="dashboard-grid">
            <div className="card-form-car">
              <h3>{editingId ? '📝 Cập Nhật Xe' : '➕ Thêm Xe Mới'}</h3>
              <form onSubmit={handleSaveCar}>
                <div style={{display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px'}}>
                  <input type="text" placeholder="Tên xe *" value={formData.name} onChange={(e)=>setFormData({...formData, name: e.target.value})} required />
                  <input type="text" placeholder="Hãng xe *" value={formData.brand} onChange={(e)=>setFormData({...formData, brand: e.target.value})} required />
                  <input type="text" placeholder="Phân khúc *" value={formData.type} onChange={(e)=>setFormData({...formData, type: e.target.value})} required />
                  <input type="number" placeholder="Giá thuê/ngày *" value={formData.price} onChange={(e)=>setFormData({...formData, price: e.target.value})} required />
                  <input type="number" placeholder="Số chỗ ngồi" value={formData.seats} onChange={(e)=>setFormData({...formData, seats: e.target.value})} min="1" />
                  <select value={formData.transmission} onChange={(e)=>setFormData({...formData, transmission: e.target.value})}>
                    <option>Tự động</option>
                    <option>Số sàn</option>
                  </select>
                  <select value={formData.fuel} onChange={(e)=>setFormData({...formData, fuel: e.target.value})}>
                    <option>Xăng</option>
                    <option>Diesel</option>
                    <option>Điện</option>
                    <option>Hybrid</option>
                  </select>
                  <input type="number" placeholder="Năm sản xuất" value={formData.year} onChange={(e)=>setFormData({...formData, year: e.target.value})} />
                  <input type="text" placeholder="Màu sắc" value={formData.color} onChange={(e)=>setFormData({...formData, color: e.target.value})} />
                  <input type="number" placeholder="Số lượng" value={formData.quantity} onChange={(e)=>setFormData({...formData, quantity: e.target.value})} min="1" />
                  <input type="text" placeholder="Link ảnh hoặc đường dẫn tệp" value={formData.image_url} onChange={(e)=>setFormData({...formData, image_url: e.target.value})} />
                  <div style={{gridColumn: '1/-1'}}>
                    <input type="checkbox" id="available" checked={formData.available} onChange={(e)=>setFormData({...formData, available: e.target.checked})} />
                    <label htmlFor="available" style={{marginLeft: '5px'}}>Còn xe</label>
                  </div>
                </div>
                <textarea placeholder="Mô tả xe" value={formData.description} onChange={(e)=>setFormData({...formData, description: e.target.value})} style={{marginTop: '10px', width: '100%', minHeight: '80px'}}></textarea>
                <div style={{display: 'flex', gap: '10px', marginTop: '10px'}}>
                  <button type="submit" className="btn-submit-car">{editingId ? 'Cập Nhật Xe' : 'Thêm Xe Mới'}</button>
                  {editingId && <button type="button" className="btn-cancel" onClick={()=>setEditingId(null)}>Hủy</button>}
                </div>
              </form>
            </div>

            <div className="card-table-cars">
              <h3>Danh Sách Xe ({cars.length})</h3>
              <div style={{overflowX: 'auto'}}>
                <table className="beautiful-table">
                  <thead>
                    <tr>
                      <th>ID</th><th>Tên</th><th>Hãng</th><th>Phân khúc</th><th>Giá</th><th>Số lượng</th><th>Hành động</th>
                    </tr>
                  </thead>
                  <tbody>
                    {cars.map((car) => (
                      <tr key={car.id}>
                        <td>{car.id}</td>
                        <td><strong>{car.name}</strong></td>
                        <td>{car.brand}</td>
                        <td>{car.type}</td>
                        <td>{Number(car.price).toLocaleString()}đ</td>
                        <td>{car.quantity}</td>
                        <td>
                          <button className="btn-table-edit" onClick={() => { setEditingId(car.id); setFormData(car); }}>Sửa</button>
                          <button className="btn-table-delete" onClick={() => handleDeleteCar(car.id)}>Xóa</button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        )}

        {/* TAB DUYỆT YÊU CẦU */}
        {activeTab === 'bookings' && (
          <div className="card-table-cars">
            <h3>Yêu Cầu Thuê Xe ({bookings.length})</h3>
            <div style={{overflowX: 'auto'}}>
              <table className="beautiful-table">
                <thead>
                  <tr>
                    <th>ID</th><th>Khách Hàng</th><th>SĐT</th><th>Xe</th><th>Địa điểm nhận xe</th><th>Từ ngày</th><th>Đến ngày</th><th>Trạng thái</th><th>Hành động</th>
                  </tr>
                </thead>
                <tbody>
                  {bookings.map((booking) => (
                    <tr key={booking.id}>
                      <td>{booking.id}</td>
                      <td>{booking.customer_name || booking.user_name || 'N/A'}</td>
                      <td>{booking.customer_phone || booking.user_phone || 'N/A'}</td>
                      <td>{booking.car_name || 'N/A'}</td>
                      <td>{booking.pickup_location || booking.pickupLocation || 'N/A'}</td>
                      <td>{booking.start_date ? new Date(booking.start_date).toLocaleDateString('vi-VN') : 'N/A'}</td>
                      <td>{booking.end_date ? new Date(booking.end_date).toLocaleDateString('vi-VN') : 'N/A'}</td>
                      <td><span style={{padding: '5px 10px', borderRadius: '5px', backgroundColor: booking.status === 'pending' ? '#ffc107' : booking.status === 'approved' ? '#28a745' : '#dc3545', color: 'white'}}>{booking.status === 'pending' ? 'Chờ duyệt' : booking.status === 'approved' ? 'Đã duyệt' : 'Từ chối'}</span></td>
                      <td>
                        {booking.status === 'pending' && (
                          <>
                            <button className="btn-approve" onClick={() => handleApproveBooking(booking.id)}>Duyệt</button>
                            <button className="btn-reject" onClick={() => handleRejectBooking(booking.id)}>Từ chối</button>
                          </>
                        )}
                        {booking.status !== 'pending' && <span>—</span>}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {bookings.length === 0 && <p style={{textAlign: 'center', marginTop: '20px'}}>Không có yêu cầu nào</p>}
          </div>
        )}

        {activeTab === 'messages' && (
          <div className="messages-grid">
            <div className="message-list-card">
              <div className="card-header">
                <h3>Danh Sách Tin Nhắn ({messages.length})</h3>
                <button className="btn-approve" onClick={fetchMessages}>Làm mới</button>
              </div>
              <div className="message-table-wrapper">
                <table className="beautiful-table">
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Người gửi</th>
                      <th>Tiêu đề</th>
                      <th>Ngày</th>
                      <th>Trạng thái</th>
                      <th>Hành động</th>
                    </tr>
                  </thead>
                  <tbody>
                    {messages.map((message) => (
                      <tr key={message.id} className={selectedMessage?.id === message.id ? 'selected-row' : ''}>
                        <td>{message.id}</td>
                        <td>{message.sender_name || '-'}<br/><small>{message.sender_email || '-'}</small></td>
                        <td>{message.subject}</td>
                        <td>{message.created_at ? new Date(message.created_at).toLocaleString() : '-'}</td>
                        <td>{message.status || '-'}</td>
                        <td><button className="btn-table-edit" onClick={() => openMessageDetail(message.id)}>Xem</button></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                {messages.length === 0 && <p style={{textAlign:'center', marginTop:'20px'}}>Không có tin nhắn</p>}
              </div>
            </div>

            <div className="message-detail-card">
              <h3>Chi Tiết Tin Nhắn</h3>
              {selectedMessage ? (
                <div className="message-detail-content">
                  <div className="message-meta">
                    <p><strong>From:</strong> {selectedMessage.sender_name || '-'} &lt;{selectedMessage.sender_email || '-'}&gt;</p>
                    <p><strong>Phone:</strong> {selectedMessage.sender_phone || '-'}</p>
                    <p><strong>Subject:</strong> {selectedMessage.subject || '-'}</p>
                    <p><strong>Ngày:</strong> {selectedMessage.created_at ? new Date(selectedMessage.created_at).toLocaleString() : '-'}</p>
                    <p><strong>Trạng thái:</strong> {selectedMessage.status || '-'}</p>
                  </div>
                  <div className="message-body">
                    <p>{selectedMessage.message || '-'}</p>
                  </div>
                  <div className="admin-reply">
                    <label><strong>Trả lời admin</strong></label>
                    <textarea value={replyText} onChange={(e) => setReplyText(e.target.value)} placeholder="Nhập nội dung trả lời..."></textarea>
                    <div className="reply-actions">
                      <button className="btn-approve" onClick={handleMarkRead}>Đánh dấu đã đọc</button>
                      <button className="btn-table-edit" onClick={handleSendReply}>Gửi trả lời</button>
                    </div>
                  </div>
                </div>
              ) : (
                <div className="message-empty">Chọn một tin nhắn bên trái để xem chi tiết.</div>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default AdminDashboard;