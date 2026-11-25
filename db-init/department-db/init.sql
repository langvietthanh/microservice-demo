USE department_db;

CREATE TABLE IF NOT EXISTS tbl_department (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    department_name VARCHAR(100) NOT NULL
);

INSERT INTO tbl_department (department_name) VALUES
('Engineering'),
('Human Resources'),
('Accounting');