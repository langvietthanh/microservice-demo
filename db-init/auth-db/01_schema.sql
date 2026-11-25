-- Bắt buộc: báo cho MySQL biết file này được lưu dạng UTF-8 (để không lỗi font tiếng Việt)
SET NAMES utf8mb4;

-- 1. Người dùng (tài khoản đăng nhập)
CREATE TABLE tbl_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    full_name   VARCHAR(100) NOT NULL,
    username    VARCHAR(50)  NOT NULL,
    password    VARCHAR(100) NOT NULL,               -- lưu chuỗi đã mã hóa BCrypt, KHÔNG lưu mật khẩu gốc
    email       VARCHAR(100) NULL,
    enabled     TINYINT(1)   NOT NULL DEFAULT 1,     -- 1: hoạt động, 0: bị khóa
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_username UNIQUE (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 2. Vai trò (nhóm quyền)
CREATE TABLE tbl_role (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    code        VARCHAR(50)  NOT NULL,               -- ADMIN, MANAGER, EMPLOYEE
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_role_code UNIQUE (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 3. Quyền (một hành động cụ thể trong hệ thống)
CREATE TABLE tbl_permission (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    code        VARCHAR(50)  NOT NULL,               -- USER_CREATE, USER_VIEW_LIST...
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_permission_code UNIQUE (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 4. Bảng nối user - role (quan hệ nhiều - nhiều)
CREATE TABLE tbl_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES tbl_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES tbl_role (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 5. Bảng nối role - permission (quan hệ nhiều - nhiều)
CREATE TABLE tbl_role_permission (
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permission_role       FOREIGN KEY (role_id)       REFERENCES tbl_role (id),
    CONSTRAINT fk_role_permission_permission FOREIGN KEY (permission_id) REFERENCES tbl_permission (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 6. Refresh token đã cấp (để thu hồi được khi logout / khi bị lộ)
CREATE TABLE tbl_refresh_token (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    user_id     BIGINT      NOT NULL,
    token_id    VARCHAR(36) NOT NULL,                -- giá trị "jti" nằm trong refresh token
    expires_at  DATETIME    NOT NULL,
    revoked     TINYINT(1)  NOT NULL DEFAULT 0,      -- 1: đã thu hồi, không dùng được nữa
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_refresh_token_token_id UNIQUE (token_id),
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES tbl_user (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;