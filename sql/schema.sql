-- Local demo setup: schema exported from the sky_take_out development database with mysqldump --no-data
-- Run only once against a new, empty database; this script does not delete or overwrite existing tables or data
-- The demo database is sky_take_out_demo; set sky.datasource.database to this database when connecting the application
CREATE DATABASE IF NOT EXISTS `sky_take_out_demo` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `sky_take_out_demo`;
SET NAMES utf8mb4;

CREATE TABLE `address_book` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `user_id` bigint NOT NULL COMMENT 'User ID',
  `consignee` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'Recipient',
  `sex` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'Gender',
  `phone` varchar(11) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT 'Phone number',
  `province_code` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'Province region code',
  `province_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'Province name',
  `city_code` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'City region code',
  `city_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'City name',
  `district_code` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'District region code',
  `district_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'District name',
  `detail` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'Street address',
  `label` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'Label',
  `is_default` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'Default address (0: no, 1: yes)',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='Address book';



CREATE TABLE `category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `type` int DEFAULT NULL COMMENT 'Type (1: dish category, 2: set meal category)',
  `name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT 'Category name',
  `sort` int NOT NULL DEFAULT '0' COMMENT 'Sort order',
  `status` int DEFAULT NULL COMMENT 'Status (0: disabled, 1: enabled)',
  `create_time` datetime DEFAULT NULL COMMENT 'Creation time',
  `update_time` datetime DEFAULT NULL COMMENT 'Update time',
  `create_user` bigint DEFAULT NULL COMMENT 'Created by',
  `update_user` bigint DEFAULT NULL COMMENT 'Updated by',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_category_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='Dish and set meal categories';



CREATE TABLE `dish` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT 'Dish name',
  `category_id` bigint NOT NULL COMMENT 'Dish category ID',
  `price` decimal(10,2) DEFAULT NULL COMMENT 'Dish price',
  `image` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'Image',
  `description` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'Description',
  `status` int DEFAULT '1' COMMENT 'Status (0: discontinued, 1: available)',
  `create_time` datetime DEFAULT NULL COMMENT 'Creation time',
  `update_time` datetime DEFAULT NULL COMMENT 'Update time',
  `create_user` bigint DEFAULT NULL COMMENT 'Created by',
  `update_user` bigint DEFAULT NULL COMMENT 'Updated by',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_dish_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='Dishes';



CREATE TABLE `dish_flavor` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `dish_id` bigint NOT NULL COMMENT 'Dish',
  `name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'Flavor name',
  `value` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'Flavor options (JSON list)',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='Dish flavors';



CREATE TABLE `employee` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT 'Name',
  `username` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT 'Username',
  `password` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT 'Password',
  `phone` varchar(11) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT 'Phone number',
  `sex` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT 'Gender',
  `id_number` varchar(18) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT 'Identity number',
  `status` int NOT NULL DEFAULT '1' COMMENT 'Status (0: disabled, 1: enabled)',
  `create_time` datetime DEFAULT NULL COMMENT 'Creation time',
  `update_time` datetime DEFAULT NULL COMMENT 'Update time',
  `create_user` bigint DEFAULT NULL COMMENT 'Created by',
  `update_user` bigint DEFAULT NULL COMMENT 'Updated by',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='Employee accounts';



CREATE TABLE `order_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'Name',
  `image` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'Image',
  `order_id` bigint NOT NULL COMMENT 'Order ID',
  `dish_id` bigint DEFAULT NULL COMMENT 'Dish ID',
  `setmeal_id` bigint DEFAULT NULL COMMENT 'Set meal ID',
  `dish_flavor` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'Flavor',
  `number` int NOT NULL DEFAULT '1' COMMENT 'Quantity',
  `amount` decimal(10,2) NOT NULL COMMENT 'Amount',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='Order line items';



CREATE TABLE `orders` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `number` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'Order number',
  `status` int NOT NULL DEFAULT '1' COMMENT 'Order status (1: pending payment, 2: pending acceptance, 3: accepted, 4: out for delivery, 5: completed, 6: cancelled, 7: refunded)',
  `user_id` bigint NOT NULL COMMENT 'Customer user ID',
  `address_book_id` bigint NOT NULL COMMENT 'Address ID',
  `order_time` datetime NOT NULL COMMENT 'Order time',
  `checkout_time` datetime DEFAULT NULL COMMENT 'Checkout time',
  `pay_method` int NOT NULL DEFAULT '1' COMMENT 'Payment method (1: WeChat Pay, 2: Alipay)',
  `pay_status` tinyint NOT NULL DEFAULT '0' COMMENT 'Payment status (0: unpaid, 1: paid, 2: refunded)',
  `amount` decimal(10,2) NOT NULL COMMENT 'Amount paid',
  `remark` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'Remarks',
  `phone` varchar(11) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'Phone number',
  `address` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'Address',
  `user_name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'Customer name',
  `consignee` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'Recipient',
  `cancel_reason` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'Cancellation reason',
  `rejection_reason` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'Rejection reason',
  `cancel_time` datetime DEFAULT NULL COMMENT 'Cancellation time',
  `estimated_delivery_time` datetime DEFAULT NULL COMMENT 'Estimated delivery time',
  `delivery_status` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'Delivery type (1: immediate, 0: scheduled)',
  `delivery_time` datetime DEFAULT NULL COMMENT 'Delivery time',
  `pack_amount` int DEFAULT NULL COMMENT 'Packaging fee',
  `tableware_number` int DEFAULT NULL COMMENT 'Cutlery quantity',
  `tableware_status` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'Cutlery quantity mode (1: based on diner count, 0: specified quantity)',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='Orders';



CREATE TABLE `setmeal` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `category_id` bigint NOT NULL COMMENT 'Dish category ID',
  `name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT 'Set meal name',
  `price` decimal(10,2) NOT NULL COMMENT 'Set meal price',
  `status` int DEFAULT '1' COMMENT 'Availability status (0: unavailable, 1: available)',
  `description` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'Description',
  `image` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'Image',
  `create_time` datetime DEFAULT NULL COMMENT 'Creation time',
  `update_time` datetime DEFAULT NULL COMMENT 'Update time',
  `create_user` bigint DEFAULT NULL COMMENT 'Created by',
  `update_user` bigint DEFAULT NULL COMMENT 'Updated by',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_setmeal_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='Set meals';



CREATE TABLE `setmeal_dish` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `setmeal_id` bigint DEFAULT NULL COMMENT 'Set meal ID',
  `dish_id` bigint DEFAULT NULL COMMENT 'Dish ID',
  `name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'Dish name (denormalized)',
  `price` decimal(10,2) DEFAULT NULL COMMENT 'Dish unit price (denormalized)',
  `copies` int DEFAULT NULL COMMENT 'Dish portions',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='Set meal items';



CREATE TABLE `shopping_cart` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'Item name',
  `image` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'Image',
  `user_id` bigint NOT NULL COMMENT 'Primary key',
  `dish_id` bigint DEFAULT NULL COMMENT 'Dish ID',
  `setmeal_id` bigint DEFAULT NULL COMMENT 'Set meal ID',
  `dish_flavor` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'Flavor',
  `number` int NOT NULL DEFAULT '1' COMMENT 'Quantity',
  `amount` decimal(10,2) NOT NULL COMMENT 'Amount',
  `create_time` datetime DEFAULT NULL COMMENT 'Creation time',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='Shopping cart';



CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `openid` varchar(45) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'WeChat user identifier',
  `name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'Name',
  `phone` varchar(11) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'Phone number',
  `sex` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'Gender',
  `id_number` varchar(18) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'Identity number',
  `avatar` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'Avatar',
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='Customers';
