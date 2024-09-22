<template>
  <div id="main">
    <el-container>
      <el-header>
        <div class="logo" >
          <img src="../assets/img/logo.png">  <!-- 此处请提前准备好图片 -->
        </div>
        <div class="user">
          你好：{{username}}
        </div>
      </el-header>
      <el-main></el-main>
      <el-footer>Footer</el-footer>
    </el-container>
  </div>

</template>


<script>
export default {
  data () {
    return {
      username: ''
    };
  },
  created() {
    this.username = this.$route.query.name;
  },
}
</script>

<style scoped>
#main {
  width: 100%;
}
.el-header, .el-footer {
  background-color: #5e6f4d;
  color: #333;
  text-align: center;
  line-height: 60px;
}
.el-footer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
}
.logo {
  float: left;
  margin: 0 auto;
}
.logo img{
  padding-top: 5px;
  height: 50px;
}
.user {
  float: right;
  color: #f7fffc;
  font-size: 22px;
  margin-right: 50px;
}
</style>