// models/userModel.js
import pool from '../config/db.js';

const findUserByEmail = async (email) => {
  const result = await pool.query(
    "SELECT * FROM users WHERE email = $1", 
    [email]
  );
  return result.rows[0]; // Trả về user nếu có, không thì trả về undefined
};

const createUser = async (name, email, hashedPassword) => {
  const result = await pool.query(
    "INSERT INTO users (name, email, password) VALUES ($1, $2, $3) RETURNING id, name, email",
    [name, email, hashedPassword]
  );
  return result.rows[0]; // Trả về thông tin user vừa tạo
};

const findUserById = async (id) => {
  const result = await pool.query(
    "SELECT id, name, email FROM users WHERE id = $1", 
    [id]
  );
  return result.rows[0]; // Trả về object user, hoặc undefined nếu không thấy
};

// 2. Cập nhật mật khẩu mới (Dùng cho Đổi/Quên mật khẩu)
const updatePassword = async (id, newHashedPassword) => {
  const result = await pool.query(
    "UPDATE users SET password = $1 WHERE id = $2 RETURNING id, email",
    [newHashedPassword, id]
  );
  return result.rows[0];
};

const deleteUser = async (id) => {
  const result = await pool.query(
    "DELETE FROM users WHERE id = $1 RETURNING id",
    [id]
  );
  return result.rows[0]; // Trả về id để xác nhận đã xóa thành công
};

// Cập nhật thông tin người dùng theo email (Không cho phép đổi email)
const updateUserByEmail = async (email, updateData) => {
  // 1. Tạo một bản sao của updateData để tránh thay đổi object gốc từ req.body
  const dataToUpdate = { ...updateData };

  // 2. Bảo mật: Xóa trường email khỏi danh sách cập nhật (nếu có)
  // Đảm bảo không ai có thể can thiệp sửa email qua hàm này
  delete dataToUpdate.email;

  const keys = Object.keys(dataToUpdate);
  
  // 3. Nếu sau khi xóa email mà không còn gì để cập nhật thì dừng lại
  if (keys.length === 0) return null; 

  // 4. Tạo chuỗi SET động, ví dụ: "name = $1, avatar = $2"
  const setString = keys
    .map((key, index) => `${key} = $${index + 1}`)
    .join(", ");
  
  const values = Object.values(dataToUpdate);
  
  // 5. Thêm email vào cuối mảng values để dùng làm điều kiện WHERE
  values.push(email);
  
  // WHERE email = $X (X là vị trí cuối cùng trong mảng values)
  const query = `
    UPDATE users 
    SET ${setString} 
    WHERE email = $${values.length} 
    RETURNING id, name, email
  `;
  
  const result = await pool.query(query, values);
  return result.rows[0]; // Trả về thông tin user sau khi update, hoặc undefined nếu sai email
};

// updateData là một object, VD: { name: "Tên Mới", email: "moi@email.com" }
const updateUserByID = async (id, updateData) => {
  const keys = Object.keys(updateData);
  if (keys.length === 0) return null; // Không có dữ liệu để cập nhật

  // Biến mảng key thành chuỗi "name = $1, email = $2"
  const setString = keys
    .map((key, index) => `${key} = $${index + 1}`)
    .join(", ");
  
  // Trích xuất các giá trị để nạp vào biến $1, $2...
  const values = Object.values(updateData);
  
  // ID sẽ nằm ở vị trí cuối cùng ($3, $4... tùy theo số lượng field cần update)
  values.push(id);
  
  const query = `UPDATE users SET ${setString} WHERE id = $${values.length} RETURNING id, name, email`;
  
  const result = await pool.query(query, values);
  return result.rows[0];
};

// Xóa tài khoản theo email (Hard delete)
const deleteUserByEmail = async (email) => {
  const query = "DELETE FROM users WHERE email = $1 RETURNING id, name, email";
  
  const result = await pool.query(query, [email]);
  
  // Trả về thông tin user đã bị xóa, hoặc undefined nếu không tìm thấy email đó
  return result.rows[0]; 
};

export { findUserByEmail, createUser, findUserById, updatePassword, deleteUser, updateUserByEmail, updateUserByID,deleteUserByEmail};