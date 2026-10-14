-- Use only sky_take_out_perf_test; the script runner rejects every other database name
CREATE DATABASE IF NOT EXISTS sky_take_out_perf_test CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE sky_take_out_perf_test;

DROP TABLE IF EXISTS order_detail;
DROP TABLE IF EXISTS orders;

CREATE TABLE orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    number VARCHAR(50) NOT NULL,
    status INT,
    user_id BIGINT,
    address_book_id BIGINT,
    order_time DATETIME,
    checkout_time DATETIME,
    pay_method INT,
    pay_status INT,
    amount DECIMAL(10, 2),
    remark VARCHAR(255),
    phone VARCHAR(20),
    address VARCHAR(255),
    consignee VARCHAR(50),
    estimated_delivery_time DATETIME,
    delivery_status INT,
    pack_amount INT,
    tableware_number INT,
    tableware_status INT,
    cancel_reason VARCHAR(255),
    rejection_reason VARCHAR(255),
    cancel_time DATETIME,
    delivery_time DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uk_orders_number (number),
    KEY idx_orders_user_time (user_id, order_time)
) ENGINE=InnoDB;

CREATE TABLE order_detail (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100),
    image VARCHAR(255),
    order_id BIGINT NOT NULL,
    dish_id BIGINT,
    setmeal_id BIGINT,
    dish_flavor VARCHAR(100),
    number INT,
    amount DECIMAL(10, 2),
    PRIMARY KEY (id),
    KEY idx_order_detail_order_id (order_id)
) ENGINE=InnoDB;
