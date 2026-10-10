USE sky_take_out_perf_test;
SET SESSION cte_max_recursion_depth = 10000;

INSERT INTO orders (number, status, user_id, order_time, pay_status, amount, consignee, phone, address)
WITH RECURSIVE sequence_numbers AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM sequence_numbers WHERE n < 10000
)
SELECT CONCAT('PERF-', LPAD(n, 8, '0')),
       MOD(n, 6),
       9001,
       DATE_SUB('2026-01-01 12:00:00', INTERVAL n MINUTE),
       MOD(n, 2),
       20.00 + MOD(n, 100),
       'Test User', '01000000000', 'Performance Test Address'
FROM sequence_numbers;

INSERT INTO order_detail (name, image, order_id, dish_id, number, amount)
SELECT CONCAT('Test Dish-', detail_number), '', o.id, detail_number, 1, 10.00 + detail_number
FROM orders o
CROSS JOIN (SELECT 1 AS detail_number UNION ALL SELECT 2 UNION ALL SELECT 3) details
WHERE o.user_id = 9001;
