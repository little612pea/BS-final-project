<template>
  <div>
    <div class="PersonTop">
      <el-avatar :src="userAvatar" style="margin-right: 10px;width: 150px;height: 150px"></el-avatar>
      <div class="PersonTop_text">
        <div class="user_text">
          <div class="user_name">
            <span> {{this.$store.state.username}}</span>
          </div>
          <div class="user_qianming">
            <span> {{ design }}</span>
          </div>
        </div>
        <div class="user_num">
          <div style="cursor: pointer" @click="modify_info">
            <div class="num_number">{{ fanCounts }}</div>
            <span class="num_text">信息修改</span>
          </div>
          <div style="cursor: pointer" @click="goto_compare">
            <div class="num_number">{{ followCounts }}</div>
            <span class="num_text">推送设置</span>
          </div>
          <div>
            <div class="num_number">{{ goodCounts }}</div>
            <span class="num_text">扫一扫</span>
          </div>
        </div>
      </div>
    </div>
    <div class="person_body">
      <el-card class="info-card" :body-style="{ padding: '30px', backgroundColor: '#f9f9f9' }"
               style="width: 100%; height: 100%; border-radius: 10px; box-shadow: 0 2px 10px rgba(0, 0, 0, 0);">
        <div class="card-header" style="font-size: 20px; font-weight: bold; text-align: center; color: #333; margin-bottom: 20px;">
          个人信息
        </div>
        <div class="card-body" style="width: 100%; height: auto;">
          <el-row :gutter="30">
            <el-col :span="12" style="margin-bottom: 10px;">
              <div class="info-item" style="margin-bottom: 10px;">
                <strong style="color: #555;">用户ID:</strong>
                <span style="color: #777;">1</span>
              </div>
              <div class="info-item" style="margin-bottom: 10px;">
                <strong style="color: #555;">用户名:</strong>
                <span style="color: #777;">{{this.$store.state.username}}</span>
              </div>
              <div class="info-item" style="margin-bottom: 10px;">
                <strong style="color: #555;">邮箱:</strong>
                <span style="color: #777;">         3220104116@zju.edu.cn</span>
              </div>
              <div class="info-item" style="margin-bottom: 10px;">
                <strong style="color: #555;">手机:</strong>
                <span style="color: #777;">         1935****138</span>
              </div>
            </el-col>
          </el-row>
        </div>
      </el-card>
    </div>

    <el-dialog v-model="dialogVisible"
        title="修改个人信息"
        width="60%"
        :before-close="handleClose">
      <el-form :model="form" :rules="rules" ref="form" label-width="150px">
        <el-form-item label="头像" prop="avatar">
          <el-avatar :src="userAvatar" style="margin-right: 10px;width: 100px;height: 100px"></el-avatar>

          <!-- 上传头像功能 -->
          <el-upload
              class="avatar-uploader"
              action="/upload-avatar"
          :show-file-list="false"
          :on-success="handleAvatarSuccess"
          :before-upload="beforeAvatarUpload"
          >
          <el-button type="primary">修改头像</el-button>
          </el-upload>
        </el-form-item>
        <!-- 输入原始密码 -->
        <el-form-item label="原始密码" prop="oldPassword">
          <el-input v-model="form.oldPassword" type="password" placeholder="请输入原始密码"></el-input>
        </el-form-item>

        <!-- 输入新密码 -->
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="form.newPassword" type="password" placeholder="请输入新密码"></el-input>
        </el-form-item>

        <!-- 确认新密码 -->
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入新密码"></el-input>
        </el-form-item>

        <!-- 提交按钮 -->
        <el-form-item>
          <el-button type="primary" @click="submitForm('form')">提交</el-button>
          <el-button @click="resetForm('form')">重置</el-button>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
  </span>
    </el-dialog>
  </div>

</template>

<script>
import defaultAvatar from '@/assets/img/logo.png';
import {mapState} from "vuex";
export default {
  data() {
    return {
      dialogVisible: false,
      userid:"",
      form: {
        avatar: 'https://example.com/avatar.jpg', // 头像的占位符
        oldPassword: '',      // 原始密码
        newPassword: '',      // 新密码
        confirmPassword: ''   // 确认新密码
      },
      // 表单验证规则
      rules: {
        oldPassword: [
          { required: true, message: '请输入原始密码', trigger: 'blur' }
        ],
        newPassword: [
          { required: true, message: '请输入新密码', trigger: 'blur' },
          { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, message: '请确认新密码', trigger: 'blur' },
          { validator: (rule, value, callback) => {
              if (value !== this.form.newPassword) {
                callback(new Error('两次输入的密码不一致'));
              } else {
                callback();
              }
            }, trigger: 'blur' }
        ]
      }
    };
  },
  computed: {
    userAvatar() {
      // 获取用户头像，如果没有则显示默认头像
      return this.$store.state.userAvatar || defaultAvatar;
    }
  },
  methods: {
    goto_compare(){
      this.$router.push('/home/compare');
    },
    // 提交表单
    submitForm(formName) {
      this.$refs[formName].validate((valid) => {
        if (valid) {
          // 执行提交逻辑，例如发送 API 请求
          console.log('提交成功', this.form);
        } else {
          console.log('提交失败');
          return false;
        }
      });
    },
    // 重置表单
    resetForm(formName) {
      this.$refs[formName].resetFields();
    },
    handleClose() {
      this.dialogVisible = false;
      this.$emit("flesh");
    },
    modify_info(){
      this.dialogVisible=true;
    },
  },

};
</script>

<style scoped>
.PersonTop {
  width: 1000px;
  height: 140px;
  padding-top: 20px;
  background-color: white;
  margin-top: 30px;
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  border-radius: 5px;
}

.PersonTop_img {
  width: 150px;
  height: 120px;
  background-color: #8c939d;
  margin-right: 24px;
  margin-left: 20px;
  overflow: hidden;
  border-radius: 20px;
}

.PersonTop_img img {
  width: 100%;
  height: 100%;
  border-radius: 20px;
}

.PersonTop_text {
  height: 120px;
  width: 880px;
  display: flex;
}

.user_text {
  width: 60%;
  height: 100%;
  line-height: 30px;
}

.user_name {
  font-weight: bold;
}
.user-v {
  margin-bottom: -5px;
}
.user-v-img {
  width: 15px;
  height: 15px;
}
.user-v-font {
  font-size: 15px;
  color: #00c3ff;
}
.user_qianming {
  font-size: 14px;
  color: #999;
}

.user_num {
  width: 40%;
  height: 100%;
  display: flex;
  align-items: center;
}

.user_num > div {
  text-align: center;
  border-right: 1px dotted #999;
  box-sizing: border-box;
  width: 80px;
  height: 40px;
  line-height: 20px;
}

.num_text {
  color: #999;
}

.num_number {
  font-size: 20px;
  color: #333;
}
.el-menu-item>span {
  font-size: 16px;
  color: #999;
}

/*下面部分样式*/
.person_body {
  width: 1000px;
  margin-top: 210px;
  display: flex;
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  border-radius: 10px;
}

.person_body_left {
  width: 27%;
  height: 600px;
  border-radius: 5px;
  margin-right: 3%;
  text-align: center;
}

.person_body_list {
  width: 100%;
  height: 50px;
  margin-top: 25px;
  font-size: 22px;
  border-bottom: 1px solid #f0f0f0;
  //background-image: -webkit-linear-gradient(
  //    left,
  //    rgb(42, 134, 141),
  //    #e9e625dc 20%,
  //    #3498db 40%,
  //    #e74c3c 60%,
  //    #09ff009a 80%,
  //    rgba(82, 196, 204, 0.281) 100%
  //);
  -webkit-text-fill-color: transparent;
  -webkit-background-clip: text;
  -webkit-background-size: 200% 100%;
  -webkit-animation: masked-animation 4s linear infinite;
}

.el-menu-item {
  margin-top: 22px;
}

.person_body_right {
  width: 70%;
  /* height: 500px; */
  border-radius: 5px;
  background-color: white;
}

.box-card {
  height: 500px;
}

/*ui样式*/
.el-button {
  width: 84px;
}
.updateinfo {
  height: 350px;
  overflow: auto;
}
.left {
  /* width: 330px; */
  float: left;
}
.right {
  overflow: hidden;
}
.info-item {
  margin-bottom: 10px;
  font-size: 16px;
  line-height: crawler.5;
}
.card-header {
  background-color: #f5f5f5;
  padding: 10px;
  border-bottom: 1px solid #e4e7ed;
}
</style>

