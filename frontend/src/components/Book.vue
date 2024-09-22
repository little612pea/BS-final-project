<!-- TODO: YOUR CODE HERE -->
<template>
  <el-scrollbar height="100%" style="width: 100%;">
    <!-- 标题和搜索框 -->
    <div style="margin-top: 20px; margin-left: 40px; font-size: 2em; font-weight: bold; ">商品比价
      <el-input v-model="toSearch" :prefix-icon="Search"
                style=" width: 15vw;min-width: 150px; margin-left: 30px; margin-right: 30px; float: right;" clearable />
      <!--在button文字的前面加上icon-->
      <el-button @click="multi_cond_BookInfo.title = '',multi_cond_BookInfo.author = '',multi_cond_BookInfo.category = '',  multi_cond_BookInfo.press = '', multi_cond_BookInfo.publishYear = '',  multi_cond_BookInfo.price = '', multi_cond_BookInfo.stock = '',
      multiCondBookVisible = true" style="float: right;" type="primary":icon="Search">
        多条件查询
      </el-button>
      <el-button @click="uploadFileInfo = '', uploadFileInfoVisible = true" style="float: right;margin-right: 10px" type="primary":icon="UploadFilled">
        图书批量入库
      </el-button>
    </div>



    <!-- 图书卡片显示区 -->
    <div style="display: flex;flex-wrap: wrap; justify-content: start;">

      <!-- 图书卡片 -->
      <div class="bookBox" v-for="book in books" v-show="book.title.includes(toSearch)" :key="book.id">
        <div>
          <!-- 卡片标题 -->
          <div style="font-size: 25px; font-weight: bold;">No. {{ book.id }}</div>

          <el-divider />

          <!-- 卡片内容 -->
          <div style="margin-left: 10px; text-align: start; font-size: 16px;">
            <p style="padding: 2.5px;"><span style="font-weight: bold;">书名：</span>{{ book.title }}</p>
            <p style="padding: 2.5px;"><span style="font-weight: bold;">作者：</span>{{ book.author }}</p>
            <p style="padding: 2.5px;"><span style="font-weight: bold;">类别：</span>{{ book.category }}</p>
            <p style="padding: 2.5px;"><span style="font-weight: bold;">出版社：</span>{{ book.press }}</p>
            <p style="padding: 2.5px;"><span style="font-weight: bold;">出版时间：</span>{{ book.publishYear }}</p>
            <p style="padding: 2.5px;"><span style="font-weight: bold;">价格：</span>{{ book.price }}</p>
            <p style="padding: 2.5px;"><span style="font-weight: bold;">库存：</span>{{ book.stock }}</p>

          </div>

          <el-divider />

          <!-- 卡片操作 -->
          <div style="margin-top: 10px;">
            <el-button type="primary" :icon="Edit" @click="this.toModifyInfo.id = book.id, this.toModifyInfo.title = book.title,
            this.toModifyInfo.author = book.author, this.toModifyInfo.category = book.category, this.toModifyInfo.press = book.press,
            this.toModifyInfo.publishYear = book.publishYear, this.toModifyInfo.price = book.price, this.toModifyInfo.stock = book.stock,
            this.modifyBookVisible = true" circle />
            <el-button type="danger" :icon="Delete" circle
                       @click="this.toRemove = book.id, this.removeBookVisible = true"
                       style="margin-left: 30px; " />
            <el-button type="success":icon="UploadFilled" circle
                       @click="this.borrowBookInfo.id = book.id, this.borrowBookVisible = true"
                       style="margin-left: 30px;" />
          </div>

        </div>
      </div>

      <!-- 新建图书卡片 -->
      <el-button class="newBookBox"
                 @click="newBookInfo.title = '',newBookInfo.author = '',newBookInfo.category = '',  newBookInfo.press = '', newBookInfo.publishYear = '',  newBookInfo.price = '', newBookInfo.stock = '', newBookVisible = true">
        <el-icon style="height: 50px; width: 50px;">
          <Plus style="height: 100%; width: 100%;" />
        </el-icon>
      </el-button>

    </div>
    <!-- 新建图书对话框 -->
    <el-dialog v-model="newBookVisible" title="新建图书" width="30%" align-center>
      <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
        书名：
        <el-input v-model="newBookInfo.title" style="width: 12.5vw;" clearable />
      </div>
      <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
        作者：
        <el-input v-model="newBookInfo.author" style="width: 12.5vw;" clearable />
      </div>
      <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
        类别：
        <el-input v-model="newBookInfo.category" style="width: 12.5vw;" clearable />
      </div>
      <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
        出版社：
        <el-input v-model="newBookInfo.press" style="width: 12.5vw;" clearable />
      </div>
      <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
        出版时间：
        <el-input v-model="newBookInfo.publishYear" style="width: 12.5vw;" clearable />
      </div>
      <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
        价格：
        <el-input v-model="newBookInfo.price" style="width: 12.5vw;" clearable />
      </div>
      <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
        库存：
        <el-input v-model="newBookInfo.stock" style="width: 12.5vw;" clearable />
      </div>

      <template #footer>
                <span>
                    <el-button @click="newBookVisible = false">取消</el-button>
                    <el-button type="primary" @click="ConfirmNewBook"
                               :disabled="newBookInfo.title.length === 0 || newBookInfo.category.length === 0 || newBookInfo.author.length === 0 || newBookInfo.press.length === 0 || newBookInfo.publishYear.length === 0 || newBookInfo.price.length === 0 || newBookInfo.stock.length === 0">确定</el-button>
                </span>
      </template>
    </el-dialog>

    <!-- 按照各种条件搜索图书 -->
    <el-dialog v-model="multiCondBookVisible" title="多条件图书查询" width="30%" align-center>
      <div style="margin-left: 1vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
        书名(模糊查询)：
        <el-input v-model="multi_cond_BookInfo.title" style="width: 12.5vw;" clearable />
      </div>
      <div style="margin-left: 1vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
        作者(模糊查询)：
        <el-input v-model="multi_cond_BookInfo.author" style="width: 12.5vw;" clearable />
      </div>
      <div style="margin-left: 1vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
        类别(精确点查)：
        <el-input v-model="multi_cond_BookInfo.category" style="width: 12.5vw;" clearable />
      </div>
      <div style="margin-left: 1vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
        出版社(模糊查询)：
        <el-input v-model="multi_cond_BookInfo.press" style="width: 12.5vw;" clearable />
      </div>
      <!-- 出版时间:下限-上限 -->
      <div style="margin-left: 1vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
        出版时间(年份下限-上限)：
        <el-input v-model="multi_cond_BookInfo.minPublishYear" style="width: 5vw;" clearable />
        <span> - </span>
        <el-input v-model="multi_cond_BookInfo.maxPublishYear" style="width: 5vw;" clearable />
      </div>
      <!-- 价格:下限-上限 -->
      <div style="margin-left: 1vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
        价格(下限-上限)：
        <el-input v-model="multi_cond_BookInfo.minPrice" style="width: 5vw;" clearable />
        <span> - </span>
        <el-input v-model="multi_cond_BookInfo.maxPrice" style="width: 5vw;" clearable />
      </div>
      <template #footer>
                <span>
                    <el-button @click="multiCondBookVisible = false">取消</el-button>
                    <el-button type="primary" @click="Multi_condition_search">确定</el-button>
                </span>
      </template>
    </el-dialog>

    <el-dialog  v-model="uploadFileInfoVisible" title="批量导入图书" width="30%" align-center>
      <div>
        <!-- 文件选择器 -->
        <input type="file" ref="fileInput" @change="uploadFile">
      </div>
    </el-dialog>

    <!-- 修改信息对话框 -->
    <el-dialog v-model="modifyBookVisible" :title="'修改信息(图书ID: ' + this.toModifyInfo.id + ')'" width="30%"
               align-center>
      <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
        书名：
        <el-input v-model="toModifyInfo.title" style="width: 12.5vw;" clearable />
      </div>
      <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
        作者：
        <el-input v-model="toModifyInfo.author" style="width: 12.5vw;" clearable />
      </div>
      <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
        类别：
        <el-input v-model="toModifyInfo.category" style="width: 12.5vw;" clearable />
      </div>
      <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
        出版社：
        <el-input v-model="toModifyInfo.press" style="width: 12.5vw;" clearable />
      </div>
      <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
        出版时间：
        <el-input v-model="toModifyInfo.publishYear" style="width: 12.5vw;" clearable />
      </div>
      <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
        价格：
        <el-input v-model="toModifyInfo.price" style="width: 12.5vw;" clearable />
      </div>
      <div style="margin-left: 2vw; font-weight: bold; font-size: 0.9rem; margin-top: 20px; ">
        库存：
        <span style="width: 12.5vw; display: inline-block;">{{ toModifyInfo.stock }}</span>
      </div>
      <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
        库存增量：
        <el-input v-model="toModifyInfo.stock_inc" style="width: 12.5vw;" clearable />
      </div>


      <template #footer>
                <span class="dialog-footer">
                    <el-button @click="modifyBookVisible = false">取消</el-button>
                    <el-button type="primary" @click="ConfirmModifyBook"
                               :disabled="toModifyInfo.title.length === 0 || toModifyInfo.category.length === 0 || toModifyInfo.author.length === 0 || toModifyInfo.press.length === 0 || toModifyInfo.publishYear.length === 0 || toModifyInfo.price.length === 0 || toModifyInfo.stock.length === 0">确定</el-button>
                </span>
      </template>




    </el-dialog>

    <el-dialog v-model="borrowBookVisible" title="借阅图书" width="30%">
      <span>确定借阅<span style="font-weight: bold;">{{ borrowBookInfo.id }}号图书</span>吗？</span>
      <template #footer>
        <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px;">
          请输入有效的借书卡号：
          <el-input v-model="borrowBookInfo.card_id" style="width: 12.5vw; margin-top: 5px;" clearable></el-input>
        </div>
        <!-- 添加垂直间距 -->
        <div style="margin-top: 20px;">
      <span class="dialog-footer">
        <el-button @click="borrowBookVisible = false">取消</el-button>
        <el-button type="primary" @click="ConfirmBorrowBook">确定</el-button>
      </span>
        </div>
      </template>
    </el-dialog>

    <!-- 删除图书对话框 -->
    <el-dialog v-model="removeBookVisible" title="删除图书" width="30%">
      <span>确定删除<span style="font-weight: bold;">{{ toRemove }}号图书</span>吗？</span>

      <template #footer>
                <span class="dialog-footer">
                    <el-button @click="removeBookVisible = false">取消</el-button>
                    <el-button type="danger" @click="ConfirmRemoveBook">
                        删除
                    </el-button>
                </span>
      </template>
    </el-dialog>

  </el-scrollbar>
</template>

<script>
import {Delete, Edit, Search, UploadFilled} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

export default {
  computed: {
    UploadFilled() {
      return UploadFilled
    }

  },
  data() {
    return {
      books: [], // 图书列表
      Delete,
      Edit,
      Search,
      toSearch: '', // 搜索内容
      newBookVisible: false, // 新建图书对话框可见性
      removeBookVisible: false, // 删除图书对话框可见性
      borrowBookVisible:false,
      multiCondBookVisible:false,
      uploadFileInfoVisible:false,
      toRemove: 0, // 待删除图书号
      borrowBookInfo:{
        id:0,
        card_id:''
      },
      uploadFileInfo:'',
      newBookInfo: { // 待新建图书信息
        category:'',
        title:'',
        press:'',
        publishYear:'',
        author:'',
        price:0,
        stock:0
      },
      modifyBookVisible: false, // 修改信息对话框可见性
      toModifyInfo: { // 待修改图书信息
        id: 0,
        category:'',
        title:'',
        press:'',
        publishYear:'',
        author:'',
        price:0,
        stock:0,
        stock_inc:0
      },
      multi_cond_BookInfo:{
        category:'',
        title:'',
        press:'',
        minPublishYear:'',
        maxPublishYear:'',
        author:'',
        minPrice:'',
        maxPrice:''
      }
    }
  },
  methods: {
    async uploadFile(event) {
      const file = event.target.files[0]
      let formData = new FormData()
      formData.append('files', file)
      const service = axios.create({})
      const res = await service({
        url: '/home/book',
        method: 'POST',
        data: formData
      })
      //console.log(res.data);
        .then(response => {
          ElMessage.success("图书批量入库成功") // 显示消息提醒
          this.uploadFileInfoVisible = false // 将对话框设置为不可见
          this.QueryBooks() // 重新查询图书以刷新页面
        })
        .catch(error=>{
          ElMessage.error("图书批量入库失败,请检查导入文件格式是否正确")
          this.uploadFileInfoVisible = false
          this.QueryBorrows()
        })
    },
    ConfirmNewBook() {
      // 发出POST请求
      axios.post("/home/book",
          { // 请求体
            category: this.newBookInfo.category, // 请求体
            title: this.newBookInfo.title, // 请求体
            press: this.newBookInfo.press,
            publishYear: this.newBookInfo.publishYear,
            author: this.newBookInfo.author,
            price: this.newBookInfo.price,
            stock: this.newBookInfo.stock
          })
          .then(response => {
            ElMessage.success("图书新建成功") // 显示消息提醒
            this.newBookVisible = false // 将对话框设置为不可见
            this.QueryBooks() // 重新查询图书以刷新页面
          })
    },
    ConfirmModifyBook() {
      // TODO: YOUR CODE HERE
      axios.post("/home/book/",
          { // 请求体
            id: this.toModifyInfo.id, // 请求体
            category: this.toModifyInfo.category, // 请求体
            title: this.toModifyInfo.title, // 请求体
            press: this.toModifyInfo.press,
            publishYear: this.toModifyInfo.publishYear,
            author: this.toModifyInfo.author,
            price: this.toModifyInfo.price,
            stock: this.toModifyInfo.stock,
            inc: this.toModifyInfo.stock_inc
          })
          .then(response => {
            ElMessage.success("图书信息修改成功") // 显示消息提醒
            this.modifyBookVisible = false // 将对话框设置为不可见
            this.QueryBooks() // 重新查询图书以刷新页面
          })
          .catch(error=>{
            ElMessage.error("图书信息修改失败，请检查输入是否合法")
            this.modifyBookVisible = false
            this.QueryBooks()
          })
    },
    ConfirmRemoveBook() {
      axios.post("/home/book/",
          { // 请求体
            id: this.toRemove // 请求体
          })
          .then(response => {
            ElMessage.success("图书删除成功") // 显示消息提醒
            this.removeBookVisible = false // 将对话框设置为不可见
            this.QueryBooks() // 重新查询图书以刷新页面
          })
          .catch(error=>{
            ElMessage.error("图书删除失败,可能本书正在被借用")
            this.borrowBookVisible = false
            this.QueryBorrows()
          })
    },
    ConfirmBorrowBook(){
      axios.post("/home/book/",
          { // 请求体
            id: this.borrowBookInfo.id,
            card_id:this.borrowBookInfo.card_id
          })
          .then(response => {
            ElMessage.success("图书借阅成功") // 显示消息提醒
            this.borrowBookVisible = false // 将对话框设置为不可见
            this.QueryBooks() // 重新查询图书以刷新页面
          })
          .catch(error=>{
            ElMessage.error("图书借阅失败,可能借书证无效")
            this.borrowBookVisible = false
            this.QueryBorrows()
          })
    },
    Multi_condition_search(){
      this.books = [] // 清空列表
      axios.get(
          '/home/book/',
          { params: { // 请求体
              category: this.multi_cond_BookInfo.category, // 请求体
              title: this.multi_cond_BookInfo.title, // 请求体
              press: this.multi_cond_BookInfo.press,
              minPublishYear: this.multi_cond_BookInfo.minPublishYear,
              maxPublishYear: this.multi_cond_BookInfo.maxPublishYear,
              author: this.multi_cond_BookInfo.author,
              minPrice: this.multi_cond_BookInfo.minPrice,
              maxPrice: this.multi_cond_BookInfo.maxPrice
            } }

      )
          .then(response => {
            ElMessage.success("多条件图书查询成功") // 显示消息提醒
            this.multiCondBookVisible = false // 将对话框设置为不可见
            let books = response.data // 接收响应负载
            books.forEach(book => { // 对于每个图书
              this.books.push(book) // 将其加入到列表中
            })
          })
          .catch(error=>{
            ElMessage.error("多条件图书查询失败")
            this.multiCondBookVisible = false
          })
    },
    QueryBooks() {
      this.books = [] // 清空列表
      let response = axios.get('/home/book') // 向/book发出GET请求
          .then(response => {
            let books = response.data // 接收响应负载
            books.forEach(book => { // 对于每个图书
              this.books.push(book) // 将其加入到列表中
            })
          })
    }
  },
  mounted() { // 当页面被渲染时
    this.QueryBooks() // 查询图书
  }
}

</script>

<style scoped>
.bookBox {
  height: 500px;
  width: 250px;
  box-shadow: 0 4px 8px 0 rgba(0, 0, 0, 0.2), 0 6px 20px 0 rgba(0, 0, 0, 0.19);
  text-align: center;
  margin-top: 40px;
  margin-left: 27.5px;
  margin-right: 10px;
  padding: 7.5px;
  padding-right: 10px;
  padding-top: 15px;
}

.newBookBox {
  height: 500px;
  width: 250px;
  margin-top: 40px;
  margin-left: 27.5px;
  margin-right: 10px;
  padding: 7.5px;
  padding-right: 10px;
  padding-top: 15px;
  box-shadow: 0 4px 8px 0 rgba(0, 0, 0, 0.2), 0 6px 20px 0 rgba(0, 0, 0, 0.19);
  text-align: center;
}
</style>