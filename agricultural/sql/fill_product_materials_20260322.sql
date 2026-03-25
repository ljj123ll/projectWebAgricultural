-- 补齐商品具体资料（主图、详情图、描述、产地）
-- 执行环境：MySQL 8.0
-- 执行库：nongnong_ecommerce

START TRANSACTION;

-- 1) 修复主图为空或占位图的商品
UPDATE product_info
SET product_img = CASE
    WHEN category_id = 1 THEN 'https://picsum.photos/seed/fresh_main_01/900/900,https://picsum.photos/seed/fresh_main_02/900/900,https://picsum.photos/seed/fresh_main_03/900/900'
    WHEN category_id = 2 THEN 'https://picsum.photos/seed/dry_main_01/900/900,https://picsum.photos/seed/dry_main_02/900/900,https://picsum.photos/seed/dry_main_03/900/900'
    WHEN category_id = 3 THEN 'https://picsum.photos/seed/grain_main_01/900/900,https://picsum.photos/seed/grain_main_02/900/900,https://picsum.photos/seed/grain_main_03/900/900'
    WHEN category_id = 4 THEN 'https://picsum.photos/seed/meat_main_01/900/900,https://picsum.photos/seed/meat_main_02/900/900,https://picsum.photos/seed/meat_main_03/900/900'
    ELSE 'https://picsum.photos/seed/agri_main_01/900/900,https://picsum.photos/seed/agri_main_02/900/900,https://picsum.photos/seed/agri_main_03/900/900'
END
WHERE delete_flag = 0
  AND (
    product_img IS NULL
    OR TRIM(product_img) = ''
    OR product_img LIKE 'https://via.placeholder.com/%'
  );

-- 2) 补齐详情图（优先复用主图首图 + 两张详情图）
UPDATE product_info
SET product_detail_img = CONCAT(
    SUBSTRING_INDEX(product_img, ',', 1),
    ',',
    CASE
      WHEN category_id = 1 THEN 'https://picsum.photos/seed/fresh_detail_01/1200/800,https://picsum.photos/seed/fresh_detail_02/1200/800'
      WHEN category_id = 2 THEN 'https://picsum.photos/seed/dry_detail_01/1200/800,https://picsum.photos/seed/dry_detail_02/1200/800'
      WHEN category_id = 3 THEN 'https://picsum.photos/seed/grain_detail_01/1200/800,https://picsum.photos/seed/grain_detail_02/1200/800'
      WHEN category_id = 4 THEN 'https://picsum.photos/seed/meat_detail_01/1200/800,https://picsum.photos/seed/meat_detail_02/1200/800'
      ELSE 'https://picsum.photos/seed/agri_detail_01/1200/800,https://picsum.photos/seed/agri_detail_02/1200/800'
    END
)
WHERE delete_flag = 0
  AND (product_detail_img IS NULL OR TRIM(product_detail_img) = '');

-- 3) 补齐商品描述（空描述、过短描述、纯数字描述）
UPDATE product_info
SET product_desc = CASE
    WHEN category_id = 1 THEN CONCAT(product_name, '采用当季鲜采直发，果形饱满、口感清甜，支持冷链配送，适合家庭日常食用。')
    WHEN category_id = 2 THEN CONCAT(product_name, '源自核心产区，原料精选、工艺规范，香味稳定，适合煲汤炖煮和长期储存。')
    WHEN category_id = 3 THEN CONCAT(product_name, '甄选优质原粮原料，生产流程可追溯，口感稳定，满足家庭主食与日常烹饪需求。')
    WHEN category_id = 4 THEN CONCAT(product_name, '选自规范养殖基地，检验合格后冷链发货，肉质紧实鲜香，适合多种家常做法。')
    ELSE CONCAT(product_name, '来自助农产区直供，品质稳定、来源清晰，支持家庭与餐饮场景使用。')
END
WHERE delete_flag = 0
  AND (
    product_desc IS NULL
    OR TRIM(product_desc) = ''
    OR CHAR_LENGTH(TRIM(product_desc)) < 15
    OR TRIM(product_desc) REGEXP '^[0-9]+$'
  );

-- 4) 补齐产地（仅填空）
UPDATE product_info
SET origin_place = CASE
    WHEN category_id = 1 THEN '四川省/成都市/蒲江县'
    WHEN category_id = 2 THEN '四川省/巴中市/通江县'
    WHEN category_id = 3 THEN '四川省/绵阳市/三台县'
    WHEN category_id = 4 THEN '四川省/甘孜州/理塘县'
    ELSE '四川省/成都市/郫都区'
END
WHERE delete_flag = 0
  AND (origin_place IS NULL OR TRIM(origin_place) = '');

COMMIT;

