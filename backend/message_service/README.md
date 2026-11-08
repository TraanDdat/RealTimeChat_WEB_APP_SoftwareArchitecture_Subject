

## Chạy API

Chạy lệnh từ thư mục gốc repository. Cài dependencies nếu chưa cài:

```powershell
uv sync
```

Message Service gọi User Service để kiểm tra user gửi/nhận. Mở terminal thứ nhất
để chạy User Service:

```powershell
uv run --env-file .env uvicorn backend.user_service.main:app --host 127.0.0.1 --port 8000
```

Mở terminal thứ hai để chạy Message Service:

```powershell
uv run --env-file .env uvicorn backend.message_service.main:app --host 127.0.0.1 --port 8001
```

API docs: <http://127.0.0.1:8001/docs>. Khi khởi động, service kết nối tới MongoDB
và tạo index cho collection `messages`.

## Dùng HTTP API

Mọi request cần header `X-User-ID` chứa ID user dương. Đây là định danh, chưa phải
xác thực.

| Method | Path | Mô tả |
| --- | --- | --- |
| `GET` | `/api/messages/contacts` | Danh sách user trừ user hiện tại |
| `GET` | `/api/messages/chats` | Danh sách user đã có hội thoại |
| `GET` | `/api/messages/{id}` | Lịch sử hội thoại với user `id` |
| `POST` | `/api/messages/send/{id}` | Gửi tin nhắn cho user `id` |

Ví dụ dùng PowerShell, user `1` gửi tin cho user `2`:

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://127.0.0.1:8001/api/messages/send/2" `
  -Headers @{"X-User-ID" = "1"} `
  -ContentType "application/json" `
  -Body '{"text":"Xin chào"}'
```

Body gửi tin nhận `text`, `image`, hoặc cả hai:

```json
{"text":"Xin chào"}
```

Đọc lịch sử hội thoại giữa user `1` và `2`:

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://127.0.0.1:8001/api/messages/2" `
  -Headers @{"X-User-ID" = "1"}
```

Các endpoint contacts và chats dùng cùng header:

```powershell
Invoke-RestMethod -Uri "http://127.0.0.1:8001/api/messages/contacts" -Headers @{"X-User-ID" = "1"}
Invoke-RestMethod -Uri "http://127.0.0.1:8001/api/messages/chats" -Headers @{"X-User-ID" = "1"}
```
