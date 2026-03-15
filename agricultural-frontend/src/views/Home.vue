<template>
  <div class="home-container">
    <el-header>
      <div class="header-inner">
        <h1>助农电商首页</h1>
        <div>
          <el-button type="primary" v-if="!userStore.token" @click="$router.push('/login')">登录</el-button>
          <el-button v-else @click="$router.push('/user/profile')">个人中心</el-button>
        </div>
      </div>
    </el-header>
    <el-main>
      <el-row :gutter="20">
        <el-col :span="24">
          <el-carousel height="300px">
            <el-carousel-item v-for="item in 4" :key="item">
              <h3 class="small justify-center" text="2xl">{{ item }}</h3>
            </el-carousel-item>
          </el-carousel>
        </el-col>
      </el-row>
      <el-row :gutter="20" style="margin-top: 20px;">
        <el-col :span="6" v-for="i in 8" :key="i">
          <el-card :body-style="{ padding: '0px' }" style="margin-bottom: 20px;">
            <img src="https://shadow.elemecdn.com/app/element/hamburger.9cf7b091-55e9-11e9-a976-7f4d0b07eef6.png" class="image" />
            <div style="padding: 14px">
              <span>好吃的汉堡 {{ i }}</span>
              <div class="bottom">
                <span class="price">¥ 29.9</span>
                <el-button text class="button" @click="$router.push(`/product/${i}`)">查看详情</el-button>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </el-main>
  </div>
</template>

<script setup lang="ts">
import { useUserStore } from '@/stores/modules/user';

const userStore = useUserStore();
</script>

<style scoped>
.header-inner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  height: 60px;
  background-color: #fff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, .12);
}
.el-carousel__item h3 {
  color: #475669;
  opacity: 0.75;
  line-height: 300px;
  margin: 0;
  text-align: center;
  background-color: #99a9bf;
}
.image {
  width: 100%;
  display: block;
}
.bottom {
  margin-top: 13px;
  line-height: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.price {
  color: red;
  font-size: 18px;
}
</style>
