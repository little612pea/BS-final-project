<template>
  <div>
    <div class="main" style="overflow-y: hidden; ">

      <el-container >
        <el-header class="title" style="display: flex; justify-content: space-between; align-items: center; padding: 0 20px;">
          <!-- 左侧 Logo 和网站标题 -->
          <div style="display: flex; align-items: center;">
            <img src="./icons/logo.png" style="margin-right: 20px; height: 40px;" />
            <span style="font-size: large; font-family: 'Microsoft YaHei'; color: black; font-weight: bold;">
        商品比价网站
      </span>
            <span style="margin-left: 40px; color: rgba(0, 0, 0, 0.2);">
        《BS体系软件设计》大程
      </span>
          </div>

          <!-- 右侧 用户问候和按钮 -->
          <div style="display: flex; align-items: center;">
            <div v-if="username" style="display: flex; align-items: center;">
              <!-- 用户头像 -->
              <el-avatar :src="userAvatar" style="margin-right: 10px;"></el-avatar>
              <!-- 用户问候 -->
              <span class="user" style="margin-right: 20px;">你好：{{ this.$store.state.username }}</span>
              <!-- 登出按钮 -->
              <el-button type="primary" @click="logoutUser">登出</el-button>
            </div>
            <div v-else>
              <!-- 登录按钮 -->
              <el-button type="primary" @click="loginUser">登录</el-button>
            </div>
          </div>
        </el-header>
        <el-container style="width: 100%; ">
          <el-aside class="aside" style="display: flex;">
            <el-menu active-text-color="#ffd04b" background-color="#0270c1" default-active="crawler" text-color="#fff"
                     style="height:100%; width: 100%;" :router="true">
              <el-menu-item index="product">
                <el-icon>
                  <Reading />
                </el-icon>
                <span>商品比价</span>
              </el-menu-item>
              <el-menu-item index="card">
                <el-icon>
                  <Postcard />
                </el-icon>
                <span>借书证管理</span>
              </el-menu-item>
              <el-menu-item index="borrow">
                <el-icon>
                  <Tickets />
                </el-icon>
                <span>查询历史记录</span>
              </el-menu-item>
              <el-menu-item index="space">
                <el-icon>
                  <Tickets />
                </el-icon>
                <span>我的空间</span>
              </el-menu-item>

            </el-menu>
          </el-aside>

          <el-main style="height: 100%; width: 100%; ">
            <el-scrollbar height="100%">
              <RouterView class="content" style="height: 90vh; max-height: 100%; background-color: white; color: black;" />
            </el-scrollbar>

          </el-main>
        </el-container>
      </el-container>
    </div>
  </div>
</template>

<script>
import { RouterView } from 'vue-router';
import { ElContainer, ElHeader, ElAside, ElMain, ElMenu, ElMenuItem, ElScrollbar, ElIcon } from 'element-plus';
import { Reading, Postcard, Tickets } from '@element-plus/icons-vue';
import { mapState } from 'vuex';
import defaultAvatar from '@/assets/img/logo.png';

export default {
  data() {
    return {
      newUsername: ''
    };
  },
  computed: {
    // 获取 Vuex 中的 username 状态
    ...mapState(['username']),
    userAvatar() {
      // 获取用户头像，如果没有则显示默认头像
      return this.$store.state.userAvatar || defaultAvatar;
    }
  },
  methods: {
    loginUser() {
      if (!this.username) { // 如果用户未登录
        this.$router.push('/login'); // 跳转到 /login 界面
      } else {
        alert('您已登录');
      }
    },
    logoutUser() {
      // 通过 this.$store.dispatch 调用 logout action
      this.$store.dispatch('logout');
    }
  }
};
</script>


<style scoped>
#app {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  background-color: #dcdcdc;
  width: 100vw;
  height: 100vh;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.main {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  width: 100%;
  min-height: 100%;
  height: auto;
  background-color: #dcdcdc;

}

.title {
  background-color: #ffffff;
  height: 60px;
}

.aside {
  min-height: calc(100vh - 60px);
  width: 180px;
  background-color: red;
}

</style>
