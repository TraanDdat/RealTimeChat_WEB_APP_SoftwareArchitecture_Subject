📘 Hướng Dẫn Cài Đặt và Thiết Lập PostgreSQL trên WSL2

Tài liệu này hướng dẫn cách cài đặt PostgreSQL trên môi trường Ubuntu (WSL2) của Windows, khởi động dịch vụ, thiết lập mật khẩu mặc định và tạo cơ sở dữ liệu mới để kết nối với các dự án (như Node.js/React).



Cài đặt PostgreSQL

Tại cửa sổ dòng lệnh Linux vừa mở, hãy chạy lệnh sau để tải và cài đặt PostgreSQL cùng các công cụ hỗ trợ:

✏️ sudo apt update && sudo apt install postgresql postgresql-contrib -y



Khởi động dịch vụ Database

Trên WSL2, PostgreSQL không tự động chạy. Bạn cần phải khởi động nó bằng lệnh:

✏️ sudo service postgresql start


Để chắc chắn hệ thống đã chạy thành công, bạn có thể kiểm tra trạng thái:

✏️ sudo service postgresql status


(Nếu màn hình hiện chữ online kèm theo cổng 5432, dịch vụ đã hoạt động).

Đăng nhập và thiết lập mật khẩu

PostgreSQL tạo sẵn một tài khoản siêu quản trị có tên là postgres nhưng chưa có mật khẩu. Để dự án code có thể kết nối được, ta cần đặt mật khẩu cho nó.

Đăng nhập vào môi trường gõ lệnh của PostgreSQL:

✏️ sudo -u postgres psql





Tạo Cơ sở dữ liệu (Database)

Các dự án code cần một database cụ thể để lưu trữ bảng (tables). Dưới đây là cách tạo database (ví dụ tên là pern_auth):

Ngay tại màn hình postgres=#, gõ lệnh:

✏️ CREATE DATABASE pern_auth;


Nếu màn hình phản hồi chữ CREATE DATABASE, bạn đã tạo thành công.

Bước 6: Các lệnh kiểm tra cơ bản (Nên biết)

Dưới đây là một số thao tác kiểm tra dữ liệu quan trọng dành cho người mới:

Xem danh sách các database đang có:

✏️ \l


Đi vào (kết nối) một database cụ thể:

✏️ \c pern_auth


(Màn hình sẽ báo: You are now connected to database "pern_auth"...)

Xem danh sách các bảng (tables) trong database:

✏️ \dt


Xem dữ liệu bên trong một bảng (ví dụ bảng users):

✏️ SELECT * FROM users;


Thoát khỏi PostgreSQL (trở về màn hình Ubuntu):

✏️ \q
