USE nongnong_ecommerce;

START TRANSACTION;

INSERT INTO product_category (id, category_name, parent_id, category_level, delete_flag, create_time)
VALUES
    (1, '生鲜果蔬', 0, 1, 0, NOW()),
    (2, '粮油副食', 0, 1, 0, NOW()),
    (3, '干货特产', 0, 1, 0, NOW()),
    (4, '畜禽肉蛋', 0, 1, 0, NOW())
ON DUPLICATE KEY UPDATE
    category_name = VALUES(category_name),
    parent_id = VALUES(parent_id),
    category_level = VALUES(category_level),
    delete_flag = VALUES(delete_flag);

UPDATE product_info
SET category_id = CASE
    WHEN category_id = 2 THEN 103
    WHEN category_id = 3 THEN 102
    ELSE category_id
END
WHERE category_id IN (2, 3);

UPDATE product_info
SET category_id = 4
WHERE category_id = 1
  AND (
      product_name LIKE '%鸡蛋%'
      OR product_name LIKE '%鸭蛋%'
      OR product_name LIKE '%鹅蛋%'
      OR product_name LIKE '%土鸡蛋%'
      OR product_name LIKE '%牛肉%'
      OR product_name LIKE '%猪肉%'
      OR product_name LIKE '%鸡肉%'
      OR product_name LIKE '%鸭肉%'
      OR product_name LIKE '%鹅肉%'
      OR product_name LIKE '%牦牛%'
      OR product_name LIKE '%腊肠%'
  );

UPDATE product_info
SET category_id = CASE
    WHEN category_id = 102 THEN 2
    WHEN category_id = 103 THEN 3
    ELSE category_id
END
WHERE category_id IN (102, 103);

UPDATE product_category
SET category_name = CASE id
    WHEN 1 THEN '生鲜果蔬'
    WHEN 2 THEN '粮油副食'
    WHEN 3 THEN '干货特产'
    WHEN 4 THEN '畜禽肉蛋'
    ELSE category_name
END,
    parent_id = 0,
    category_level = 1,
    delete_flag = 0
WHERE id BETWEEN 1 AND 4;

DELETE FROM product_category
WHERE id > 4;

COMMIT;
