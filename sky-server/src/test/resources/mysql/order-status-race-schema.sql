CREATE DATABASE IF NOT EXISTS sky_take_out_test
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE sky_take_out_test;

CREATE TABLE IF NOT EXISTS orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    number VARCHAR(100) NOT NULL,
    status INT NOT NULL,
    order_time DATETIME NOT NULL,
    checkout_time DATETIME NULL,
    pay_status INT NOT NULL,
    cancel_reason VARCHAR(255) NULL,
    cancel_time DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_status_race_number (number)
) ENGINE=InnoDB;
