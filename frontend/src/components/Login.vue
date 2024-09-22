<template>
  <div class="bg">
    <div id="login">
      <h2>登录页面</h2>
      <el-form ref="form" :model="form" label-width="20%">
        <el-form-item label="用户名:">
          <el-input v-model="form.username"></el-input>
        </el-form-item>
        <el-form-item label="密  码:">
          <el-input v-model="form.password" type="password"></el-input>
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
          this.$store.dispatch('setUserName', this.form.username);
          this.$router.push({
            path: '/home/book',
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

<style scoped>
.bg {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  right: 0;
  background: url('../assets/img/login_bg.jpg');
  background-size: 100% 100%;
}
#login {
  height: 250px;
  width: 350px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.25);
  background: #ffffff;
  opacity: 0.85;
  position: absolute;
  top: 20%;
  left: 40%;
  text-align: center;
  display: flex;
  flex-direction: column;/*横向*/
  padding: 30px;
}
#login h2 {
  padding-bottom: 30px;
}
.btn {
  width: 60%;
  margin: auto;
}
.reg {
  color: red;
  position: absolute;
  bottom: 10px;
  right: 10px;
  cursor:pointer;
}
</style>