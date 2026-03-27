<template>
  <footer id="site-footer">
    <div class="footer-container">
       <div class="footer-top">
           <h3>助农电商平台</h3>
           <p>提供农产品展示、滞销帮扶、订单履约与售后沟通的一体化服务。</p>
       </div>
       <div class="footer-links">
           <router-link to="/">首页</router-link>
           <router-link to="/products">商品浏览</router-link>
           <router-link to="/unsold">滞销帮扶</router-link>
           <router-link to="/news">助农新闻</router-link>
           <router-link to="/merchant/register">商家入驻</router-link>
       </div>
       <div class="footer-copyright">
           Copyright © 2026 助农电商平台 | 农产品直供、帮扶销售与售后协同服务
       </div>
    </div>
  </footer>
</template>

<style scoped lang="scss">
#site-footer {
    background: #272727;
    color: #999;
    padding: 40px 0;
    text-align: center;

    .footer-container {
        max-width: 1200px;
        margin: 0 auto;
    }

    .footer-top {
        margin-bottom: 30px;
        h3 {
            color: white;
            font-size: 20px;
            margin-bottom: 10px;
        }
    }

    .footer-links {
        display: flex;
        justify-content: center;
        gap: 30px;
        margin-bottom: 30px;

        a {
            color: #999;
            transition: color 0.3s;
            &:hover {
                color: white;
            }
        }
    }

    .footer-copyright {
        font-size: 12px;
        border-top: 1px solid rgba(255,255,255,0.1);
        padding-top: 20px;
    }
}
</style>
