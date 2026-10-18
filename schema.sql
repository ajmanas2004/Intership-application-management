-- MySQL 8.4+. Select/create internship_management before running this script.
CREATE TABLE IF NOT EXISTS companies (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(255) NOT NULL,
 industry VARCHAR(100), location VARCHAR(255), website VARCHAR(255)
);
CREATE TABLE IF NOT EXISTS internships (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, company_id BIGINT NOT NULL,
 position VARCHAR(255) NOT NULL, location VARCHAR(255), deadline DATE, description TEXT,
 CONSTRAINT fk_internship_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE RESTRICT
);
CREATE TABLE IF NOT EXISTS applications (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, internship_id BIGINT NOT NULL,
 applied_date DATE, status ENUM('SAVED','APPLIED','INTERVIEW','OFFERED','REJECTED') NOT NULL,
 notes TEXT, created_at DATETIME NOT NULL, updated_at DATETIME NOT NULL,
 CONSTRAINT uq_application_internship UNIQUE (internship_id),
 CONSTRAINT fk_application_internship FOREIGN KEY (internship_id) REFERENCES internships(id) ON DELETE RESTRICT
);

