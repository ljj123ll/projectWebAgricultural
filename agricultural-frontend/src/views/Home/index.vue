<template>
  <div class="home-container">
    <!-- Banner -->
    <div class="banner">
       <el-carousel height="500px">
        <el-carousel-item v-for="item in 3" :key="item">
           <div class="banner-item" :style="{ backgroundColor: item % 2 ? '#99a9bf' : '#d3dce6' }">
               <h1>助农扶贫，连接你我</h1>
               <p>精选优质农产品，源头直供</p>
           </div>
        </el-carousel-item>
      </el-carousel>
    </div>

    <div class="container main-content">
        <!-- Hot Products -->
        <div class="section">
            <h2 class="section-title">热销助农</h2>
            <div class="product-grid">
                <div class="product-card" v-for="item in hotProducts" :key="item.id" @click="goToDetail(item.id)">
                    <div class="img-wrapper">
                        <img :src="item.productImg" :alt="item.productName">
                    </div>
                    <div class="info">
                        <h3 class="name">{{ item.productName }}</h3>
                        <p class="desc">{{ item.productDesc }}</p>
                        <div class="price-row">
                            <span class="price">¥{{ item.price }}</span>
                            <span class="sales">销量 {{ item.salesVolume }}</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- News -->
        <div class="section">
             <h2 class="section-title">助农资讯</h2>
             <div class="news-list">
                 <div class="news-item" v-for="news in newsList" :key="news.id">
                     <span class="tag">新闻</span>
                     <span class="title">{{ news.title }}</span>
                     <span class="date">{{ news.createTime }}</span>
                 </div>
             </div>
        </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { getHomeData, listNews } from '@/apis/user';

const router = useRouter();
const hotProducts = ref<any[]>([]);
const newsList = ref<any[]>([]);

const loadData = async () => {
    try {
        const res = await getHomeData();
        if (res?.hotList) hotProducts.value = res.hotList;
        if (res?.newsList) newsList.value = res.newsList;
        if (!res?.newsList) {
            const news = await listNews({ pageNum: 1, pageSize: 6 });
            if (news?.list) newsList.value = news.list;
        }
    } catch (e) {
        console.error(e);
    }
};

const goToDetail = (id: number) => {
    router.push(`/product/${id}`);
};

onMounted(() => {
    loadData();
});
</script>

<style scoped lang="scss">
.home-container {
    min-height: 100vh;
}

.banner {
    width: 100%;
    .banner-item {
        height: 100%;
        display: flex;
        flex-direction: column;
        justify-content: center;
        align-items: center;
        color: white;
        h1 { font-size: 48px; margin-bottom: 20px; }
        p { font-size: 24px; }
    }
}

.main-content {
    padding: 40px 0;
}

.section {
    margin-bottom: 60px;

    .section-title {
        font-size: 28px;
        font-weight: 600;
        text-align: center;
        margin-bottom: 40px;
        position: relative;
        
        &::after {
            content: '';
            display: block;
            width: 40px;
            height: 3px;
            background: #409EFF;
            margin: 10px auto 0;
        }
    }
}

.product-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 20px;

    .product-card {
        background: white;
        border-radius: 8px;
        overflow: hidden;
        cursor: pointer;
        transition: transform 0.3s, box-shadow 0.3s;
        
        &:hover {
            transform: translateY(-5px);
            box-shadow: 0 10px 20px rgba(0,0,0,0.1);
        }

        .img-wrapper {
            width: 100%;
            height: 200px;
            overflow: hidden;
            img {
                width: 100%;
                height: 100%;
                object-fit: cover;
                transition: transform 0.3s;
            }
        }

        &:hover .img-wrapper img {
            transform: scale(1.05);
        }

        .info {
            padding: 15px;

            .name {
                font-size: 16px;
                font-weight: 600;
                margin-bottom: 5px;
                white-space: nowrap;
                overflow: hidden;
                text-overflow: ellipsis;
            }

            .desc {
                font-size: 12px;
                color: #999;
                margin-bottom: 10px;
                height: 36px;
                overflow: hidden;
            }

            .price-row {
                display: flex;
                justify-content: space-between;
                align-items: center;

                .price {
                    color: #f56c6c;
                    font-size: 18px;
                    font-weight: bold;
                }

                .sales {
                    color: #999;
                    font-size: 12px;
                }
            }
        }
    }
}

.news-list {
    background: white;
    padding: 20px;
    border-radius: 8px;

    .news-item {
        display: flex;
        align-items: center;
        padding: 15px 0;
        border-bottom: 1px solid #eee;
        
        &:last-child { border-bottom: none; }

        .tag {
            background: #f0f9eb;
            color: #67c23a;
            padding: 2px 8px;
            border-radius: 4px;
            font-size: 12px;
            margin-right: 10px;
        }

        .title {
            flex: 1;
            font-size: 14px;
            cursor: pointer;
            &:hover { color: #409EFF; }
        }

        .date {
            color: #999;
            font-size: 12px;
        }
    }
}
</style>
