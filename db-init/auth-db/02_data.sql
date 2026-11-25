SET NAMES utf8mb4;

-- ---------- ROLE ----------
INSERT INTO tbl_role (id, code, name, description) VALUES
    (1, 'ADMIN',    'Quản trị viên', 'Toàn quyền trên hệ thống'),
    (2, 'MANAGER',  'Quản lý',       'Chỉ xem được danh sách user'),
    (3, 'EMPLOYEE', 'Nhân viên',     'Chỉ xem và sửa thông tin của chính mình');

-- ---------- PERMISSION ----------
INSERT INTO tbl_permission (id, code, name, description) VALUES
    (1, 'USER_CREATE',      'Tạo user',                 'POST /api/accounts'),
    (2, 'USER_VIEW_LIST',   'Xem danh sách user',       'GET /api/accounts (auth-service), GET /api/users (user-service)'),
    (3, 'USER_VIEW_DETAIL', 'Xem chi tiết user bất kỳ', 'GET /api/accounts/{id}'),
    (4, 'USER_UPDATE',      'Sửa user bất kỳ',          'PUT /api/accounts/{id}'),
    (5, 'USER_DELETE',      'Xóa user',                 'DELETE /api/accounts/{id}'),
    (6, 'USER_VIEW_SELF',   'Xem thông tin của mình',   'GET /api/accounts/{id} với id của chính mình'),
    (7, 'USER_UPDATE_SELF', 'Sửa thông tin của mình',   'PUT /api/accounts/{id} với id của chính mình');

-- ---------- ROLE - PERMISSION ----------
-- ADMIN: có tất cả các quyền
INSERT INTO tbl_role_permission (role_id, permission_id) VALUES
    (1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7);
-- MANAGER: chỉ xem danh sách
INSERT INTO tbl_role_permission (role_id, permission_id) VALUES
    (2, 2);
-- EMPLOYEE: chỉ xem và sửa chính mình
INSERT INTO tbl_role_permission (role_id, permission_id) VALUES
    (3, 6), (3, 7);

-- ---------- USER ----------
-- Mật khẩu của cả 4 tài khoản đều là: 123456 (đã mã hóa BCrypt)
INSERT INTO tbl_user (id, full_name, username, password, email, enabled) VALUES
    (1, 'Nguyễn Văn Admin', 'admin',     '$2a$10$.PVKOaZADsvldsCtj2TVeeugO.ucRt073Ev4PjpJho1q4rY3PGQXm', 'admin@example.com',     1),
    (2, 'Trần Thị Quản Lý', 'manager',   '$2a$10$.PVKOaZADsvldsCtj2TVeeugO.ucRt073Ev4PjpJho1q4rY3PGQXm', 'manager@example.com',   1),
    (3, 'Lê Văn Nhân Viên', 'employee',  '$2a$10$.PVKOaZADsvldsCtj2TVeeugO.ucRt073Ev4PjpJho1q4rY3PGQXm', 'employee@example.com',  1),
    (4, 'Phạm Thị Hoa',     'employee2', '$2a$10$.PVKOaZADsvldsCtj2TVeeugO.ucRt073Ev4PjpJho1q4rY3PGQXm', 'employee2@example.com', 1);

-- ---------- USER - ROLE ----------
INSERT INTO tbl_user_role (user_id, role_id) VALUES
    (1, 1),   -- admin     -> ADMIN
    (2, 2),   -- manager   -> MANAGER
    (3, 3),   -- employee  -> EMPLOYEE
    (4, 3);   -- employee2 -> EMPLOYEE