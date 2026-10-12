# Hướng dẫn sử dụng BaseModel và UserModel

Tài liệu này hướng dẫn cách sử dụng kiến trúc Model (dựa trên `BaseModel.js` và `userModel.js`) để tương tác với cơ sở dữ liệu (PostgreSQL) một cách nhanh chóng, sạch sẽ và tái sử dụng được.

## 1. Giới thiệu chung

*   **`BaseModel.js`**: Là một class nền tảng (Base Class). Nó cung cấp sẵn các phương thức CRUD cơ bản (Create, Read, Update, Delete) giúp tự động sinh ra các câu lệnh SQL dựa trên object bạn truyền vào.
*   **`userModel.js`**: Kế thừa từ `BaseModel`, đại diện riêng cho bảng `users`. Nơi này chứa các hàm đặc thù cho User (ví dụ: `safeUpdateUserByEmail`) và xuất ra một **instance duy nhất** để dùng chung trong toàn bộ app.

---

## 2. Cách import và sử dụng trong Controller/Router

Vì `userModel.js` đã export sẵn một instance, bạn chỉ cần import nó vào file cần dùng (ví dụ: `auth.js`):

```javascript
import UserModel from '../models/userModel.js';
```

---

## 3. Các thao tác (CRUD) qua UserModel

Dưới đây là cách gọi các phương thức có sẵn từ UserModel. Tất cả các phương thức này đều là bất đồng bộ (`async/await`).

### 3.1. Thêm mới dữ liệu (`create`)
Hàm `create(data)` nhận vào một object chứa dữ liệu cần insert. Tên key trong object phải trùng với tên cột trong database.

```javascript
// Ví dụ: Đăng ký người dùng mới
const newUser = await UserModel.create({
  name: "Nguyen Van A",
  email: "a@gmail.com",
  password: "hashed_password_here"
});

console.log(newUser); // Trả về object user vừa tạo (có kèm id)
```

### 3.2. Tìm một bản ghi (`findOne`)
Hàm `findOne(conditions)` nhận vào một object điều kiện. Các điều kiện sẽ được nối với nhau bằng toán tử `AND`.

```javascript
// Ví dụ: Tìm user bằng email (dùng trong Login)
const user = await UserModel.findOne({ email: "a@gmail.com" });

if (!user) {
  console.log("Không tìm thấy user");
}

// Ví dụ: Tìm user với nhiều điều kiện
const activeAdmin = await UserModel.findOne({ 
  role: "admin", 
  status: "active" 
});
```

### 3.3. Cập nhật dữ liệu (`update`)
Hàm `update(conditions, dataToUpdate)` cần 2 tham số:
1. `conditions`: Điều kiện để tìm bản ghi cần update (object).
2. `dataToUpdate`: Dữ liệu mới cần cập nhật (object).

```javascript
// Ví dụ: Đổi tên và mật khẩu của user có id là 1
const updatedUser = await UserModel.update(
  { id: 1 }, // Điều kiện (WHERE id = 1)
  { name: "Ten Moi", password: "new_hashed_password" } // Dữ liệu cập nhật (SET name = ..., password = ...)
);
```

### 3.4. Xóa dữ liệu (`remove`)
Hàm `remove(conditions)` nhận vào một object điều kiện và sẽ xóa (các) bản ghi khớp với điều kiện đó.

```javascript
// Ví dụ: Xóa user bằng id
const deletedUser = await UserModel.remove({ id: 5 });

// Ví dụ: Xóa bằng email
const deletedUserByEmail = await UserModel.remove({ email: "spam@gmail.com" });
```

### 3.5. Hàm tự viết thêm (`safeUpdateUserByEmail`)
Đây là một hàm custom được định nghĩa riêng trong `userModel.js` để bạn tham khảo cách mở rộng chức năng. Hàm này giúp update thông tin nhưng bảo vệ không cho phép thay đổi cột `email`.

```javascript
// Ví dụ: Cập nhật thông tin qua email, cố ý truyền email mới vào
const result = await UserModel.safeUpdateUserByEmail(
  "a@gmail.com", 
  { 
    name: "Tên update", 
    email: "email_moi_se_bi_loai_bo@gmail.com" 
  }
);
// Kết quả: Tên được đổi, nhưng email vẫn giữ nguyên.
```

---

## 4. Hướng dẫn tạo Model mới cho bảng khác

Khi dự án to lên, bạn có thêm bảng `products`, `posts`, v.v., bạn chỉ cần tạo file mới tương tự `userModel.js`. Không cần viết lại các câu SQL CRUD!

**Ví dụ tạo `ProductModel.js`:**

```javascript
import BaseModel from './BaseModel.js';

class ProductModel extends BaseModel {
  constructor() {
    super('products'); // Truyền tên bảng 'products' vào đây
  }

  // Viết thêm hàm custom cho Product nếu cần
  async findInStock() {
    // Tự viết raw query kết hợp db pool nếu hàm Base chưa đáp ứng đủ
  }
}

export default new ProductModel();
```