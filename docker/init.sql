-- ============================================
-- Smart Procurement System - Database Schema
-- BCNF Normalized
-- ============================================

CREATE TABLE IF NOT EXISTS department (
    dept_id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    budget DECIMAL(15,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS employee (
    emp_id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL CHECK (role IN ('ADMIN', 'MANAGER', 'EMPLOYEE', 'FINANCE')),
    dept_id INT REFERENCES department(dept_id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vendor (
    vendor_id SERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    contact VARCHAR(20),
    rating DECIMAL(3,2) DEFAULT 0.0 CHECK (rating >= 0 AND rating <= 5),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS expense (
    exp_id SERIAL PRIMARY KEY,
    emp_id INT REFERENCES employee(emp_id),
    amount DECIMAL(12,2) NOT NULL,
    category VARCHAR(50) CHECK (category IN ('TRAVEL', 'FOOD', 'EQUIPMENT', 'OTHER')),
    description TEXT,
    fraud_score DECIMAL(5,4) DEFAULT 0.0,
    status VARCHAR(30) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'FLAGGED')),
    receipt_url VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS purchase_order (
    po_id SERIAL PRIMARY KEY,
    vendor_id INT REFERENCES vendor(vendor_id),
    dept_id INT REFERENCES department(dept_id),
    requested_by INT REFERENCES employee(emp_id),
    amount DECIMAL(12,2) NOT NULL,
    description TEXT,
    status VARCHAR(30) DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PENDING', 'APPROVED', 'REJECTED', 'COMPLETED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS approval (
    approval_id SERIAL PRIMARY KEY,
    po_id INT REFERENCES purchase_order(po_id),
    approver_id INT REFERENCES employee(emp_id),
    action VARCHAR(20) NOT NULL CHECK (action IN ('APPROVED', 'REJECTED')),
    comments TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- Seed Data
-- ============================================

INSERT INTO department (name, budget) VALUES
('Engineering', 500000.00),
('Finance', 300000.00),
('Operations', 400000.00),
('HR', 200000.00);

-- Passwords are BCrypt hashed (plain: admin123, manager123, employee123, finance123)
INSERT INTO employee (name, email, password, role, dept_id) VALUES
('Admin User',     'admin@company.com',    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHHy', 'ADMIN',    1),
('Manager User',   'manager@company.com',  '$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO1OHjinK8.', 'MANAGER',  1),
('Employee User',  'employee@company.com', '$2a$10$ByIUiNaRfBKSV8urFVLSCOxKlyKPWFIqCLPQHSNi5N7wVCaT2oqJ.', 'EMPLOYEE', 2),
('Finance User',   'finance@company.com',  '$2a$10$ByIUiNaRfBKSV8urFVLSCOxKlyKPWFIqCLPQHSNi5N7wVCaT2oqJ.', 'FINANCE',  2);

INSERT INTO vendor (name, email, contact, rating) VALUES
('TechSupplies Co.',  'tech@techsupplies.com',  '+91-9876543210', 4.5),
('Office Essentials', 'office@essentials.com',  '+91-9876543211', 4.2),
('Travel Partners',   'info@travelpartners.com', '+91-9876543212', 4.7),
('Equipment Hub',     'sales@equipmenthub.com',  '+91-9876543213', 3.9);

INSERT INTO expense (emp_id, amount, category, description, fraud_score, status) VALUES
(3, 2500.00, 'TRAVEL',    'Client visit to Mumbai',     0.05, 'APPROVED'),
(3, 850.00,  'FOOD',      'Team lunch Q1',              0.02, 'APPROVED'),
(3, 45000.00,'EQUIPMENT', 'Laptop purchase',            0.15, 'PENDING'),
(3, 120000.00,'EQUIPMENT','Suspicious bulk order',      0.89, 'FLAGGED');

INSERT INTO purchase_order (vendor_id, dept_id, requested_by, amount, description, status) VALUES
(1, 1, 3, 75000.00,  'Server hardware upgrade',   'PENDING'),
(2, 1, 3, 12000.00,  'Office stationery Q2',      'APPROVED'),
(3, 3, 3, 180000.00, 'Annual travel package',     'DRAFT'),
(4, 1, 3, 350000.00, 'Development workstations',  'PENDING');
