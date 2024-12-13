<template>
  <div class="main-container">
    <!-- Header Section -->
    <header class="header-gradient">
      <div class="header-left">
        <img src="/favicon.ico" alt="Logo" class="header-logo" />
        <div class="header-titles">
          <h1 class="main-title">商品比价网站</h1>
          <p class="sub-title">《BS体系软件设计》大程</p>
        </div>
      </div>
      <div class="header-right">
        <img
            v-if="username"
            :src="userAvatar"
            alt="User Avatar"
            class="user-avatar"
            @click="toggleUserInfo"
            style="cursor: pointer;"
        />

        <el-dialog
            v-model="showUserInfo"
            title="用户信息"
            @close="toggleUserInfo"
            :close-on-click-modal="false"
        >
          <div v-if="username">
            <img :src="userAvatar" alt="User Avatar" class="user-avatar" />
            <span class="user">你好：{{ this.$store.state.username }}</span>
            <button @click="logoutUser" class="btn btn-primary">登出</button>
          </div>
          <div v-else>
            <button @click="loginUser" class="btn btn-primary">登录</button>
          </div>
          <span slot="footer" class="dialog-footer">
        <button @click="toggleUserInfo" class="btn btn-default">关闭</button>
      </span>
        </el-dialog>
      </div>
    </header>

    <!-- Main Content Section -->
    <main class="content">
      <router-view />
    </main>

    <!-- Footer Navigation -->
    <footer class="footer">
      <el-menu
          active-text-color="#ffd04b"
          default-active="crawler"
          text-color="#fff"
          :router="true"
          class="menu-horizontal menu-gradient">
        <el-menu-item index="product">
          <el-icon style="display: block; margin-bottom: 5px;">
            <Postcard />
          </el-icon>
          <span style="display: block;">商品比价</span>
        </el-menu-item>

        <el-menu-item index="card">
          <el-icon style="display: block; margin-bottom: 5px;">
            <Reading />
          </el-icon>
          <span style="display: block;">降价推送</span>
        </el-menu-item>

        <el-menu-item index="borrow">
          <el-icon style="display: block; margin-bottom: 5px;">
            <Clock />
          </el-icon>
          <span style="display: block;">历史记录</span>
        </el-menu-item>
      </el-menu>
    </footer>
  </div>

</template>

<script>
import { RouterView } from 'vue-router';
import { ElContainer, ElHeader, ElFooter, ElMain, ElMenu, ElMenuItem, ElScrollbar, ElIcon } from 'element-plus';
import {Reading, Postcard, Clock,  User} from '@element-plus/icons-vue';
import { mapState } from 'vuex';
import defaultAvatar from '@/assets/img/logo.png';

export default {
  components: {Postcard, Reading, User, Clock},
  data() {
    return {
      newUsername: '',
      showUserInfo: false, // 控制浮窗显示
    };
  },
  computed: {
    ...mapState(['username']),
    userAvatar() {
      return this.$store.state.userAvatar || defaultAvatar;
    }
  },
  methods: {
    loginUser() {
      if (!this.username) {
        this.$router.push('/login');
      } else {
        alert('您已登录');
      }
    },
    logoutUser() {
      this.$store.dispatch('logout');
    },
    toggleUserInfo() {
      this.showUserInfo = !this.showUserInfo; // 切换浮窗状态
    }
  }
};
</script>

<style scoped>
.main-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  min-height: 100vh;
  width: 100%;
  overflow: hidden;
}

.header-left {
  display: flex;
  margin-left: 10px;
  align-items: center; /* 垂直居中 */
}

.header-logo {
  width: 40px;
  height: 40px;
  margin-right: 5px; /* 增加间距 */
}

.header-titles {
  display: flex;
  flex-direction: column; /* 标题垂直排列 */
}

.main-title {
  font-size: 18px;
  margin: 0;
}

.sub-title {
  font-size: 12px;
  margin: 0;
}


.sub-title {
  font-size: 10px;
  margin: 0;
  color:gray;
  font-style: italic;
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  cursor: pointer;
  border: 2px solid white; /* White border with 2px width */
}

.btn {
  padding: 5px 10px;
  border: none;
  border-radius: 5px;
  width: 80px;
  background-color: #ffd04b;
  color: #333;
  cursor: pointer;
}

.content {
  flex: 1;
  overflow-y: auto;
  padding: 10px 10px 60px;
}


.menu-horizontal {
  display: flex;
  flex-wrap: wrap;
  width: 100%;
  justify-content: space-around;
  overflow-x: hidden;
}

.menu-gradient {
  background: linear-gradient(to bottom, #fd613e, #ffd04b); /* 从橙色渐变到金色 */
  height: 100%;
  width: 100%;
  color: #fff;
  border: none; /* 可选：去掉边框以增强视觉效果 */
}

.menu-gradient .el-menu-item {
  transition: background 0.3s; /* 可选：为菜单项添加平滑过渡 */
}


.el-menu-item .el-icon {
  margin-bottom: 5px;
}

.el-menu-item span {
  font-size: 12px;
  text-align: center;
}

.menu-gradient .el-menu-item:hover {
  background: rgba(255, 255, 255, 0.2); /* 可选：鼠标悬停时的高亮效果 */
}
.header-gradient {
  background:
      linear-gradient(to right, rgba(255, 255, 255, 1) 0%, rgba(255, 255, 255, 1) 50%, rgba(255, 255, 255, 0) 100%), /* 蒙版从左到右渐变，透明度逐渐增加 */
      url('../../assets/img/sunset.png'); /* 替换为你的背景图片路径 */
  background-size: auto 100%; /* 高度适应 header，高宽按比例缩放 */
  background-repeat: no-repeat; /* 避免图片重复 */
  background-position: right center; /* 图片居中 */
  height: 60px; /* 设置 header 的高度 */
  color: #000; /* 字体颜色根据需求调整 */
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
  width: 100%;
}

.header-right {
  display: flex;
  align-items: center; /* Vertically center the content within header-right */
  justify-content: flex-end; /* Align the content (avatar) to the right */
}
footer {
  position: absolute;
  bottom: 0;
  width: 100%;
  height: 50px;
}
</style>
