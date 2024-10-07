<template>
  <div>
    <div class="PersonTop">
      <div class="PersonTop_img">
        <img src="@/assets/img/logo.png"  />
      </div>
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
          <div style="cursor: pointer" @click="myfan">
            <div class="num_number">{{ fanCounts }}</div>
            <span class="num_text">信息修改</span>
          </div>
          <div style="cursor: pointer" @click="myfollow">
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
      <el-card class="info-card" :body-style="{ padding: '20px' }" style="width: 100%;height:100%">
        <div class="card-header" style="font-size: 18px; font-weight: bold; text-align: center;">个人信息</div>
        <div class="card-body"  style="width: 100%; height: 400px">
          <el-row gutter={20}>
            <el-col :span="12">
              <div class="info-item"><strong>昵称:</strong> {{ form.nickname }}</div>
              <div class="info-item"><strong>年龄:</strong> {{ form.age }}</div>
              <div class="info-item"><strong>性别:</strong> {{ form.sex === 1 ? '男' : '女' }}</div>
              <div class="info-item"><strong>邮箱:</strong> {{ form.email }}</div>
            </el-col>
            <el-col :span="12">
              <div class="info-item"><strong>用户编号:</strong> {{ form.id }}</div>
              <div class="info-item"><strong>账号:</strong> {{ form.account }}</div>
              <div class="info-item"><strong>地区:</strong> {{ form.area }}</div>
              <div class="info-item"><strong>兴趣爱好:</strong> {{ form.hobby }}</div>
            </el-col>
          </el-row>
        </div>
      </el-card>
    </div>
    <personal-dia ref="dia" @flesh="reload" />
    <el-dialog v-model="dialogVisible"
        title="修改个人信息"
        width="60%"
        :before-close="handleClose">
      <el-form :model="form" :rules="rules" ref="form" label-width="150px">
        <div class="updateinfo">
          <div class="left">
            <el-form-item label="头像" prop="avatar">
              <img style="width:150px;height:110px" :src="form.avatar"></img>
            </el-form-item>
            <el-form-item label="账号密码" prop="password">
              <el-input v-model="form.password"></el-input>
            </el-form-item>
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="form.nickname"></el-input>
            </el-form-item>
            <el-form-item label="年龄" prop="age">
              <el-input v-model="form.age"></el-input>
            </el-form-item>
            <el-form-item label="性别" prop="sex">
              <el-switch
                  v-model="form.sex"
                  active-color="#13ce66"
                  inactive-color="#ff4949"
                  active-text="男"
                  inactive-text="女"
                  :active-value= "1"
                  :inactive-value= "0"
              >
              </el-switch>
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email"></el-input>
            </el-form-item>

          </div>
          <div class="right">
            <el-form-item label="用户编号" prop="id">
              <el-input v-model="form.id" disabled></el-input>
            </el-form-item>
            <el-form-item label="账号" prop="account">
              <el-input v-model="form.account" disabled></el-input>
            </el-form-item>
            <el-form-item label="地区" prop="area">
              <el-input v-model="form.area"></el-input>
            </el-form-item>
            <el-form-item label="兴趣爱好" prop="hobby">
              <el-input v-model="form.hobby"></el-input>
            </el-form-item>
            <el-form-item label="职业" prop="work">
              <el-input v-model="form.work"></el-input>
            </el-form-item>
            <el-form-item label="个性签名" prop="design">
              <el-input v-model="form.design"></el-input>
            </el-form-item>
            <el-form-item label="手机号码" prop="mobilePhoneNumber">
              <el-input v-model="form.mobilePhoneNumber"></el-input>
            </el-form-item>
          </div>
        </div>
      </el-form>
      <span slot="footer" class="dialog-footer">
    <el-button @click="handleClose">取 消</el-button>
    <el-button type="primary" @click="submit">提 交</el-button>
  </span>
    </el-dialog>
  </div>

</template>

<script>

import {mapState} from "vuex";

export default {
  name: "Personal",
  inject: ["reload"],
  data() {
    return {
      avatar: "",
      nickname: "",
      newUsername: '',
      v: 1,
      design: "",
      followCounts: "",
      fanCounts: "",
      goodCounts: "",
      ModifyPersonalInfo:false,
      followData: {
        fanId: "",
        followId: "",
      },
      dialogVisible: false,
      form: {
        avatar: "",
        password: "",
        nickname: "",
        age: Number,
        email: "",
        mobilePhoneNumber: "",
        sex: Number,
        id: Number,
        account: "",
        area: "",
        hobby: "",
        work: "",
        design: "",
      },
      rules: {
        nickname: [
          { required: true, message: "昵称不能为空", trigger: "blur" },
        ],
        password: [
          { required: true, message: "账号密码不能为空", trigger: "blur" },
        ],
      },
    };
  },
  computed: {
    // 获取 Vuex 中的 username 状态
    ...mapState(['username'])
  },
  methods: {
    open() {
      this.dialogVisible = true;
    },
    submit() {
      updateUser(this.form)
          .then((res) => {
            console.log(res);
            this.dialogVisible = false;
            this.$emit("flesh");
          })
          .catch((err) => {
            console.log(err);
          });
    },
    handleClose() {
      this.dialogVisible = false;
      this.$emit("flesh");
    },
    myfan(){
      this.dialogVisible=true;
    },
    loginUser() {
      if (!this.username) { // 检查是否已登录（Vuex中的username是否为空）
        // 如果未登录，跳转到 /login 页面
        this.$router.push('/login');
      } else {
        alert('您已登录');
      }
    },
    logoutUser() {
      // 通过 this.$store.dispatch 调用 logout action
      this.$store.dispatch('logout');
    }
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
  line-height: 1.5;
}
.card-header {
  background-color: #f5f5f5;
  padding: 10px;
  border-bottom: 1px solid #e4e7ed;
}
</style>

