<template>
  <div class="bg">
    <div id="register">
      <h2>注册页面</h2>
      <el-form ref="form" :model="form" label-width="20%">
        <el-form-item label="用户名:">
          <el-input v-model="form.username"></el-input>
        </el-form-item>
        <el-form-item label="密  码:">
          <el-input v-model="form.password" type="password"></el-input>
        </el-form-item>
      </el-form>
      <el-button type="primary" round @click="register" class="btn">注册</el-button>
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
      },
      isnull: false
    };
  },

  methods: {
    register() {
      if(this.form.username === '') {
        this.$message.error('用户名不能为空');
      }else if(this.form.password === '') {
        this.$message.error('密码不能为空');
      }else{
        axios.post('/register', {
          params: {
            name: this.form.username,
            password: this.form.password
          }
        }).then(res => {
          ElMessage.success("注册成功") // 显示消息提醒
          this.$alert('是否返回登录页面', '注册成功', {
            confirmButtonText: '确定',
            callback: action => {
              this.$router.push('/login')
            }
          })
        }).catch(err => {
          ElMessage.error("注册失败,用户已存在")
          this.$alert('用户名已存在', '注册失败', {
            confirmButtonText: '确定',
            callback: action => {
              this.form.username = '',
                  this.form.password = ''
            }
          })
        })
      }
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
  background: url('../assets/img/register_bg.jpg');
  background-size: 100% 100%;
}
#register {
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
#register h2 {
  padding-bottom: 30px;
}
.btn {
  width: 60%;
  margin: auto;
}
</style>