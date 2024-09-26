<template>
  <div class="container">
      <div id="register" class="login-box">
        <h2>注册页面</h2>
        <el-form ref="form" :model="form" label-width="100px">
          <el-form-item label="用户名:">
            <el-input v-model="form.username" placeholder="请输入用户名"></el-input>
          </el-form-item>
          <el-form-item label="密  码:">
            <el-input show-password v-model="form.password" type="password" placeholder="请输入密码">
            </el-input>
          </el-form-item>
          <el-form-item label="确认密码:">
            <el-input show-password v-model="form.password_confirm" type="password" placeholder="请再次输入密码"></el-input>
          </el-form-item>
            <el-form-item label="邮箱" >
              <el-input v-model="form.email"></el-input>
              <el-button type="primary" @click="send_code">获取验证码</el-button>
            </el-form-item>
            <el-form-item label="验证码">
              <el-input v-model="form.code"></el-input>
              <el-button type="primary" @click="verify_email">验证邮箱</el-button>
            </el-form-item>
        </el-form>
        <el-button :disabled="reg_disable" type="primary" round @click="register" class="btn">立即注册</el-button>
      </div>
  </div>
</template>

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
  background: url('../assets/img/zju.jpg');
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
  width: 400px;
}

h2 {
  text-align: center;
  margin-bottom: 20px;
  color: #333;
}

.btn {
  display: block;
  width: 100%;
  margin-top: 20px;
}
</style>


<script>
import axios from 'axios';
import {ElMessage} from "element-plus";
//邮件
export default {
  data () {
    return {
      form: {
        username: '',
        password: '',
        password_confirm: '',
        email:'',
        code:'',
        veri_code:''
      },
      reg_disable: true,
      isnull: false
    };
  },

  methods: {
    send_code(){
      //邮件正则
      const EmailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (EmailRegex.test(this.form.email)) {
        axios.get('/register', {
          params: {
            email:this.form.email
          }
        }).then(res => {
          this.form.veri_code =  res.data
          ElMessage.success("验证码发送成功") // 显示消息提醒
        }).catch(err => {
          ElMessage.error("验证码发送失败")
        })
      } else {
        console.log('邮件格式不正确')
      }
    },
    verify_email(){
        if(this.form.veri_code.toString() === this.form.code.toString()){
          ElMessage.success("邮箱验证成功") // 显示消息提醒
          //使能注册按钮
          this.reg_disable = false;
        }
        else{
          ElMessage.error("邮箱验证失败，验证码错误") // 显示消息提醒
        }
    },
    register() {
      if (this.form.username === '') {
        this.$message.error('用户名不能为空');
      } else if (this.form.username.length < 6) {
        this.$message.error('用户名长度不能少于6字节');
      } else if (this.form.password === '') {
        this.$message.error('密码不能为空');
      } else if (this.form.password !==this.form.password_confirm) {
        this.$message.error('确认密码与密码不一致');
      } else if (this.form.password.length < 6) {
        this.$message.error('密码长度不能少于6字节');
      } else if (this.form.email === '') {
        this.$message.error('邮箱不能为空');
      } else if (!this.validateEmail(this.form.email)) {
        this.$message.error('邮箱格式不正确');
      } else {
        axios.post('/register', {
          params: {
            name: this.form.username,
            password: this.form.password,
            email:this.form.email
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
          ElMessage.error("注册失败,用户或邮箱已存在")
          this.$alert('用户名或邮箱已存在', '注册失败', {
            confirmButtonText: '确定',
            callback: action => {
              this.form.username = '',
                  this.form.password = '',
                  this.form.password_confirm = '',
                  this.form.email = ''
            }
          })
        })
      }
    },
    validateEmail(email) {
    const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return re.test(email);
    }
  }
}
</script>

