<template>
  <div class="container">
    <div id="register" class="login-box">
      <h2>用户登录</h2>
      <el-form ref="form" :model="form" label-width="20%">
        <el-form-item label="用户名:">
          <el-input v-model="form.username"></el-input>
        </el-form-item>
        <el-form-item label="密  码:">
          <el-input show-password v-model="form.password" type="password"></el-input>
        </el-form-item>
      </el-form>
      <el-button type="primary" round @click="login" class="btn">登录</el-button>
      <span @click="register" class="reg">前往注册</span>
    </div>
  </div>
</template>

<script>
import axios from 'axios'
import {ElMessage} from "element-plus";
export default {
  data () {
    return {
      form: {
        username: '',
        password: ''
      }
    };
  },

  methods: {
    login() {
      if(this.form.username === '') {
        this.$message.error('用户名不能为空');
      }else if(this.form.password === '') {
        this.$message.error('密码不能为空');
      }else{
        axios.post('/login', {
          params: {
            name: this.form.username,
            password: this.form.password
          }
        }).then(res=>{
          ElMessage.success("登录成功") // 显示消息提醒
          this.$store.dispatch('login', this.form.username);
          this.$router.push({
            path: '/home/product',
          })
        }).catch(err=>{
          console.log("登录失败，用户名或密码错误" + err);
          this.$alert('用户名或密码错误', '登录失败', {
            confirmButtonText: '确定',
            callback: action => {
              this.form.username = '',
                  this.form.password = ''
            }
          })
        })
      }
    },
    register() {
      this.$router.push('/register')
    }
  }
}
</script>

<style>
.container {
  display: flex;
  justify-content: center;
  align-items: center;
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  right: 0;
  background: url('../assets/img/background.png');
  background-size: 100% 100%;
}

.overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5); /* 半透明的黑色遮罩 */
}

.login-box {
  position: relative;
  z-index: 2;
  padding: 40px;
  background: rgba(255, 255, 255, 0.8); /* 半透明背景 */
  backdrop-filter: blur(10px); /* 模糊效果 */
  border-radius: 15px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.3); /* 添加阴影 */
  width: 500px;
  height: 300px;
}

h2 {
  text-align: center;
  margin-bottom: 20px;
  color: #333;
}

.btn {
  background: linear-gradient(to bottom, #fd8c4c, #ffb84d); /* 温和的橙色到金色渐变 */
  border: none; /* 去掉边框 */
  color: #fff;  /* 设置文本颜色为白色 */
  font-size: 16px; /* 设置字体大小 */
  padding: 12px 20px; /* 设置内边距 */
  border-radius: 30px; /* 圆角效果 */
  width: 100%; /* 按钮宽度填满容器 */
  text-align: center; /* 文字居中 */
  cursor: pointer; /* 设置鼠标为指针形态 */
  transition: all 0.3s ease; /* 平滑过渡效果 */
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1); /* 添加阴影，增加按钮立体感 */
}


h2 {
  text-align: center;
  margin-bottom: 20px;
  color: #333;
  font-size: 24px;
  font-weight: bold;
}

.reg {
  display: inline-block;
  color: #fd8c4c; /* 文字颜色 */
  font-size: 16px;
  text-decoration: none; /* 去掉下划线 */
  font-weight: bold;
  margin-top: 10px;
  cursor: pointer;
  position: absolute;
  bottom: 10px;
  right: 10px;
  transition: all 0.3s ease; /* 平滑过渡效果 */
}

.reg:hover {
  color: #ffb84d; /* 鼠标悬停时颜色变化 */
  text-decoration: underline; /* 添加下划线 */
  transform: translateY(-2px); /* 鼠标悬停时略微向上浮动 */
}

.reg:active {
  color: #fd8c4c; /* 鼠标点击时的颜色 */
  transform: translateY(1px); /* 点击时的轻微下移 */
}
</style>