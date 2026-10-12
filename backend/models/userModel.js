// models/userModel.js
import BaseModel from './BaseModel.js';

class UserModel extends BaseModel {
  constructor() {
    // Truyền tên bảng 'users' vào class cha
    super('users'); 
  }

  // Bạn có thể viết thêm các hàm chỉ dành riêng cho User ở đây
  // Ví dụ: hàm bảo mật không cho phép update email
  async safeUpdateUserByEmail(email, updateData) {
    const data = { ...updateData };
    delete data.email; // Đảm bảo email không bị sửa
    return this.update({ email }, data);
  }
}

// Xuất ra một instance duy nhất (Singleton pattern)
export default new UserModel();