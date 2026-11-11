## Chạy backend và frontend

Trước khi chạy, cấu hình `.env` ở thư mục gốc và cài dependencies (`uv sync --extra dev`). Mở hai terminal PowerShell riêng tại thư mục gốc repository.

Terminal 1 - backend:

```powershell
uv run uvicorn zalo_backend.main:app --app-dir backend/src --host 127.0.0.1 --port 8000 --reload
```

Chờ tới khi thấy `Application startup complete`. Giữ terminal này mở. Tài liệu API nằm tại http://127.0.0.1:8000/docs.

Terminal 2 - frontend:

```powershell
cd frontend
npm install
npm run dev
```

Mở http://localhost:5173. Sau lần cài đầu tiên, có thể bỏ qua `npm install` ở các lần chạy sau. Dừng mỗi server bằng `Ctrl+C` trong terminal tương ứng.

##  Đăng ký, đăng nhập và chat

1. Tại http://localhost:5173, đăng ký tài khoản A bằng tên, email và mật khẩu dài ít nhất 6 ký tự.
2. Đăng xuất rồi đăng nhập lại với tài khoản A để kiểm tra luồng đăng nhập.
3. Mở một cửa sổ ẩn danh tại cùng địa chỉ và đăng ký tài khoản B.
4. Trong Contacts, chọn tài khoản còn lại và gửi tin nhắn. Gửi phản hồi từ cửa sổ kia để kiểm tra chat hai chiều và realtime.
5. Gửi ảnh hoặc đổi avatar chỉ hoạt động khi đã cấu hình `CLOUDINARY_URL`; nhắn tin văn bản không cần Cloudinary.

Các thao tác chính được thực hiện qua API:

| Chức năng | Endpoint |
| --- | --- |
| Đăng ký | `POST /api/auth/signup` |
| Đăng nhập | `POST /api/auth/login` |
| Đăng xuất | `POST /api/auth/logout` |
| Kiểm tra phiên | `GET /api/auth/check` |
| Danh bạ | `GET /api/messages/contacts` |
| Người đã chat | `GET /api/messages/chats` |
| Lịch sử chat | `GET /api/messages/{userId}` |
| Gửi tin nhắn | `POST /api/messages/send/{userId}` |

`GET /api/auth/check` trả `401` khi chưa đăng nhập là bình thường. Giao diện đăng nhập dùng cookie JWT httpOnly để duy trì phiên.

##  Kiểm tra dữ liệu đã lưu

### PostgreSQL: tài khoản

Trong WSL, mở `psql` với đúng tên database từ `POSTGRES_DATABASE_URL` (ví dụ database tên `users`):

```bash
sudo -u postgres psql -d users
```

Sau đó chạy:

```sql
SELECT id, username, email, profile_pic,
       LEFT(password_hash, 20) AS password_hash_preview
FROM users
ORDER BY id;
```

Tài khoản vừa đăng ký cần xuất hiện; mật khẩu được lưu dưới dạng hash. Thoát `psql` bằng `Ctrl+D`.

### MongoDB Atlas: tin nhắn

Trong Atlas, mở **Data Explorer → chat_db → messages**. Sau khi gửi tin nhắn, document sẽ có các trường như `senderId`, `receiverId`, `text`, `image`, `createdAt` và `updatedAt`. Lọc theo `senderId` hoặc `receiverId` để tìm tin nhắn của hai tài khoản.

## 6. Kiểm tra frontend

Có thể dừng dev server bằng `Ctrl+C`, rồi chạy build/lint trong thư mục `frontend`:

```powershell
npm run build
npm run lint
```

## Xử lý lỗi thường gặp

- **`No module named 'zalo_backend'`:** chạy lệnh Uvicorn từ thư mục gốc với `--app-dir backend/src`.
- **PostgreSQL báo sai mật khẩu/user:** kiểm tra `POSTGRES_DATABASE_URL` trong `.env`; không chỉ sửa `.env.example`.
- **MongoDB báo `NXDOMAIN`/SRV:** kiểm tra hostname Atlas, DNS, VPN hoặc chính sách mạng. Đây không phải lỗi mật khẩu MongoDB.
- **MongoDB báo timeout:** kiểm tra IP trong Atlas Network Access và firewall.
- **Frontend không gọi được API:** xác nhận backend đang chạy port `8000`; mở frontend tại `http://localhost:5173`.
- **Ảnh không gửi được:** cần cấu hình Cloudinary trong `CLOUDINARY_URL`.
- **Đăng nhập hết hạn sau khi restart:** giữ `JWT_SECRET` ổn định trong `.env`.
