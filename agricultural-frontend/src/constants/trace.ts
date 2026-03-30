export type TraceFieldType = 'input' | 'textarea' | 'select' | 'date'

export interface TraceFieldOption {
  label: string
  value: string
}

export interface TraceFieldDefinition {
  key: string
  label: string
  type: TraceFieldType
  placeholder?: string
  options?: TraceFieldOption[]
}

const SHARED_OPTIONS = {
  growingMode: [
    { label: '露天自然生长', value: '露天自然生长' },
    { label: '设施农业', value: '设施农业' },
    { label: '有机栽培', value: '有机栽培' }
  ],
  irrigation: [
    { label: '山泉灌溉', value: '山泉灌溉' },
    { label: '滴灌节水', value: '滴灌节水' },
    { label: '河渠灌溉', value: '河渠灌溉' }
  ],
  packageSpec: [
    { label: '500g/袋', value: '500g/袋' },
    { label: '1kg/袋', value: '1kg/袋' },
    { label: '礼盒装', value: '礼盒装' }
  ],
  breeding: [
    { label: '散养放养', value: '散养放养' },
    { label: '标准化养殖', value: '标准化养殖' },
    { label: '生态循环养殖', value: '生态循环养殖' }
  ]
}

export const TRACE_FIELD_MAP: Record<number, TraceFieldDefinition[]> = {
  1: [
    { key: 'variety', label: '品种规格', type: 'input', placeholder: '如：红心猕猴桃 80-100g/枚' },
    { key: 'plantingMethod', label: '种植方式', type: 'select', options: SHARED_OPTIONS.growingMode, placeholder: '请选择种植方式' },
    { key: 'irrigationMethod', label: '灌溉方式', type: 'select', options: SHARED_OPTIONS.irrigation, placeholder: '请选择灌溉方式' },
    { key: 'pestControl', label: '病虫害防治', type: 'textarea', placeholder: '填写绿色防控、物理防控或人工除草等措施' },
    { key: 'pickingWindow', label: '采摘时段', type: 'input', placeholder: '如：清晨 5:30-8:30 人工采摘' },
    { key: 'qualityGrade', label: '品质等级', type: 'input', placeholder: '如：一级果 / A 级 / 礼盒级' }
  ],
  2: [
    { key: 'rawMaterialSource', label: '原粮来源', type: 'input', placeholder: '如：黑龙江五常基地稻谷' },
    { key: 'processingTechnology', label: '加工工艺', type: 'textarea', placeholder: '如：低温压榨、石磨碾磨、传统发酵' },
    { key: 'millingDate', label: '碾磨压榨日期', type: 'date', placeholder: '请选择日期' },
    { key: 'ingredientList', label: '配料组成', type: 'textarea', placeholder: '填写主要原料或配料表' },
    { key: 'shelfLife', label: '保质周期', type: 'input', placeholder: '如：常温 12 个月' },
    { key: 'packageSpec', label: '规格净含量', type: 'select', options: SHARED_OPTIONS.packageSpec, placeholder: '请选择规格' }
  ],
  3: [
    { key: 'specialtyType', label: '特产类型', type: 'input', placeholder: '如：菌菇干货 / 茶叶 / 坚果蜜饯' },
    { key: 'dryingMethod', label: '干制方式', type: 'input', placeholder: '如：阳光晾晒 / 低温烘干' },
    { key: 'roastingLevel', label: '烘焙火候', type: 'input', placeholder: '如：中度烘焙 / 文火炒制' },
    { key: 'storageHumidity', label: '储藏环境', type: 'input', placeholder: '如：阴凉干燥，湿度低于 50%' },
    { key: 'flavorProfile', label: '风味特征', type: 'textarea', placeholder: '描述香气、口感与回味' },
    { key: 'recommendedUse', label: '推荐吃法', type: 'textarea', placeholder: '如：煲汤、冲泡、凉拌、即食' }
  ],
  4: [
    { key: 'breed', label: '品种来源', type: 'input', placeholder: '如：若尔盖牦牛、本地散养土鸡' },
    { key: 'breedingMethod', label: '养殖方式', type: 'select', options: SHARED_OPTIONS.breeding, placeholder: '请选择养殖方式' },
    { key: 'feedType', label: '饲喂方式', type: 'input', placeholder: '如：玉米谷物 + 牧草 + 山泉水' },
    { key: 'quarantineStatus', label: '检疫情况', type: 'input', placeholder: '如：检疫合格 / 产地检疫编号' },
    { key: 'slaughterOrLayingDate', label: '屠宰产蛋日期', type: 'date', placeholder: '请选择日期' },
    { key: 'coldChainTemperature', label: '冷链温度', type: 'input', placeholder: '如：0-4℃ 全程冷链' }
  ]
}

export const getTraceFieldDefinitions = (categoryId?: number | string | null) => {
  const normalized = Number(categoryId)
  return TRACE_FIELD_MAP[normalized] || []
}

export const getTraceCycleLabel = (categoryId?: number | string | null) => {
  const normalized = Number(categoryId)
  if (normalized === 4) return '养殖周期'
  if (normalized === 2 || normalized === 3) return '生产周期'
  return '种植周期'
}

export const getProductionDateLabel = (categoryId?: number | string | null) => {
  const normalized = Number(categoryId)
  if (normalized === 2) return '加工建档日期'
  if (normalized === 4) return '养殖建档日期'
  return '生产建档日期'
}

export const getHarvestDateLabel = (categoryId?: number | string | null) => {
  const normalized = Number(categoryId)
  if (normalized === 4) return '出栏/产蛋日期'
  if (normalized === 2) return '原料收储日期'
  if (normalized === 3) return '采收/干制日期'
  return '采摘日期'
}
