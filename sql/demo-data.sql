-- Intended only for a fresh sky_take_out_demo database created by sql/schema.sql
-- Demo credentials: admin / 123456; the current backend verifies employee passwords with MD5
-- Source menu data is copied from sky_take_out; employee audit references are cleared because source employees are not copied
USE `sky_take_out_demo`;
SET NAMES utf8mb4;

START TRANSACTION;

INSERT INTO `employee`
  (`id`, `name`, `username`, `password`, `phone`, `sex`, `id_number`, `status`)
VALUES
  (1, 'Demo Administrator', 'admin', MD5('123456'), '00000000000', '1', '000000000000000000', 1);

INSERT INTO `category` (`id`, `type`, `name`, `sort`, `status`, `create_time`, `update_time`, `create_user`, `update_user`) VALUES (11,1,'酒水饮料',10,1,'2022-06-09 22:09:18','2022-06-09 22:09:18',NULL,NULL);
INSERT INTO `category` (`id`, `type`, `name`, `sort`, `status`, `create_time`, `update_time`, `create_user`, `update_user`) VALUES (12,1,'传统主食',9,1,'2022-06-09 22:09:32','2022-06-09 22:18:53',NULL,NULL);
INSERT INTO `category` (`id`, `type`, `name`, `sort`, `status`, `create_time`, `update_time`, `create_user`, `update_user`) VALUES (13,2,'人气套餐',12,1,'2022-06-09 22:11:38','2022-06-10 11:04:40',NULL,NULL);
INSERT INTO `category` (`id`, `type`, `name`, `sort`, `status`, `create_time`, `update_time`, `create_user`, `update_user`) VALUES (15,2,'商务套餐',13,1,'2022-06-09 22:14:10','2022-06-10 11:04:48',NULL,NULL);
INSERT INTO `category` (`id`, `type`, `name`, `sort`, `status`, `create_time`, `update_time`, `create_user`, `update_user`) VALUES (16,1,'蜀味烤鱼',5,1,'2022-06-09 22:15:37','2026-07-15 23:47:55',NULL,NULL);
INSERT INTO `category` (`id`, `type`, `name`, `sort`, `status`, `create_time`, `update_time`, `create_user`, `update_user`) VALUES (17,1,'蜀味牛蛙',4,1,'2022-06-09 22:16:14','2026-07-15 23:48:00',NULL,NULL);
INSERT INTO `category` (`id`, `type`, `name`, `sort`, `status`, `create_time`, `update_time`, `create_user`, `update_user`) VALUES (18,1,'特色蒸菜',6,1,'2022-06-09 22:17:42','2022-06-09 22:17:42',NULL,NULL);
INSERT INTO `category` (`id`, `type`, `name`, `sort`, `status`, `create_time`, `update_time`, `create_user`, `update_user`) VALUES (19,1,'新鲜时蔬',7,1,'2022-06-09 22:18:12','2022-06-09 22:18:28',NULL,NULL);
INSERT INTO `category` (`id`, `type`, `name`, `sort`, `status`, `create_time`, `update_time`, `create_user`, `update_user`) VALUES (20,1,'水煮鱼',8,1,'2022-06-09 22:22:29','2022-06-09 22:23:45',NULL,NULL);
INSERT INTO `category` (`id`, `type`, `name`, `sort`, `status`, `create_time`, `update_time`, `create_user`, `update_user`) VALUES (21,1,'汤类',11,1,'2022-06-10 10:51:47','2022-06-10 10:51:47',NULL,NULL);
INSERT INTO `dish` (`id`, `name`, `category_id`, `price`, `image`, `description`, `status`, `create_time`, `update_time`, `create_user`, `update_user`) VALUES (46,'王老吉',11,6.00,'https://res.cloudinary.com/dnlorqmf/image/upload/v1791141590/sky-take-out/dh0e5egcopws7hezxip7.png','',1,'2022-06-09 22:40:47','2026-10-04 21:19:54',NULL,NULL);
INSERT INTO `dish` (`id`, `name`, `category_id`, `price`, `image`, `description`, `status`, `create_time`, `update_time`, `create_user`, `update_user`) VALUES (47,'北冰洋',11,4.00,'https://res.cloudinary.com/dnlorqmf/image/upload/v1791141605/sky-take-out/ih8dugakuit1iffqlej2.png','还是小时候的味道',1,'2022-06-10 09:18:49','2026-10-04 21:20:07',NULL,NULL);
INSERT INTO `dish` (`id`, `name`, `category_id`, `price`, `image`, `description`, `status`, `create_time`, `update_time`, `create_user`, `update_user`) VALUES (48,'雪花啤酒',11,4.00,'https://res.cloudinary.com/dnlorqmf/image/upload/v1791141620/sky-take-out/b4pya1nrljfswvuxmogt.png','',1,'2022-06-10 09:22:54','2026-10-04 21:20:22',NULL,NULL);
INSERT INTO `dish` (`id`, `name`, `category_id`, `price`, `image`, `description`, `status`, `create_time`, `update_time`, `create_user`, `update_user`) VALUES (49,'米饭',12,2.00,'https://res.cloudinary.com/dnlorqmf/image/upload/v1791141640/sky-take-out/evggo2pxabovin6t4vgh.png','精选五常大米',1,'2022-06-10 09:30:17','2026-10-04 21:20:42',NULL,NULL);
INSERT INTO `dish` (`id`, `name`, `category_id`, `price`, `image`, `description`, `status`, `create_time`, `update_time`, `create_user`, `update_user`) VALUES (50,'馒头',12,1.00,'https://res.cloudinary.com/dnlorqmf/image/upload/v1791141655/sky-take-out/cmluilzodvfimuo6gbf4.png','优质面粉',1,'2022-06-10 09:34:28','2026-10-04 21:20:58',NULL,NULL);
INSERT INTO `dish` (`id`, `name`, `category_id`, `price`, `image`, `description`, `status`, `create_time`, `update_time`, `create_user`, `update_user`) VALUES (51,'老坛酸菜鱼',20,56.00,'https://res.cloudinary.com/dnlorqmf/image/upload/v1791141666/sky-take-out/chrrsj1pfsyyuvs05kzw.png','原料：汤，草鱼，酸菜',1,'2022-06-10 09:40:51','2026-10-04 21:21:09',NULL,NULL);
INSERT INTO `dish` (`id`, `name`, `category_id`, `price`, `image`, `description`, `status`, `create_time`, `update_time`, `create_user`, `update_user`) VALUES (52,'经典酸菜鮰鱼',20,66.00,'https://res.cloudinary.com/dnlorqmf/image/upload/v1791141681/sky-take-out/qm2klk76k9y78of6nttq.png','原料：酸菜，江团，鮰鱼',1,'2022-06-10 09:46:02','2026-10-04 21:21:28',NULL,NULL);
INSERT INTO `dish` (`id`, `name`, `category_id`, `price`, `image`, `description`, `status`, `create_time`, `update_time`, `create_user`, `update_user`) VALUES (53,'蜀味水煮草鱼',20,38.00,'https://res.cloudinary.com/dnlorqmf/image/upload/v1791141697/sky-take-out/e0qld9i7rpwcban3yfip.png','原料：草鱼，汤',1,'2022-06-10 09:48:37','2026-10-04 21:21:39',NULL,NULL);
INSERT INTO `dish` (`id`, `name`, `category_id`, `price`, `image`, `description`, `status`, `create_time`, `update_time`, `create_user`, `update_user`) VALUES (54,'清炒小油菜',19,18.00,'https://res.cloudinary.com/dnlorqmf/image/upload/v1791141708/sky-take-out/ro9aa8iovlb1m3c03jrh.png','原料：小油菜',1,'2022-06-10 09:51:46','2026-10-04 21:21:51',NULL,NULL);
INSERT INTO `dish` (`id`, `name`, `category_id`, `price`, `image`, `description`, `status`, `create_time`, `update_time`, `create_user`, `update_user`) VALUES (55,'蒜蓉娃娃菜',19,18.00,'https://res.cloudinary.com/dnlorqmf/image/upload/v1791141722/sky-take-out/acz5p4e2moqmsmitgucv.png','原料：蒜，娃娃菜',1,'2022-06-10 09:53:37','2026-10-04 21:22:05',NULL,NULL);
INSERT INTO `dish` (`id`, `name`, `category_id`, `price`, `image`, `description`, `status`, `create_time`, `update_time`, `create_user`, `update_user`) VALUES (56,'清炒西兰花',19,18.00,'https://res.cloudinary.com/dnlorqmf/image/upload/v1791141736/sky-take-out/fdv621q3dozepztar5p3.png','原料：西兰花',1,'2022-06-10 09:55:44','2026-10-04 21:22:18',NULL,NULL);
INSERT INTO `dish` (`id`, `name`, `category_id`, `price`, `image`, `description`, `status`, `create_time`, `update_time`, `create_user`, `update_user`) VALUES (57,'炝炒圆白菜',19,18.00,'https://res.cloudinary.com/dnlorqmf/image/upload/v1791141752/sky-take-out/mh3w25iboa79zi9btchl.png','原料：圆白菜',1,'2022-06-10 09:58:35','2026-10-04 21:22:36',NULL,NULL);
INSERT INTO `dish` (`id`, `name`, `category_id`, `price`, `image`, `description`, `status`, `create_time`, `update_time`, `create_user`, `update_user`) VALUES (58,'清蒸鲈鱼',18,98.00,'https://res.cloudinary.com/dnlorqmf/image/upload/v1791141765/sky-take-out/d5bl77il4qyo06xles5p.png','原料：鲈鱼',1,'2022-06-10 10:12:28','2026-10-04 21:22:47',NULL,NULL);
INSERT INTO `dish` (`id`, `name`, `category_id`, `price`, `image`, `description`, `status`, `create_time`, `update_time`, `create_user`, `update_user`) VALUES (59,'东坡肘子',18,138.00,'https://res.cloudinary.com/dnlorqmf/image/upload/v1791141778/sky-take-out/jr02lqo3ug8vke6kt0br.png','原料：猪肘棒',1,'2022-06-10 10:24:03','2026-10-04 21:23:01',NULL,NULL);
INSERT INTO `dish` (`id`, `name`, `category_id`, `price`, `image`, `description`, `status`, `create_time`, `update_time`, `create_user`, `update_user`) VALUES (60,'梅菜扣肉',18,58.00,'https://res.cloudinary.com/dnlorqmf/image/upload/v1791141789/sky-take-out/ryvxe1lz87z7vcywxdvq.png','原料：猪肉，梅菜',1,'2022-06-10 10:26:03','2026-10-04 21:23:13',NULL,NULL);
INSERT INTO `dish` (`id`, `name`, `category_id`, `price`, `image`, `description`, `status`, `create_time`, `update_time`, `create_user`, `update_user`) VALUES (61,'剁椒鱼头',18,66.00,'https://res.cloudinary.com/dnlorqmf/image/upload/v1791141801/sky-take-out/iepdbcvb4s0jvwyxc97q.png','原料：鲢鱼，剁椒',1,'2022-06-10 10:28:54','2026-10-04 21:23:22',NULL,NULL);
INSERT INTO `dish` (`id`, `name`, `category_id`, `price`, `image`, `description`, `status`, `create_time`, `update_time`, `create_user`, `update_user`) VALUES (62,'金汤酸菜牛蛙',17,88.00,'https://res.cloudinary.com/dnlorqmf/image/upload/v1791141811/sky-take-out/xp3rbc2wriidkaqqmhop.png','原料：鲜活牛蛙，酸菜',1,'2022-06-10 10:33:05','2026-10-04 21:23:33',NULL,NULL);
INSERT INTO `dish` (`id`, `name`, `category_id`, `price`, `image`, `description`, `status`, `create_time`, `update_time`, `create_user`, `update_user`) VALUES (63,'香锅牛蛙',17,88.00,'https://res.cloudinary.com/dnlorqmf/image/upload/v1791141822/sky-take-out/t2pqzi0wq9xy7vvvthhh.png','配料：鲜活牛蛙，莲藕，青笋',1,'2022-06-10 10:35:40','2026-10-04 21:23:44',NULL,NULL);
INSERT INTO `dish` (`id`, `name`, `category_id`, `price`, `image`, `description`, `status`, `create_time`, `update_time`, `create_user`, `update_user`) VALUES (64,'馋嘴牛蛙',17,88.00,'https://res.cloudinary.com/dnlorqmf/image/upload/v1791141832/sky-take-out/dzqai9a5mjigbpxguszn.png','配料：鲜活牛蛙，丝瓜，黄豆芽',1,'2022-06-10 10:37:52','2026-10-04 21:23:53',NULL,NULL);
INSERT INTO `dish` (`id`, `name`, `category_id`, `price`, `image`, `description`, `status`, `create_time`, `update_time`, `create_user`, `update_user`) VALUES (65,'草鱼2斤',16,68.00,'https://res.cloudinary.com/dnlorqmf/image/upload/v1791141841/sky-take-out/oaslfyk9x6rxrrs0d5nm.png','原料：草鱼，黄豆芽，莲藕',1,'2022-06-10 10:41:08','2026-10-04 21:24:06',NULL,NULL);
INSERT INTO `dish` (`id`, `name`, `category_id`, `price`, `image`, `description`, `status`, `create_time`, `update_time`, `create_user`, `update_user`) VALUES (66,'江团鱼2斤',16,119.00,'https://res.cloudinary.com/dnlorqmf/image/upload/v1791141856/sky-take-out/rsbscqaeww4gfrpmjs7f.png','配料：江团鱼，黄豆芽，莲藕',1,'2022-06-10 10:42:42','2026-10-04 21:24:18',NULL,NULL);
INSERT INTO `dish` (`id`, `name`, `category_id`, `price`, `image`, `description`, `status`, `create_time`, `update_time`, `create_user`, `update_user`) VALUES (67,'鮰鱼2斤',16,72.00,'https://res.cloudinary.com/dnlorqmf/image/upload/v1791141866/sky-take-out/bdyrgxefjic73vxw7xjg.png','原料：鮰鱼，黄豆芽，莲藕',1,'2022-06-10 10:43:56','2026-10-04 21:24:29',NULL,NULL);
INSERT INTO `dish` (`id`, `name`, `category_id`, `price`, `image`, `description`, `status`, `create_time`, `update_time`, `create_user`, `update_user`) VALUES (68,'鸡蛋汤',21,4.00,'https://res.cloudinary.com/dnlorqmf/image/upload/v1791141900/sky-take-out/ru3iz0hc1h4ffdqh4hli.png','配料：鸡蛋，紫菜',1,'2022-06-10 10:54:25','2026-10-04 21:25:03',NULL,NULL);
INSERT INTO `dish` (`id`, `name`, `category_id`, `price`, `image`, `description`, `status`, `create_time`, `update_time`, `create_user`, `update_user`) VALUES (69,'平菇豆腐汤',21,6.00,'https://res.cloudinary.com/dnlorqmf/image/upload/v1791141911/sky-take-out/qxektaivkynglnam25kb.png','配料：豆腐，平菇',1,'2022-06-10 10:55:02','2026-10-04 21:25:14',NULL,NULL);
INSERT INTO `dish_flavor` (`id`, `dish_id`, `name`, `value`) VALUES (111,51,'忌口','[\"不要葱\",\"不要蒜\",\"不要香菜\",\"不要辣\"]');
INSERT INTO `dish_flavor` (`id`, `dish_id`, `name`, `value`) VALUES (112,51,'辣度','[\"不辣\",\"微辣\",\"中辣\",\"重辣\"]');
INSERT INTO `dish_flavor` (`id`, `dish_id`, `name`, `value`) VALUES (113,52,'忌口','[\"不要葱\",\"不要蒜\",\"不要香菜\",\"不要辣\"]');
INSERT INTO `dish_flavor` (`id`, `dish_id`, `name`, `value`) VALUES (114,52,'辣度','[\"不辣\",\"微辣\",\"中辣\",\"重辣\"]');
INSERT INTO `dish_flavor` (`id`, `dish_id`, `name`, `value`) VALUES (115,53,'忌口','[\"不要葱\",\"不要蒜\",\"不要香菜\",\"不要辣\"]');
INSERT INTO `dish_flavor` (`id`, `dish_id`, `name`, `value`) VALUES (116,53,'辣度','[\"不辣\",\"微辣\",\"中辣\",\"重辣\"]');
INSERT INTO `dish_flavor` (`id`, `dish_id`, `name`, `value`) VALUES (117,54,'忌口','[\"不要葱\",\"不要蒜\",\"不要香菜\"]');
INSERT INTO `dish_flavor` (`id`, `dish_id`, `name`, `value`) VALUES (118,56,'忌口','[\"不要葱\",\"不要蒜\",\"不要香菜\",\"不要辣\"]');
INSERT INTO `dish_flavor` (`id`, `dish_id`, `name`, `value`) VALUES (119,57,'忌口','[\"不要葱\",\"不要蒜\",\"不要香菜\",\"不要辣\"]');
INSERT INTO `dish_flavor` (`id`, `dish_id`, `name`, `value`) VALUES (120,60,'忌口','[\"不要葱\",\"不要蒜\",\"不要香菜\",\"不要辣\"]');
INSERT INTO `dish_flavor` (`id`, `dish_id`, `name`, `value`) VALUES (121,65,'辣度','[\"不辣\",\"微辣\",\"中辣\",\"重辣\"]');
INSERT INTO `dish_flavor` (`id`, `dish_id`, `name`, `value`) VALUES (122,66,'辣度','[\"不辣\",\"微辣\",\"中辣\",\"重辣\"]');
INSERT INTO `dish_flavor` (`id`, `dish_id`, `name`, `value`) VALUES (123,67,'辣度','[\"不辣\",\"微辣\",\"中辣\",\"重辣\"]');
INSERT INTO `setmeal` (`id`, `category_id`, `name`, `price`, `status`, `description`, `image`, `create_time`, `update_time`, `create_user`, `update_user`) VALUES (35,13,'人气套餐A',58.00,1,'','https://res.cloudinary.com/dnlorqmf/image/upload/v1791142007/sky-take-out/vcotr789dxdsrba1dkkg.png','2026-09-20 18:14:13','2026-10-04 21:33:23',NULL,NULL);
INSERT INTO `setmeal` (`id`, `category_id`, `name`, `price`, `status`, `description`, `image`, `create_time`, `update_time`, `create_user`, `update_user`) VALUES (36,15,'商务套餐A',118.00,1,'','https://res.cloudinary.com/dnlorqmf/image/upload/v1791142255/sky-take-out/pifr6vdfa72dkcxbu0v7.png','2026-10-04 21:32:09','2026-10-08 11:26:20',NULL,NULL);
INSERT INTO `setmeal_dish` (`id`, `setmeal_id`, `dish_id`, `name`, `price`, `copies`) VALUES (72,35,49,'米饭',2.00,1);
INSERT INTO `setmeal_dish` (`id`, `setmeal_id`, `dish_id`, `name`, `price`, `copies`) VALUES (73,35,60,'梅菜扣肉',58.00,1);
INSERT INTO `setmeal_dish` (`id`, `setmeal_id`, `dish_id`, `name`, `price`, `copies`) VALUES (74,35,54,'清炒小油菜',18.00,1);
INSERT INTO `setmeal_dish` (`id`, `setmeal_id`, `dish_id`, `name`, `price`, `copies`) VALUES (75,35,68,'鸡蛋汤',4.00,1);
INSERT INTO `setmeal_dish` (`id`, `setmeal_id`, `dish_id`, `name`, `price`, `copies`) VALUES (76,36,57,'炝炒圆白菜',18.00,1);
INSERT INTO `setmeal_dish` (`id`, `setmeal_id`, `dish_id`, `name`, `price`, `copies`) VALUES (77,36,56,'清炒西兰花',18.00,1);
INSERT INTO `setmeal_dish` (`id`, `setmeal_id`, `dish_id`, `name`, `price`, `copies`) VALUES (78,36,49,'米饭',2.00,1);
INSERT INTO `setmeal_dish` (`id`, `setmeal_id`, `dish_id`, `name`, `price`, `copies`) VALUES (79,36,68,'鸡蛋汤',4.00,1);
INSERT INTO `setmeal_dish` (`id`, `setmeal_id`, `dish_id`, `name`, `price`, `copies`) VALUES (80,36,59,'东坡肘子',138.00,1);

COMMIT;
