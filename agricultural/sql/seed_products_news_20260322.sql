USE nongnong_ecommerce;

START TRANSACTION;

-- ----------------------------
-- 1) 批量导入商品（幂等：按商品名去重）
-- ----------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_seed_products_20260322;
CREATE TEMPORARY TABLE tmp_seed_products_20260322 (
    product_name VARCHAR(100) NOT NULL,
    merchant_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    stock INT NOT NULL,
    stock_warning INT NOT NULL,
    product_img VARCHAR(500),
    product_detail_img VARCHAR(1000),
    product_desc TEXT,
    origin_place VARCHAR(50),
    planting_cycle VARCHAR(50),
    origin_place_detail VARCHAR(100),
    fertilizer_type VARCHAR(50),
    storage_method VARCHAR(50),
    transport_method VARCHAR(50),
    qr_code_url VARCHAR(255)
);

INSERT INTO tmp_seed_products_20260322 (
    product_name, merchant_id, category_id, price, stock, stock_warning,
    product_img, product_detail_img, product_desc, origin_place,
    planting_cycle, origin_place_detail, fertilizer_type, storage_method, transport_method, qr_code_url
) VALUES
('高山草莓礼盒500g', 6, 1, 39.90, 160, 20, 'https://picsum.photos/seed/agri_p01/900/900,https://picsum.photos/seed/agri_p01b/900/900,https://picsum.photos/seed/agri_p01c/900/900', 'https://picsum.photos/seed/agri_p01d/1200/800,https://picsum.photos/seed/agri_p01e/1200/800', '高海拔种植，果香浓郁，酸甜平衡，适合家庭即食与甜品制作。', '四川省/成都市/蒲江县', '春季育苗，冬季采收', '四川省成都市蒲江县寿安镇', '有机肥为主', '冷藏', '冷链运输', 'https://quickchart.io/qr?text=trace_agri_20260322_01'),
('蒲江耙耙柑5斤装', 6, 1, 36.80, 260, 30, 'https://picsum.photos/seed/agri_p02/900/900,https://picsum.photos/seed/agri_p02b/900/900,https://picsum.photos/seed/agri_p02c/900/900', 'https://picsum.photos/seed/agri_p02d/1200/800,https://picsum.photos/seed/agri_p02e/1200/800', '皮薄多汁、果肉细嫩，轻甜不腻，适合作为日常水果补给。', '四川省/成都市/蒲江县', '秋季挂果，冬季采摘', '四川省成都市蒲江县西来镇', '复合有机肥', '阴凉通风', '冷链运输', 'https://quickchart.io/qr?text=trace_agri_20260322_02'),
('资中血橙精选礼盒', 6, 1, 42.00, 180, 20, 'https://picsum.photos/seed/agri_p03/900/900,https://picsum.photos/seed/agri_p03b/900/900,https://picsum.photos/seed/agri_p03c/900/900', 'https://picsum.photos/seed/agri_p03d/1200/800,https://picsum.photos/seed/agri_p03e/1200/800', '果肉细腻，汁水充足，富含花青素，适合榨汁和鲜食。', '四川省/内江市/资中县', '春花冬果', '四川省内江市资中县双龙镇', '生物有机肥', '阴凉储存', '常温快运', 'https://quickchart.io/qr?text=trace_agri_20260322_03'),
('盐源糖心苹果5斤', 6, 1, 45.00, 150, 20, 'https://picsum.photos/seed/agri_p04/900/900,https://picsum.photos/seed/agri_p04b/900/900,https://picsum.photos/seed/agri_p04c/900/900', 'https://picsum.photos/seed/agri_p04d/1200/800,https://picsum.photos/seed/agri_p04e/1200/800', '昼夜温差大形成糖心，口感脆甜，耐储存，家庭囤货友好。', '四川省/凉山州/盐源县', '春季疏花，秋季采摘', '四川省凉山州盐源县梅雨镇', '农家堆肥', '冷藏', '冷链运输', 'https://quickchart.io/qr?text=trace_agri_20260322_04'),
('阿坝蓝莓125g*4', 6, 1, 49.90, 120, 15, 'https://picsum.photos/seed/agri_p05/900/900,https://picsum.photos/seed/agri_p05b/900/900,https://picsum.photos/seed/agri_p05c/900/900', 'https://picsum.photos/seed/agri_p05d/1200/800,https://picsum.photos/seed/agri_p05e/1200/800', '高原蓝莓颗粒饱满，口味清甜，独立小盒包装更便于分食。', '四川省/阿坝州/汶川县', '春季开花，夏季采果', '四川省阿坝州汶川县映秀镇', '有机质改良土', '冷藏', '冷链运输', 'https://quickchart.io/qr?text=trace_agri_20260322_05'),
('攀枝花凯特芒果3斤', 6, 1, 38.50, 210, 25, 'https://picsum.photos/seed/agri_p06/900/900,https://picsum.photos/seed/agri_p06b/900/900,https://picsum.photos/seed/agri_p06c/900/900', 'https://picsum.photos/seed/agri_p06d/1200/800,https://picsum.photos/seed/agri_p06e/1200/800', '香气浓郁、纤维少，成熟后果肉细腻，适合即食与果昔。', '四川省/攀枝花市/仁和区', '春季开花，夏季成熟', '四川省攀枝花市仁和区大龙潭乡', '水肥一体', '阴凉通风', '常温快运', 'https://quickchart.io/qr?text=trace_agri_20260322_06'),

('通江银耳中朵200g', 7, 3, 58.00, 180, 20, 'https://picsum.photos/seed/agri_p07/900/900,https://picsum.photos/seed/agri_p07b/900/900,https://picsum.photos/seed/agri_p07c/900/900', 'https://picsum.photos/seed/agri_p07d/1200/800,https://picsum.photos/seed/agri_p07e/1200/800', '朵形完整、易泡发，适合炖煮甜汤与家常养生食谱。', '四川省/巴中市/通江县', '春季接菌，秋季采收', '四川省巴中市通江县诺江镇', '菌棒有机培养', '干燥密封', '常温运输', 'https://quickchart.io/qr?text=trace_agri_20260322_07'),
('大凉山黑木耳250g', 7, 3, 36.00, 220, 25, 'https://picsum.photos/seed/agri_p08/900/900,https://picsum.photos/seed/agri_p08b/900/900,https://picsum.photos/seed/agri_p08c/900/900', 'https://picsum.photos/seed/agri_p08d/1200/800,https://picsum.photos/seed/agri_p08e/1200/800', '山林环境自然晾晒，泡发率高，口感脆爽，炒炖皆宜。', '四川省/凉山州/西昌市', '春季出耳，夏秋采收', '四川省凉山州西昌市安宁镇', '生态基质', '干燥密封', '常温运输', 'https://quickchart.io/qr?text=trace_agri_20260322_08'),
('青川香菇干300g', 7, 3, 42.00, 170, 20, 'https://picsum.photos/seed/agri_p09/900/900,https://picsum.photos/seed/agri_p09b/900/900,https://picsum.photos/seed/agri_p09c/900/900', 'https://picsum.photos/seed/agri_p09d/1200/800,https://picsum.photos/seed/agri_p09e/1200/800', '香气浓郁，菇形完整，适合炖鸡、煲汤和素食料理。', '四川省/广元市/青川县', '春秋两季采菇', '四川省广元市青川县乔庄镇', '林下菌棒种植', '阴凉密封', '常温运输', 'https://quickchart.io/qr?text=trace_agri_20260322_09'),
('剑阁核桃仁500g', 7, 3, 68.00, 140, 15, 'https://picsum.photos/seed/agri_p10/900/900,https://picsum.photos/seed/agri_p10b/900/900,https://picsum.photos/seed/agri_p10c/900/900', 'https://picsum.photos/seed/agri_p10d/1200/800,https://picsum.photos/seed/agri_p10e/1200/800', '手工去壳，仁大饱满，适合烘焙、冲饮及每日坚果补充。', '四川省/广元市/剑阁县', '秋季采果晾晒', '四川省广元市剑阁县下寺镇', '生态种植', '密封防潮', '常温运输', 'https://quickchart.io/qr?text=trace_agri_20260322_10'),
('若尔盖牦牛肉干120g', 8, 3, 39.80, 260, 30, 'https://picsum.photos/seed/agri_p11/900/900,https://picsum.photos/seed/agri_p11b/900/900,https://picsum.photos/seed/agri_p11c/900/900', 'https://picsum.photos/seed/agri_p11d/1200/800,https://picsum.photos/seed/agri_p11e/1200/800', '高原牧场原料，纹理紧实，越嚼越香，追剧和出行都方便。', '四川省/阿坝州/若尔盖县', '全年放牧，分批加工', '四川省阿坝州若尔盖县唐克镇', '天然牧草喂养', '常温避光', '常温运输', 'https://quickchart.io/qr?text=trace_agri_20260322_11'),
('雅安藏茶散茶250g', 7, 3, 55.00, 190, 20, 'https://picsum.photos/seed/agri_p12/900/900,https://picsum.photos/seed/agri_p12b/900/900,https://picsum.photos/seed/agri_p12c/900/900', 'https://picsum.photos/seed/agri_p12d/1200/800,https://picsum.photos/seed/agri_p12e/1200/800', '传统工艺发酵，茶汤红浓，适合日常暖胃饮用。', '四川省/雅安市/雨城区', '春采秋制', '四川省雅安市雨城区草坝镇', '生态茶园管理', '干燥密封', '常温运输', 'https://quickchart.io/qr?text=trace_agri_20260322_12'),

('五常大米5kg', 7, 2, 69.90, 300, 40, 'https://picsum.photos/seed/agri_p13/900/900,https://picsum.photos/seed/agri_p13b/900/900,https://picsum.photos/seed/agri_p13c/900/900', 'https://picsum.photos/seed/agri_p13d/1200/800,https://picsum.photos/seed/agri_p13e/1200/800', '米粒油润，蒸煮后口感软糯香甜，适合家庭主食。', '黑龙江省/哈尔滨市/五常市', '春播秋收', '黑龙江省哈尔滨市五常市拉林镇', '有机肥', '阴凉干燥', '常温运输', 'https://quickchart.io/qr?text=trace_agri_20260322_13'),
('小榨菜籽油5L', 7, 2, 119.00, 180, 20, 'https://picsum.photos/seed/agri_p14/900/900,https://picsum.photos/seed/agri_p14b/900/900,https://picsum.photos/seed/agri_p14c/900/900', 'https://picsum.photos/seed/agri_p14d/1200/800,https://picsum.photos/seed/agri_p14e/1200/800', '传统压榨工艺，油香浓郁，适合川味炒菜与凉拌。', '四川省/绵阳市/三台县', '秋季采籽，冬季压榨', '四川省绵阳市三台县芦溪镇', '非转基因原料', '密封避光', '常温运输', 'https://quickchart.io/qr?text=trace_agri_20260322_14'),
('农家红薯粉条1kg', 7, 2, 29.80, 260, 30, 'https://picsum.photos/seed/agri_p15/900/900,https://picsum.photos/seed/agri_p15b/900/900,https://picsum.photos/seed/agri_p15c/900/900', 'https://picsum.photos/seed/agri_p15d/1200/800,https://picsum.photos/seed/agri_p15e/1200/800', '纯薯浆制作，煮后爽滑弹韧，火锅炖菜都合适。', '四川省/南充市/仪陇县', '秋季采薯加工', '四川省南充市仪陇县马鞍镇', '无添加', '干燥保存', '常温运输', 'https://quickchart.io/qr?text=trace_agri_20260322_15'),
('青稞面1kg', 7, 2, 22.00, 230, 25, 'https://picsum.photos/seed/agri_p16/900/900,https://picsum.photos/seed/agri_p16b/900/900,https://picsum.photos/seed/agri_p16c/900/900', 'https://picsum.photos/seed/agri_p16d/1200/800,https://picsum.photos/seed/agri_p16e/1200/800', '高原青稞研磨，麦香明显，适合面食和营养代餐。', '四川省/甘孜州/理塘县', '春播秋收', '四川省甘孜州理塘县高城镇', '绿色种植', '干燥保存', '常温运输', 'https://quickchart.io/qr?text=trace_agri_20260322_16'),
('有机黄豆2.5kg', 7, 2, 35.00, 280, 35, 'https://picsum.photos/seed/agri_p17/900/900,https://picsum.photos/seed/agri_p17b/900/900,https://picsum.photos/seed/agri_p17c/900/900', 'https://picsum.photos/seed/agri_p17d/1200/800,https://picsum.photos/seed/agri_p17e/1200/800', '颗粒饱满，豆香自然，适合打豆浆、做豆腐和炖菜。', '四川省/德阳市/中江县', '夏播秋收', '四川省德阳市中江县凯江镇', '有机肥', '防潮密封', '常温运输', 'https://quickchart.io/qr?text=trace_agri_20260322_17'),
('土蜂蜜500g', 6, 2, 59.90, 190, 20, 'https://picsum.photos/seed/agri_p18/900/900,https://picsum.photos/seed/agri_p18b/900/900,https://picsum.photos/seed/agri_p18c/900/900', 'https://picsum.photos/seed/agri_p18d/1200/800,https://picsum.photos/seed/agri_p18e/1200/800', '百花蜜源，口感醇厚，冲饮、烘焙和早餐搭配都不错。', '四川省/雅安市/天全县', '四季采蜜', '四川省雅安市天全县始阳镇', '自然放养蜂群', '常温避光', '常温运输', 'https://quickchart.io/qr?text=trace_agri_20260322_18'),

('林下散养土鸡1.2kg', 8, 4, 58.00, 120, 15, 'https://picsum.photos/seed/agri_p19/900/900,https://picsum.photos/seed/agri_p19b/900/900,https://picsum.photos/seed/agri_p19c/900/900', 'https://picsum.photos/seed/agri_p19d/1200/800,https://picsum.photos/seed/agri_p19e/1200/800', '林地散养，肉质紧实，适合炖汤和家常红烧。', '四川省/绵阳市/三台县', '120天以上散养', '四川省绵阳市三台县富顺镇', '玉米谷物饲喂', '冷鲜', '冷链运输', 'https://quickchart.io/qr?text=trace_agri_20260322_19'),
('冷鲜土猪前腿肉500g', 8, 4, 29.90, 220, 25, 'https://picsum.photos/seed/agri_p20/900/900,https://picsum.photos/seed/agri_p20b/900/900,https://picsum.photos/seed/agri_p20c/900/900', 'https://picsum.photos/seed/agri_p20d/1200/800,https://picsum.photos/seed/agri_p20e/1200/800', '当日分割冷鲜直发，肥瘦均衡，炒菜、炖煮都适合。', '四川省/成都市/郫都区', '180天生态饲养', '四川省成都市郫都区唐昌镇', '谷物配比饲料', '冷鲜', '冷链运输', 'https://quickchart.io/qr?text=trace_agri_20260322_20'),
('农家土鸡蛋40枚', 8, 4, 52.00, 260, 30, 'https://picsum.photos/seed/agri_p21/900/900,https://picsum.photos/seed/agri_p21b/900/900,https://picsum.photos/seed/agri_p21c/900/900', 'https://picsum.photos/seed/agri_p21d/1200/800,https://picsum.photos/seed/agri_p21e/1200/800', '蛋黄饱满，蛋香明显，适合蒸蛋、烘焙与早餐煎蛋。', '四川省/遂宁市/射洪市', '自然散养产蛋', '四川省遂宁市射洪市太和镇', '谷物饲喂', '常温通风', '防震运输', 'https://quickchart.io/qr?text=trace_agri_20260322_21'),
('生态鸭肉1kg', 8, 4, 45.00, 170, 20, 'https://picsum.photos/seed/agri_p22/900/900,https://picsum.photos/seed/agri_p22b/900/900,https://picsum.photos/seed/agri_p22c/900/900', 'https://picsum.photos/seed/agri_p22d/1200/800,https://picsum.photos/seed/agri_p22e/1200/800', '生态水域养殖，肉质细嫩，适合红烧、卤制和炖汤。', '四川省/乐山市/夹江县', '90天生态养殖', '四川省乐山市夹江县木城镇', '谷物饲喂', '冷鲜', '冷链运输', 'https://quickchart.io/qr?text=trace_agri_20260322_22'),
('高原牦牛里脊500g', 8, 4, 79.00, 110, 15, 'https://picsum.photos/seed/agri_p23/900/900,https://picsum.photos/seed/agri_p23b/900/900,https://picsum.photos/seed/agri_p23c/900/900', 'https://picsum.photos/seed/agri_p23d/1200/800,https://picsum.photos/seed/agri_p23e/1200/800', '高原自然放牧，肉味浓郁，适合煎烤与快炒。', '四川省/甘孜州/理塘县', '全年天然牧场放养', '四川省甘孜州理塘县甲洼镇', '天然牧草', '冷冻', '冷链运输', 'https://quickchart.io/qr?text=trace_agri_20260322_23'),
('川味腊肠500g', 8, 4, 38.80, 240, 30, 'https://picsum.photos/seed/agri_p24/900/900,https://picsum.photos/seed/agri_p24b/900/900,https://picsum.photos/seed/agri_p24c/900/900', 'https://picsum.photos/seed/agri_p24d/1200/800,https://picsum.photos/seed/agri_p24e/1200/800', '传统川味配方，咸香微辣，蒸炒焖饭都很下饭。', '四川省/眉山市/东坡区', '冬季腌制晾晒', '四川省眉山市东坡区尚义镇', '精选猪肉配料', '阴凉干燥', '常温运输', 'https://quickchart.io/qr?text=trace_agri_20260322_24');

INSERT INTO product_info (
    product_name, merchant_id, category_id, price, stock, stock_warning,
    product_img, product_detail_img, product_desc, origin_place,
    status, is_unsalable, sales_volume, score, delete_flag
)
SELECT
    t.product_name, t.merchant_id, t.category_id, t.price, t.stock, t.stock_warning,
    t.product_img, t.product_detail_img, t.product_desc, t.origin_place,
    1, 0, 0, 5.0, 0
FROM tmp_seed_products_20260322 t
WHERE NOT EXISTS (
    SELECT 1 FROM product_info p
    WHERE p.product_name = t.product_name
      AND p.delete_flag = 0
);

INSERT INTO product_trace (
    product_id, planting_cycle, origin_place_detail, fertilizer_type, storage_method, transport_method, qr_code_url, delete_flag
)
SELECT
    p.id, t.planting_cycle, t.origin_place_detail, t.fertilizer_type, t.storage_method, t.transport_method, t.qr_code_url, 0
FROM tmp_seed_products_20260322 t
JOIN product_info p ON p.product_name = t.product_name AND p.delete_flag = 0
LEFT JOIN product_trace pt ON pt.product_id = p.id AND pt.delete_flag = 0
WHERE pt.id IS NULL;

DROP TEMPORARY TABLE IF EXISTS tmp_seed_products_20260322;

-- ----------------------------
-- 2) 批量导入助农新闻（幂等：按标题去重）
-- ----------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_seed_news_20260322;
CREATE TEMPORARY TABLE tmp_seed_news_20260322 (
    title VARCHAR(100) NOT NULL,
    category_id BIGINT NOT NULL,
    cover_img VARCHAR(255),
    content TEXT NOT NULL,
    view_count INT NOT NULL
);

INSERT INTO tmp_seed_news_20260322 (title, category_id, cover_img, content, view_count) VALUES
('2026年春季农产品冷链补贴政策解读', 1, 'https://picsum.photos/seed/agri_n01/1200/700', '# 政策要点\n\n为降低生鲜流通损耗，平台联合地方部门发布冷链补贴细则：\n\n- 冷链订单按单补贴运费；\n- 偏远乡镇线路提高补贴上限；\n- 商家需保持真实物流轨迹与签收记录。\n\n建议商家在发货前核对冷链资质和运单信息。', 126),
('农村电商助农奖补资金申报流程（2026版）', 1, 'https://picsum.photos/seed/agri_n02/1200/700', '# 申报流程\n\n1. 提交店铺经营与订单材料；\n2. 平台初审后进入复核；\n3. 公示通过后拨付奖补。\n\n请确保票据、运单、订单号一致，避免因材料缺失影响进度。', 97),
('绿色种养循环农业试点支持范围公布', 1, 'https://picsum.photos/seed/agri_n03/1200/700', '# 重点支持方向\n\n- 有机肥替代化肥；\n- 畜禽粪污资源化利用；\n- 生态种养结合示范。\n\n平台将持续发布入选地区与申报窗口时间。', 83),
('县域物流共同配送补贴标准调整通知', 1, 'https://picsum.photos/seed/agri_n04/1200/700', '# 通知摘要\n\n为提升末端配送效率，县域共同配送补贴标准优化如下：\n\n- 高频线路提高补贴比例；\n- 对偏远村点增加单票补助；\n- 异常签收需在48小时内说明。', 74),
('农产品质量安全追溯新规实施提醒', 1, 'https://picsum.photos/seed/agri_n05/1200/700', '# 实施提醒\n\n商家上传商品时需补齐产地、施肥、运输等追溯信息。\n\n缺失追溯字段将影响商品审核与推荐曝光。', 66),
('2026年助农电商培训计划启动', 1, 'https://picsum.photos/seed/agri_n06/1200/700', '# 培训方向\n\n围绕商品上架规范、售后处理、直播短视频运营开展分阶段培训。\n\n报名后可在商家端查看课程安排。', 58),

('返乡青年打造猕猴桃品牌，订单同比增长210%', 2, 'https://picsum.photos/seed/agri_n07/1200/700', '# 案例故事\n\n通过标准化分级与溯源展示，合作社提升了复购率。\n\n关键经验：\n\n- 稳定品控；\n- 优化包装；\n- 主打产地故事与口碑评价。', 189),
('山区土鸡蛋合作社实现“以销定产”', 2, 'https://picsum.photos/seed/agri_n08/1200/700', '# 运营亮点\n\n合作社将历史销量与节令需求结合，分批次组织生产，减少滞销风险。\n\n售后响应时效提升后，店铺评分同步上涨。', 143),
('村集体联营腊味工坊，带动周边农户增收', 2, 'https://picsum.photos/seed/agri_n09/1200/700', '# 增收路径\n\n通过统一原料标准、统一包装与统一线上渠道，单季销量创新高。\n\n项目还带动本地就业与包装物流服务。', 132),
('高原牦牛肉品牌化实践：从散卖到精品礼盒', 2, 'https://picsum.photos/seed/agri_n10/1200/700', '# 升级动作\n\n- 建立分割标准；\n- 完善冷链履约；\n- 打造节庆礼盒。\n\n品牌化后客单价明显提升。', 121),
('银耳产业链延伸，村民人均收入稳步提升', 2, 'https://picsum.photos/seed/agri_n11/1200/700', '# 经验总结\n\n围绕“种植 + 初加工 + 电商销售”构建闭环，减少中间流通成本。\n\n平台评价体系帮助新商家更快建立信任。', 111),
('农家蜂蜜数字化上行，复购率持续走高', 2, 'https://picsum.photos/seed/agri_n12/1200/700', '# 实践要点\n\n通过短视频讲解采蜜过程，并结合图文追溯，提高了用户信任度。\n\n老客订阅制也带来了稳定现金流。', 98),

('蒲江春果上市：耙耙柑与草莓进入发货高峰', 3, 'https://picsum.photos/seed/agri_n13/1200/700', '# 产地动态\n\n近期气温回升，春果采收与发货量增长明显。\n\n建议消费者错峰下单，商家注意冷链时效。', 167),
('凉山苹果进入最佳采后分选期', 3, 'https://picsum.photos/seed/agri_n14/1200/700', '# 一线观察\n\n当地合作社正在推进机械化分选，提升规格稳定性。\n\n预计本周起优果供应量增加。', 154),
('三台禽蛋主产区启动质量抽检周', 3, 'https://picsum.photos/seed/agri_n15/1200/700', '# 检测安排\n\n平台联动抽检机构，对重点品类开展随机抽样与批次追踪。\n\n抽检结果将同步到商品详情页。', 139),
('通江食用菌基地扩容，黑木耳产能提升', 3, 'https://picsum.photos/seed/agri_n16/1200/700', '# 基地进展\n\n新建菌棚投入使用后，预计旺季供应能力提高约30%。\n\n加工环节也同步升级了干燥与分拣设备。', 128),
('雅安茶区春茶采摘窗口提前开启', 3, 'https://picsum.photos/seed/agri_n17/1200/700', '# 采摘提示\n\n受天气影响，部分茶区采摘时间提前。\n\n平台建议商家及时更新库存与发货时效说明。', 119),
('阿坝蓝莓基地发布本季采摘与预售计划', 3, 'https://picsum.photos/seed/agri_n18/1200/700', '# 预售节奏\n\n基地将分批释放采摘配额，优先保障已预售订单。\n\n消费者可关注商品详情页的发货时间提醒。', 102);

INSERT INTO news (
    title, category_id, content, cover_img, view_count, audit_status, create_by, delete_flag
)
SELECT
    t.title, t.category_id, t.content, t.cover_img, t.view_count, 1, 4, 0
FROM tmp_seed_news_20260322 t
WHERE NOT EXISTS (
    SELECT 1 FROM news n
    WHERE n.title = t.title
      AND n.delete_flag = 0
);

DROP TEMPORARY TABLE IF EXISTS tmp_seed_news_20260322;

COMMIT;
