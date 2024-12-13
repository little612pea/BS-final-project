<template>
  <div class="container">
      <div id="register" class="register-box">
        <h2>新用户注册</h2>
        <el-form ref="form" :model="form" label-width="100px">
          <el-form-item label="用户名:">
            <el-input v-model="form.username" placeholder="请输入用户名"></el-input>
          </el-form-item>
          <el-form-item label="密码:">
            <el-input show-password v-model="form.password" type="password" placeholder="请输入密码"></el-input>
          </el-form-item>
          <el-form-item label="确认密码:">
            <el-input show-password v-model="form.password_confirm" type="password" placeholder="请再次输入密码"></el-input>
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="form.email"></el-input>
            <el-button class="button-gradient" @click="send_code">获取验证码</el-button>
          </el-form-item>
          <el-form-item label="验证码">
            <el-input v-model="form.code"></el-input>
            <el-button class="button-gradient" @click="verify_email">验证邮箱</el-button>
          </el-form-item>
        </el-form>
        <el-button :disabled="reg_disable" type="primary" round @click="register" class="btn">立即注册</el-button>
      </div>
    </div>
</template>

<style scoped>
.container {
  display: flex;
  justify-content: center;
  align-items: center;
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  right: 0;
  background: url('../../assets/img/background.png');
  background-size: 100% 100%;
}

.register-box {
  background: rgba(255, 255, 255, 0.95); /* 更加不透明的背景，突出表单 */
  padding: 40px;
  border-radius: 10px;
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.2); /* 表单阴影 */
  width: 100%;
  max-width: 400px; /* 限制表单最大宽度 */
}

h2 {
  text-align: center;
  margin-bottom: 20px;
  color: #333;
  font-size: 24px;
  font-weight: bold;
}

.el-form-item {
  margin-bottom: 20px;
}

.el-input {
  width: 100%;
  border-radius: 10px;
  padding: 10px;
  font-size: 16px;
}

.el-button {
  width: 100%;
  padding: 12px;
  font-size: 16px;
  margin-top: 10px;
}

.button-gradient {
  background: linear-gradient(to bottom, #fd8c4c, #ffb84d); /* 温和的橙色到金色渐变 */
  border: none;
  color: #fff;
  font-size: 16px;
  padding: 12px 20px;
  border-radius: 30px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.button-gradient:hover {
  background: linear-gradient(to bottom, #ffb84d, #fd8c4c);
  transform: translateY(-2px);
  box-shadow: 0 6px 12px rgba(0, 0, 0, 0.15);
}

.button-gradient:active {
  background: linear-gradient(to bottom, #fd8c4c, #ffb84d);
  transform: translateY(2px);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
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

.btn:disabled {
  background: #d3d3d3; /* 禁用时的背景色 */
  cursor: not-allowed; /* 禁用时显示禁用光标 */
  box-shadow: none; /* 禁用时移除阴影 */
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
          const errorMessage = err.response?.data?.message || err.message || "未知错误";
          ElMessage.error(`验证码发送失败: ${errorMessage}`);
          console.error("验证码发送失败:", err); // 打印详细错误信息到控制台
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

