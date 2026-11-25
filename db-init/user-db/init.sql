USE user_db;

CREATE TABLE IF NOT EXISTS tbl_user (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    department_id INT NOT NULL,
    user_name     VARCHAR(100) NOT NULL
);

INSERT INTO tbl_user (department_id, user_name) VALUES
(1, 'nguyenvana'),
(2, 'tranthib'),
(1, 'lehoangc'),
(3, 'phamthid');