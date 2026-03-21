export const PRODUCT_CATEGORY_OPTIONS = [
  { label: '生鲜果蔬', value: 1 },
  { label: '粮油副食', value: 2 },
  { label: '干货特产', value: 3 },
  { label: '畜禽肉蛋', value: 4 }
];

export const PRODUCT_CATEGORY_NAME_MAP: Record<number, string> = PRODUCT_CATEGORY_OPTIONS.reduce(
  (acc, cur) => {
    acc[cur.value] = cur.label;
    return acc;
  },
  {} as Record<number, string>
);

const PRODUCT_CATEGORY_ALIAS_MAP: Record<string, string> = {
  生鲜: '生鲜果蔬',
  果蔬: '生鲜果蔬',
  生鲜果蔬: '生鲜果蔬',
  粮油: '粮油副食',
  副食: '粮油副食',
  粮油副食: '粮油副食',
  干货: '干货特产',
  特产: '干货特产',
  干货特产: '干货特产',
  畜禽: '畜禽肉蛋',
  肉蛋: '畜禽肉蛋',
  畜禽肉蛋: '畜禽肉蛋'
};

export const getProductCategoryName = (
  categoryId?: number | string | null,
  categoryName?: string | null
) => {
  const id = Number(categoryId);
  if (!Number.isNaN(id) && PRODUCT_CATEGORY_NAME_MAP[id]) {
    return PRODUCT_CATEGORY_NAME_MAP[id];
  }

  const normalizedName = String(categoryName || '').trim();
  if (!normalizedName) {
    return '';
  }

  return PRODUCT_CATEGORY_ALIAS_MAP[normalizedName] || normalizedName;
};

