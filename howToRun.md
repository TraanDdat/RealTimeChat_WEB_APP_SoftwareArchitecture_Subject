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
2. Sau khi tạo tài khoản, đăng nhập với tài khoản A để nhận cookie phiên.
3. Mở một cửa sổ ẩn danh tại cùng địa chỉ, đăng ký rồi đăng nhập tài khoản B.
4. Trong Contacts, chọn tài khoản còn lại và gửi tin nhắn. Gửi phản hồi từ cửa sổ kia để kiểm tra chat hai chiều và realtime.
5. Gửi ảnh hoặc đổi avatar chỉ hoạt động khi đã cấu hình `CLOUDINARY_URL`; nhắn tin văn bản không cần Cloudinary.

Các thao tác chính được thực hiện qua API:

| Chức năng | Endpoint |
| --- | --- |
| Đăng ký | `POST /api/auth/register` |
| Đăng nhập | `POST /api/auth/login` |
| Kiểm tra phiên | `GET /api/auth/check` |
| Danh bạ | `GET /api/messages/contacts` |
| Người đã chat | `GET /api/messages/chats` |
| Lịch sử chat | `GET /api/messages/{userId}` |
| Gửi tin nhắn | `POST /api/messages/send/{userId}` |

`GET /api/auth/check` trả `401` khi chưa đăng nhập là bình thường. Giao diện đăng nhập dùng cookie JWT httpOnly để duy trì phiên.

## Chạy Android và kiểm tra đồng bộ với Web

Mở thư mục `src/android_app` trong Android Studio. Cài JDK 21 (theo cấu hình Gradle daemon của dự án), Android SDK 34 và các công cụ NDK/CMake mà project hiện yêu cầu. Chờ Gradle Sync hoàn tất rồi chạy trên emulator.

Trong `src/android_app/app/src/main/java/com/example/zaloclone/ApiConfig.kt`, `BASE_URL` mặc định là `http://10.0.2.2:8000/`: đây là địa chỉ truy cập máy host từ Android Emulator, không phải địa chỉ của Web Vite. Nếu dùng điện thoại thật, đổi sang IP LAN của máy chạy backend, chạy Uvicorn với `--host 0.0.0.0` và cho phép port 8000 qua firewall. Không dùng `localhost` trên điện thoại để trỏ tới backend trên máy tính.

- Android gọi `POST /api/auth/register` với `{email, username, password, role: "User"}`; sau đó gọi login để nhận phiên.
- Login dùng `POST /api/auth/login` với `{email, password, role: "User"}`, sau đó gọi `/api/auth/check` để lấy user công khai có `_id`. Không dùng `access_token` từ JSON phản hồi làm Bearer token.
- Cookie `jwt` được lưu trong EncryptedSharedPreferences và dùng chung bởi Retrofit/OkHttp và Socket.io, kể cả khi mở lại app. Cookie không được đưa vào backup. Với HTTP local, đặt `COOKIE_SECURE=false`; triển khai thật cần HTTPS và cookie secure.
- Contacts là danh sách user từ `/api/messages/contacts` để bắt đầu chat, không có thao tác gửi/chấp nhận lời mời kết bạn. Tab Chats dùng `/api/messages/chats`.
- Gửi tin qua `POST /api/messages/send/{receiverId}` với đúng `{text, image}`. `image` là `null` hoặc data URL base64 như Web; không gửi URI `content://`, `senderId`, `_id` hay timestamp của thiết bị lên database. Backend tự gán định danh và thời gian.
- Socket.io kết nối cùng backend tại `/socket.io` bằng cùng cookie, nhận `newMessage` và `getOnlineUsers`. Sau reconnect, app tải lại lịch sử REST và hợp nhất theo `_id` để bù tin bị lỡ, không tạo bản sao.
- Backend hiện chưa có route logout. Nút Sign out trên Android đóng socket, xóa cookie và state cục bộ; không thu hồi JWT ở phía server. Nút logout Web hiện có vẫn cần route backend tương ứng, nằm ngoài thay đổi này.

Kiểm tra thủ công:

1. Tạo/đăng nhập tài khoản A trên Web và tài khoản B trên Android. Chọn nhau trong Contacts.
2. Gửi tin từ Web sang Android rồi gửi ngược lại. Tin của mình phải nằm bên phải, khác màu; tin của người kia nằm bên trái.
3. Đóng/mở lại app và mở lại hội thoại: phiên còn hợp lệ phải được khôi phục, lịch sử phải được tải từ backend.
4. Chuyển hội thoại khi lịch sử đang tải, gửi tin mới trong lúc tải lịch sử, rồi ngắt/bật lại mạng. Không được hiện tin của hội thoại khác hoặc lặp tin sau reconnect.
5. Gửi ảnh từ cả hai client khi đã cấu hình Cloudinary. Sign out rồi đăng nhập tài khoản khác trên Android để kiểm tra không còn dữ liệu của phiên trước.

Chạy kiểm tra từ thư mục gốc khi đã cài toolchain:

```powershell
uv run --extra dev pytest tests/test_auth_contract.py -q
.\src\android_app\gradlew.bat -p src/android_app :app:testDebugUnitTest :app:assembleDebug
```

Unit test Android kiểm tra endpoint/body login và register, cookie REST/socket, khôi phục/xóa cookie, payload gửi tin, lọc hội thoại và hợp nhất lịch sử với realtime. Test backend dùng dữ liệu giả, không truy cập PostgreSQL/MongoDB. Các test này không thay thế kiểm tra Web/Android trên backend và database thật.

##  Kiểm tra dữ liệu đã lưu

### PostgreSQL: tài khoản

Trong WSL, mở `psql` với đúng tên database từ `POSTGRES_DATABASE_URL` (ví dụ database tên `users`):

```bash
sudo -u postgres psql -d users
```

Sau đó chạy:

```sql
SELECT id, username, email, profile_pic,
       LEFT(password, 20) AS password_hash_preview
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
