USE nongnong_ecommerce;
INSERT INTO merchant_info (phone, merchant_name, contact_person, contact_phone, audit_status, status) VALUES ('13800000001', '蒲江生态果园', '张三', '13800000001', 1, 1);
SET @merchant_id1 = LAST_INSERT_ID();
INSERT INTO shop_info (merchant_id, shop_name) VALUES (@merchant_id1, '蒲江生态果园');

INSERT INTO merchant_info (phone, merchant_name, contact_person, contact_phone, audit_status, status) VALUES ('13800000002', '三台养殖合作社', '李四', '13800000002', 1, 1);
SET @merchant_id2 = LAST_INSERT_ID();
INSERT INTO shop_info (merchant_id, shop_name) VALUES (@merchant_id2, '三台养殖合作社');

INSERT INTO product_info (product_name, merchant_id, category_id, price, stock, product_img, origin_place, status, sales_volume, score) VALUES 
('四川红心猕猴桃', @merchant_id1, 1, 29.9, 8, 'https://via.placeholder.com/300x300/67c23a/fff?text=Kiwi', '成都市蒲江', 1, 156, 4.9),
('农家土鸡蛋 30枚', @merchant_id2, 1, 45.0, 120, 'https://via.placeholder.com/300x300/e6a23c/fff?text=Eggs', '绵阳市三台', 1, 89, 4.8),
('安岳柠檬 5斤装', @merchant_id1, 1, 19.9, 500, 'https://via.placeholder.com/300x300/f56c6c/fff?text=Lemon', '资阳市安岳', 1, 230, 4.7),
('通江银耳 特级', @merchant_id2, 2, 88.0, 45, 'https://via.placeholder.com/300x300/409eff/fff?text=Fungus', '巴中市通江', 1, 67, 5.0);

INSERT INTO news (title, category_id, content, create_by, audit_status) VALUES
('四川启动助农增收计划', 1, '四川省农业农村厅近日印发《2026年助农增收工作方案》，旨在通过多渠道帮助农户增加收入...', 1, 1),
('蒲江猕猴桃丰收在望', 3, '今年气候适宜，蒲江县10万亩红心猕猴桃长势喜人，预计总产量将达15万吨，果农们正忙着采摘...', 1, 1);
