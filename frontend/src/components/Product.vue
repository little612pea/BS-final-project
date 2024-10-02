<!-- TODO: YOUR CODE HERE -->
<template>
  <el-scrollbar height="100%" style="width: 100%;">


    <div style="margin-top: 20px; margin-left: 40px; font-size: 2em; font-weight: bold; ">商品比价
      <el-input v-model="toSearch" :prefix-icon="Search"
                style=" width: 15vw;min-width: 150px; margin-left: 30px; margin-right: 30px; float: right;" clearable />
      <!--在button文字的前面加上icon-->
      <!-- 标题和搜索框 -->
      <div style="display: inline-flex">
        <el-input
            v-model="keyword"
            :disabled="disabled"
            :placeholder="placeholder"
            prefix-icon="el-icon-search"
            style="width: 350px;margin-right: 10px"
            clearable></el-input>
        <el-button
            :disabled="disabled"
            icon="el-icon-search"
            type="primary"
            @click="search">
          搜索
        </el-button>
      </div>
      <el-button @click="multi_cond_ProductInfo.title = '',multi_cond_ProductInfo.img_url = '',multi_cond_ProductInfo.comment = '',  multi_cond_ProductInfo.shop = '', multi_cond_ProductInfo.deal = '',  multi_cond_ProductInfo.price = '', multi_cond_ProductInfo.source = '',
      multiCondProductVisible = true" style="float: right;" type="primary":icon="Search">
        多条件查询
      </el-button>
<!--      <el-button @click="uploadFileInfo = '', uploadFileInfoVisible = true" style="float: right;margin-right: 10px" type="primary":icon="UploadFilled">-->
<!--        商品批量入库-->
<!--      </el-button>-->
    </div>



    <!-- 商品卡片显示区 -->
    <div style="display: flex;flex-wrap: wrap; justify-content: start;">

      <!-- 商品卡片 -->
      <div class="productBox" v-for="product in products" v-show="product.title.includes(toSearch)" :key="product.id">
        <div @click="detailedProductInfo.id = product.id; detailedProductVisible = true">
        <!-- 卡片标题 -->
          <div style="font-size: 15px; font-weight: bold;">{{product.title}}</div>

          <el-divider />

          <!-- 卡片内容 -->
          <div style="margin: 10px; text-align: start; font-size: 16px;">
            <img :src="product.img_url" alt="Product Image" style="max-width: 100%; border-radius: 5px;"/>
            <p style="padding: 2.5px;"><span style="font-weight: bold;">店铺：</span>{{ product.shop }}</p>
            <p style="padding: 2.5px;"><span style="font-weight: bold;">价格：</span>{{ product.price }}</p>
            <p style="padding: 2.5px;">
              <span style="font-weight: bold;">来源：</span>
              <span v-if="product.source === 1">京东</span>
              <span v-else-if="product.source === 0">淘宝</span>
            </p>

            <!-- 当 source 为 1 时，显示 comment -->
            <p style="padding: 2.5px;" v-if="product.source === 1">
              <span style="font-weight: bold;">评论：</span>{{ product.comment }}
            </p>

            <!-- 当 source 为 0 时，显示 deal -->
            <p style="padding: 2.5px;" v-if="product.source === 0">
              <span style="font-weight: bold;">交易量：</span>{{ product.deal }}
            </p>
          </div>
        </div>
      </div>


    </div>


    <!-- 按照各种条件搜索商品 -->
    <el-dialog v-model="multiCondProductVisible" title="多条件商品查询" width="30%" align-center>
      <div style="margin-left: 1vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
        书名(模糊查询)：
        <el-input v-model="multi_cond_ProductInfo.title" style="width: 12.5vw;" clearable />
      </div>
      <div style="margin-left: 1vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
        店名(模糊查询)：
        <el-input v-model="multi_cond_ProductInfo.shop" style="width: 12.5vw;" clearable />
      </div>
      <div style="margin-left: 1vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
        来源(精确点查)：
        <el-input v-model="multi_cond_ProductInfo.source" style="width: 12.5vw;" clearable />
      </div>
      <!-- 价格:下限-上限 -->
      <div style="margin-left: 1vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
        价格(下限-上限)：
        <el-input v-model="multi_cond_ProductInfo.minPrice" style="width: 5vw;" clearable />
        <span> - </span>
        <el-input v-model="multi_cond_ProductInfo.maxPrice" style="width: 5vw;" clearable />
      </div>
      <template #footer>
                <span>
                    <el-button @click="multiCondProductVisible = false">取消</el-button>
                    <el-button type="primary" @click="Multi_condition_search">确定</el-button>
                </span>
      </template>
    </el-dialog>

    <el-dialog v-model="detailedProductVisible" title="查看商品" width="30%">
      <span>确定借阅<span style="font-weight: bold;">{{ detailedProductInfo.id }}号商品</span>吗？</span>
      <div style="margin-left: 10px; text-align: start; font-size: 16px;">
        <p style="padding: 2.5px;"><span style="font-weight: bold;">店铺：</span>{{ detailedProductInfo.shop }}</p>
        <p style="padding: 2.5px;"><span style="font-weight: bold;">图片：</span>{{ detailedProductInfo.img_url }}</p>
        <p style="padding: 2.5px;"><span style="font-weight: bold;">价格：</span>{{ detailedProductInfo.price }}</p>
        <p style="padding: 2.5px;"><span style="font-weight: bold;">来源：</span>{{ detailedProductInfo.source }}</p>
      </div>
    </el-dialog>

  </el-scrollbar>
</template>

<script>
import {Delete, Edit, Search, UploadFilled} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

export default {
  props:{
    disabled:{
      type:Boolean,
      default:false
    },
    placeholder:{
      type:String,
      default: '请输入昵称进行搜索，可以直接回车搜索...'
    }
  },
  computed: {
    UploadFilled() {
      return UploadFilled
    }

  },
  data() {
    return {
      products: [], // 商品列表
      Delete,
      Edit,
      Search,
      toSearch: '', // 搜索内容
      multiCondProductVisible:false,
      detailedProductVisible:false,
      detailedProductInfo:{
        productID:'',
        comment:'',
        title:'',
        shop:'',
        deal:'',
        img_url:'',
        price:'',
        source:''
      },
      uploadFileInfo:'',
      keyword:'',
      multi_cond_ProductInfo:{
        productID:'',
        title:'',
        shop:'',
        maxPrice:'',
        minPrice:'',
        source:''
      }
    }
  },
  methods: {
    search() {
      this.$emit("search", ['search', this.keyword])
      axios.get('/search', {
        params: {
          keyword: this.keyword
        }
      }).then(res => {
        ElMessage.success("搜索执行成功") // 显示消息提醒
        this.products = [] // 清空列表
        let products = res.data // 接收响应负载
        console.log(products)
        products.forEach(product => { // 对于每个商品
          this.products.push(product) // 将其加入到列表中
        })
      }).catch(err => {
        ElMessage.error("搜索执行失败")
      })
    },
    DetailedProductInfo(){
      axios.post("/book/",
          { // 请求体
            id: this.DetailedProductInfo.id,
            card_id:this.DetailedProductInfo.card_id
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
      this.products = [] // 清空列表
      axios.get(
          '/home/product/',
          { params: { // 请求体
              id: this.multi_cond_ProductInfo.id,
              title: this.multi_cond_ProductInfo.title, // 请求体
              shop: this.multi_cond_ProductInfo.shop, // 请求体
              minPrice: this.multi_cond_ProductInfo.minPrice,
              maxPrice: this.multi_cond_ProductInfo.maxPrice,
              source: this.multi_cond_ProductInfo.source
            } }

      )
          .then(response => {
            ElMessage.success("多条件商品查询成功") // 显示消息提醒
            this.multiCondProductVisible = false // 将对话框设置为不可见
            let products = response.data // 接收响应负载
            products.forEach(product => { // 对于每个商品
              this.products.push(product) // 将其加入到列表中
            })
          })
          .catch(error=>{
            ElMessage.error("多条件商品查询失败")
            this.multiCondProductVisible = false
          })
    },
    QueryProducts() {
      this.products = [] // 清空列表
      let response = axios.get('/home/product') // 向/product发出GET请求
          .then(response => {
            let products = response.data // 接收响应负载
            products.forEach(product => { // 对于每个商品
              this.products.push(product) // 将其加入到列表中
            })
          })
    },
    mounted() { // 当页面被渲染时
      this.QueryProducts() // 查询商品
    }
}
}

</script>

<style scoped>
.productBox {
  height: 500px;
  width: 280px;
  box-shadow: 0 4px 8px 0 rgba(0, 0, 0, 0.2), 0 6px 20px 0 rgba(0, 0, 0, 0.19);
  text-align: center;
  margin-top: 40px;
  margin-left: 27.5px;
  margin-right: 10px;
  padding: 7.5px;
  padding-right: 10px;
  padding-top: 15px;
}

.newProductBox {
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