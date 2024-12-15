<!-- TODO: YOUR CODE HERE -->
<template>



  <el-scrollbar height="100%" style="width: 100%;">
    <!-- 动画节点 -->
    <div id="loader-wrapper" v-show="loadingVisible">
      <div id="loader"></div>

      <div class="load_title">正在加载,请耐心等待
        <br>
        <span>爬虫运行中</span>
      </div>
    </div>
    <div class="empty-wrapper" v-show="products.length === 0 && !loadingVisible">
      <img class="empty" src="../../assets/img/empty.png" alt="No content"/>
      <div class="empty_title">请执行爬虫或加载历史记录
        <br>
        <span>空空如也...</span>
      </div>
    </div>
    <div class="history-container" style="display: flex; flex-direction: column; align-items: center; gap: 10px; width:100vw">
      <span style="font-weight: bold">降价提醒</span>

      <el-button @click="openDialog"
                 style="padding: 5px 15px; font-size: 14px;"
                 type="primary"
                 icon="UploadFilled"
                 class="button-gradient">
        设置更新时间间隔
      </el-button>

      <el-button
          @click="QueryProducts"
          type="primary"
          icon="UploadFilled"
          class="button-gradient"
          style="padding: 5px 15px; font-size: 14px;">
        显示收藏商品
      </el-button>

      <el-button
          @click="CrawNewPrices"
          type="primary"
          icon="UploadFilled"
          class="button-gradient"
          style="padding: 5px 15px; font-size: 14px;">
        爬取最新价格
      </el-button>

    </div>
    <!-- el-dialog 弹出框 -->
    <el-dialog
        title="设置商品更新时间间隔"
        v-model:="dialogVisible"
        width="400px"
        @close="handleClose"
    >
      <!-- 标题分隔线 -->
      <div style="border-bottom: 1px solid #e0e0e0; margin-bottom: 20px;"></div>

      <!-- 启用更新的滑块 -->
      <div style="margin-bottom: 20px; text-align: center;">
        <el-switch
            v-model="isEnabled"
            active-text="启用"
            inactive-text="禁用"
            active-color="#ec4a18"
            inactive-color="#ec4a18"
        ></el-switch>
      </div>

      <!-- 表单内容 -->
      <el-form
          v-if="isEnabled"
          label-width="120px"
          style="margin-top: 10px;"
      >
        <p v-if="intervalSet">当前更新时间间隔：{{ displayInterval }}</p>
        <!-- 选择更新时间间隔 -->
        <el-form-item label="更新时间间隔">
          <el-select
              v-model="selectedInterval"
              placeholder="选择更新时间间隔"
              style="width: 100%;"
          >
            <el-option label="30分钟" value="1800000"></el-option>
            <el-option label="1小时" value="3600000"></el-option>
            <el-option label="2小时" value="7200000"></el-option>
            <el-option label="4小时" value="14400000"></el-option>
            <el-option label="自定义时间" value="custom"></el-option>
          </el-select>
        </el-form-item>

        <!-- 自定义时间输入框 -->
        <el-form-item v-if="selectedInterval === 'custom'" label="自定义时间">
          <el-input
              v-model="customInterval"
              placeholder="输入自定义时间（分钟）"
              type="number"
              style="width: 100%;"
          />
        </el-form-item>
      </el-form>

      <!-- 按钮区域 -->
      <div slot="footer" class="dialog-footer" style="text-align: right;">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" class="button-gradient" @click="saveInterval">确定</el-button>
      </div>
    </el-dialog>

    <!-- 商品横条显示区 -->
    <div style="display: flex; flex-direction: column; width: 100%;">

      <!-- 商品横条 -->
      <div
          class="productRow product-item"
          v-for="product in products"
          v-show="product.title.includes(toSearch) && product.favorite !== 0"
          :key="product.id"
      >
        <!-- 商品内容 -->
        <div class="product-content">
          <!-- 图片部分 -->
          <div class="product-image">
            <img
                :src="product.img_url"
                alt="Product Image"
                class="img"
            />
          </div>

          <!-- 信息部分 -->
          <div class="product-info">
            <div class="product-title">
              <p class="title">
                <span v-if="product.source.includes('jd') || product.source.includes('360')" class="source-jd">京东 </span>
                <span v-else-if="product.source.includes('tmall') || product.source.includes('taobao')" class="source-taobao">淘宝 </span>
                {{ product.title }}
              </p>
            </div>
            <p class="shop-name">店铺: <span>{{ product.shop }}</span></p>
            <p class="sales">
              <span v-if="product.source.includes('jd')">评论数: {{ product.comment }}</span>
              <span v-else-if="product.source.includes('tmall') || product.source.includes('taobao')">销量: {{ product.deal }}</span>
            </p>
            <div class="price">
              <p class="old-price">旧价格: ￥{{ product.price }}</p>
              <p class="new-price">新价格: ￥<span v-if="this.priceUpdates">{{ this.priceUpdates[product.id] }}</span><span v-else>？</span></p>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="product-actions">
            <el-button
                @click.stop="product.favorite = 0; products = products.filter(p => p.favorite !== 0);"
                icon="Delete"
                style="background-color: #e8b9b5; color: white; border: none; border-radius: 5px; padding: 5px 10px; cursor: pointer;">
              取消收藏商品
            </el-button>
            <el-button
                @click="CrawNewPrices_single(product)"
                icon="UploadFilled"
                style="background-color: #fa8035; color: white; border: none; border-radius: 5px; padding: 5px 10px; cursor: pointer; margin-top: 5px;">
              爬取最新价格
            </el-button>
          </div>
        </div>
      </div>
    </div>
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
      default: '请输入要搜索的商品名称或关键词'
    }
  },
  computed: {
    filteredProductsList() {
      return this.filter ? this.filteredProducts : this.similarProducts;
    },
    displayInterval() {
      if (this.selectedInterval && this.selectedInterval !== "custom") {
        let hours = Math.floor(this.selectedInterval / 3600000);
        let minutes = (this.selectedInterval % 3600000) / 60000;
        return `${hours}小时 ${minutes}分钟`;
      }
      if (this.selectedInterval === 'custom' && this.customInterval) {
        let hours = Math.floor(this.customInterval / 3600);
        let minutes = (this.customInterval % 3600) / 60;
        return `${hours}小时 ${minutes}分钟`;
      }
      return "未设置";
    }
  },
  data() {
    return {
      products: [], // 商品列表
      similarProducts: [], // 相似商品列表
      filteredProducts: [], // 过滤后的商品列表
      updatedProducts:[],
      compareProducts: [], // 收藏商品列表
      compareProducts_single:[],
      priceUpdates: {}, // 价格更新
      Delete,
      Edit,
      Search,
      toSearch: '', // 搜索内容
      multiCondProductVisible:false,
      detailedProductVisible:false,
      priceHistoryVisible:false,
      loading_history_Visible:false,
      loadingVisible:false,
      filter: false,
      crawled_already:false,
      searchQuery:'',
      similarProducts_id:0,
      activeTab: 'history',  // 默认选中 "历史记录查询"
      history_img_src:'',
      detailedProductInfo:{
        id:'',
        comment:'',
        title:'',
        shop:'',
        deal:'',
        img_url:'',
        price:'',
        source:'',
        favorite:''
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
      },
      selectedInterval: '', // 选择的更新时间间隔
      isEnabled: false, // 是否启用更新
      dialogVisible: false, // 控制对话框显示与否
      customInterval: null, // 自定义时间（秒）
      updateInterval: null, // 存储定时器ID
      intervalSet: false, // 用于显示当前更新时间间隔的状态
    }
  },
  watch: {
    // 监听 activeTab 的变化
    activeTab(newTab, oldTab) {
      // 如果切换到"历史记录查询" Tab
      console.log(newTab)
      console.log(oldTab)
      if (newTab === 'history') {
        this.showPriceHistory();  // 调用显示历史价格的函数
      }
      // 如果切换到"相似产品列表" Tab
      else if (newTab === 'similar') {
        this.get_similar_products(this.similarProducts_id);  // 调用获取相似产品的函数
      }
    }
  },
  methods: {
    openDialog() {
      this.dialogVisible = true;
    },
    getMostSimilarProducts(products, targetIndex, topN = 3) {
      // 计算 Jaccard 相似性
      function jaccardIndex(set1, set2) {
        const intersection = new Set([...set1].filter(item => set2.has(item)));
        const union = new Set([...set1, ...set2]);
        return intersection.size / union.size;
      }

      // 提取标题中的关键词并转换为 Set（使用空格分割单词）
      function extractKeywords(title) {
        return new Set(title.toLowerCase().split(/\s+/));
      }
      console.log(targetIndex)
      const targetProduct = products[targetIndex];
      console.log(targetProduct)
      const targetKeywords = extractKeywords(targetProduct.title);

      const similarityScores = products.map((product, index) => {
        if (index === targetIndex) return { index, score: -1 }; // 排除自己
        const keywords = extractKeywords(product.title);
        const score = jaccardIndex(targetKeywords, keywords);
        return { index, score };
      });

      // 按照相似度降序排序
      similarityScores.sort((a, b) => b.score - a.score);

      // 返回前 N 个相似的商品
      return similarityScores.slice(0, topN).map(item => products[item.index]);
    },
    get_similar_products(id){
      this.similarProducts = this.getMostSimilarProducts(this.products, id, 10);
      console.log(this.similarProducts);
    },
    search() {
      this.products = [] // 清空列表
      this.loadingVisible = true;
      this.$emit("search", ['search', this.keyword])
      // 创建一个 EventSource 对象，连接到后端的 /search 路径
      const eventSource = new EventSource(`http://localhost:8000/search?keyword=${encodeURIComponent(this.keyword)}`);

      // 当接收到数据时触发 'message' 事件
      eventSource.onmessage = function(event) {
        this.loadingVisible = false;
        const product = JSON.parse(event.data);
        console.log(product);
        this.products.push(product);
      }.bind(this);

      // 当连接关闭时触发
      eventSource.onopen = function() {
        console.log("连接已打开");
      };

      eventSource.onclose = function() {
        ElMessage.success("搜索执行成功") // 显示消息提醒
        console.log("连接已关闭");
        eventSource.close(); // 关闭连接
        // 可以在这里执行一些清理操作，或者重新连接等
      };
      // 当发生错误时触发
      eventSource.onerror = function(error) {
        console.group("EventSource Error"); // 打开一个分组日志
        console.error("发生错误:");
        console.error("错误详情：", error); // 打印完整错误对象

        console.error("连接状态:", error.target.readyState); // 打印连接状态
        switch (error.target.readyState) {
          case EventSource.CONNECTING:
            console.warn("尝试重新连接...");
            break;
          case EventSource.CLOSED:
            console.error("连接已关闭，无法恢复");
            break;
          default:
            console.error("未知错误状态");
        }
        console.groupEnd(); // 结束分组
        ElMessage.error("搜索执行失败，请检查网络或后端状态");
        eventSource.close(); // 关闭连接以防止意外行为
      };
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
    QueryProducts() {
      this.products = [] // 清空列表
      console.log("QueryProducts called")
      axios.get('/home/product',{
        params: { // 请求体
          user_name:this.$store.state.username} }) // 向/product发出GET请求
          .then(response => {
            // let cleanedData = response.data.replace(/[\n\r\t]/g, '');
            let products = response.data;
            console.log(products);
            products.forEach(product => { // 对于每个商品
              this.products.push(product) // 将其加入到列表中
            })
          })
    },
    CrawNewPrices(){
      this.priceUpdates = {};
      console.log("CrawNewPrices called")
      //遍历products,如果favorite为1,则将其加入到compare_products中
      this.products.forEach(product => {
        if(product.favorite === 1){
          this.compareProducts.push(product)
        }
      })
      axios.post('/update', {
        params: {
          user_name:this.$store.state.username,
          product: this.compareProducts
        }
      }).then(res => {
        console.log(res.data)
        this.priceUpdates = res.data;
        ElMessage.success("更新商品价格成功")
      }).catch(err => {
        ElMessage.error("更新商品价格失败")
      })
    },
    CrawNewPrices_single(product){
      this.compareProducts_single = [];
      console.log("CrawNewPrices called")
      this.compareProducts_single.push(product)
      axios.post('/update', {
        params: {
          user_name:this.$store.state.username,
          product: this.compareProducts_single
        }
      }).then(res => {
        console.log(res.data)
        this.priceUpdates = res.data;
        ElMessage.success("更新商品价格成功")
      }).catch(err => {
        ElMessage.error("更新商品价格失败")
      })
    },
    saveInterval() {
      let intervalInSeconds;
      if (this.selectedInterval === 'custom' && this.customInterval) {
        // 用户选择了自定义时间，并且输入了分钟数
        intervalInSeconds = this.customInterval * 60; // 将分钟转换为秒
        this.displayInterval = `${this.customInterval} 分钟`;
      } else {
        // 从预设选项中获取秒数，并格式化显示
        intervalInSeconds = this.getIntervalInSeconds(this.selectedInterval);
      }
      this.intervalSet = true;
      this.dialogVisible = false;

      // 输出定时器的秒数，可以用来执行定时更新功能
      console.log('定时器设置的秒数: ', intervalInSeconds);
      axios.get('/update/',{
        params: { // 请求体
          user_name: this.$store.state.username,
          interval: intervalInSeconds  } }) // 向/product发出GET请求
          .then(response => {
            console.log("set interval success")
            ElMessage.success("设置商品更新时间间隔成功") // 显示消息提醒
          })
          .catch(error=>{
            ElMessage.error("设置商品更新时间间隔失败")
          })
    },

    getIntervalInSeconds(interval) {
      // 这里假设获取秒数的函数会根据传入的值返回对应的秒数
      const intervals = {
        '1800000': 30 * 60,  // 30分钟
        '3600000': 60 * 60,  // 1小时
        '7200000': 2 * 60 * 60, // 2小时
        '14400000': 4 * 60 * 60, // 4小时
      };

      // 如果是自定义，返回分钟数转换为秒数
      if (interval === 'custom') return this.customInterval * 60;

      return intervals[interval] || 0;
    },
    sortedProducts() {
      let filteredProducts = this.similarProducts;

      // 根据搜索框内容过滤
      if (this.searchQuery) {
        filteredProducts = filteredProducts.filter(product =>
            product.title.toLowerCase().includes(this.searchQuery.toLowerCase())
        );
      }

      // 根据排序条件排序
      if (this.sortMethod === 'priceAsc') {
        filteredProducts = filteredProducts.sort((a, b) => a.price - b.price);
      } else if (this.sortMethod === 'priceDesc') {
        filteredProducts = filteredProducts.sort((a, b) => b.price - a.price);
      } else if (this.sortMethod === 'title') {
        filteredProducts = filteredProducts.sort((a, b) => a.title.localeCompare(b.title));
      }

      this.filteredProducts=filteredProducts;
      this.filter = true;
    },
    showPriceHistory() {
      this.similarProducts = [];
      if(this.crawled_already){
        return;
      }
      this.priceHistoryVisible = false; // 显示图像
      this.loading_history_Visible = true;
      axios.post('/search/', {
        params: {
          url: this.detailedProductInfo.source
        }
      }).then(res => {
        ElMessage.success("查找历史价格成功");
        // this.history_img_src = history;  // 假设 res 中有历史图像地址
        console.log(res.data)
        this.crawled_already = true;
        // 使用 setTimeout 延迟绘制操作，等待 canvas 完全渲染
        // this.priceHistoryVisible = true;  // 显示图像
        this.history_img_src = '/history.png?' + new Date().getTime();
        console.log(this.history_img_src);
        setTimeout(() => {
          this.loading_history_Visible = false;
          this.priceHistoryVisible = true; // 显示图像
        }, 500); // 延迟 500 毫秒（0.5秒）

      }).catch(err => {
        ElMessage.error("查找历史价格失败");
        console.log(err);
      });
    },
    toggleFavorite(product) {
      //切换商品的收藏状态
      console.log("toggleFavorite called")
      axios.post('/home/product/', {
        params: {
          user_name: this.$store.state.username,
          like: product
        }
      }).then(res => {
        ElMessage.success("修改商品收藏状态成功")
      }).catch(err => {
        ElMessage.error("修改商品收藏状态失败")
      })
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
  border-radius: 8px;
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
.chromeframe {
  margin: 0.2em 0;
  background: #fff;
  color: #000;
  padding: 0.2em 0;
}

#loader-wrapper {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 999999;
}

#loader {
  display: block;
  position: relative;
  left: 50%;
  top: 50%;
  width: 150px;
  height: 150px;
  margin: -75px 0 0 -75px;
  border-radius: 50%;
  border: 3px solid transparent;
  border-top-color: #fd613e;
  -webkit-animation: spin 2s linear infinite;
  -ms-animation: spin 2s linear infinite;
  -moz-animation: spin 2s linear infinite;
  -o-animation: spin 2s linear infinite;
  animation: spin 2s linear infinite;
  z-index: 1001;
}

#loader:before {
  content: "";
  position: absolute;
  top: 5px;
  left: 5px;
  right: 5px;
  bottom: 5px;
  border-radius: 50%;
  border: 3px solid transparent;
  border-top-color: #fd613e;
  -webkit-animation: spin 3s linear infinite;
  -moz-animation: spin 3s linear infinite;
  -o-animation: spin 3s linear infinite;
  -ms-animation: spin 3s linear infinite;
  animation: spin 3s linear infinite;
}

#loader:after {
  content: "";
  position: absolute;
  top: 15px;
  left: 15px;
  right: 15px;
  bottom: 15px;
  border-radius: 50%;
  border: 3px solid transparent;
  border-top-color: #fd613e;
  -moz-animation: spin crawler.5s linear infinite;
  -o-animation: spin crawler.5s linear infinite;
  -ms-animation: spin crawler.5s linear infinite;
  -webkit-animation: spin crawler.5s linear infinite;
  animation: spin crawler.5s linear infinite;
}

@-webkit-keyframes spin {
  0% {
    -webkit-transform: rotate(0deg);
    -ms-transform: rotate(0deg);
    transform: rotate(0deg);
  }

  100% {
    -webkit-transform: rotate(360deg);
    -ms-transform: rotate(360deg);
    transform: rotate(360deg);
  }
}

@keyframes spin {
  0% {
    -webkit-transform: rotate(0deg);
    -ms-transform: rotate(0deg);
    transform: rotate(0deg);
  }

  100% {
    -webkit-transform: rotate(360deg);
    -ms-transform: rotate(360deg);
    transform: rotate(360deg);
  }
}

#loader-wrapper .loader-section {
  position: fixed;
  top: 0;
  width: 51%;
  height: 100%;
  background: #ffffff;
  z-index: 1000;
  -webkit-transform: translateX(0);
  -ms-transform: translateX(0);
  transform: translateX(0);
}

#loader-wrapper .loader-section.section-left {
  left: 0;
}

#loader-wrapper .loader-section.section-right {
  right: 0;
}

.loaded #loader-wrapper .loader-section.section-left {
  -webkit-transform: translateX(-100%);
  -ms-transform: translateX(-100%);
  transform: translateX(-100%);
  -webkit-transition: all 0.7s 0.3s cubic-bezier(0.645, 0.045, 0.355, crawler.000);
  transition: all 0.7s 0.3s cubic-bezier(0.645, 0.045, 0.355, crawler.000);
}

.loaded #loader-wrapper .loader-section.section-right {
  -webkit-transform: translateX(100%);
  -ms-transform: translateX(100%);
  transform: translateX(100%);
  -webkit-transition: all 0.7s 0.3s cubic-bezier(0.645, 0.045, 0.355, crawler.000);
  transition: all 0.7s 0.3s cubic-bezier(0.645, 0.045, 0.355, crawler.000);
}

::v-deep .el-dialog{
  display: flex;
  flex-direction: column;
  margin:0 !important;
  position:absolute;
  top:50%;
  left:50%;
  transform:translate(-50%,-50%);
  max-height:calc(100% - 20px);
  max-width:calc(100% - 20px);
}
::v-deep  .el-dialog .el-dialog__body{
  flex:1;
  overflow: auto;
}


.loaded #loader {
  opacity: 0;
  -webkit-transition: all 0.3s ease-out;
  transition: all 0.3s ease-out;
}

.loaded #loader-wrapper {
  visibility: hidden;
  -webkit-transform: translateY(-100%);
  -ms-transform: translateY(-100%);
  transform: translateY(-100%);
  -webkit-transition: all 0.3s 1s ease-out;
  transition: all 0.3s 1s ease-out;
}

.no-js #loader-wrapper {
  display: none;
}

.no-js h1 {
  color: #222222;
}

#loader-wrapper .load_title {
  font-family: 'Open Sans';
  color: #fd613e;
  font-size: 19px;
  width: 100%;
  text-align: center;
  z-index: 9999999999999;
  position: absolute;
  top: 60%;
  opacity: crawler;
  line-height: 30px;
}

#loader-wrapper .load_title span {
  font-weight: normal;
  font-style: italic;
  font-size: 13px;
  color: #fd613e;
  opacity: 0.5;
}

.empty {
  position: relative;
  left: 50%;
  top: 50%;
  width: 150px;
  height: 150px;
  margin: -75px 0 0 -75px;
}

.empty-wrapper {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

.empty-wrapper .empty_title {
  font-family: 'Open Sans';
  color: #fd613e;
  font-size: 19px;
  width: 100%;
  text-align: center;
  position: absolute;
  top: 60%;
  opacity: 1;
  line-height: 30px;
}

.empty-wrapper .empty_title span {
  font-weight: normal;
  font-style: italic;
  font-size: 13px;
  color: #fd613e;
  opacity: 0.5;
}

.button-gradient {
  background: linear-gradient(to bottom, #fd8c4c, #ffb84d); /* 温和的橙色到金色渐变 */
  border: none; /* 去掉边框 */
  color: #fff;  /* 设置文本颜色为白色 */
  transition: background 0.3s ease; /* 为背景颜色过渡添加平滑动画 */
}

.button-gradient:hover {
  background: linear-gradient(to bottom, #ffb84d, #fd8c4c); /* 悬停时反转渐变颜色 */
}

.button-gradient:active {
  background: linear-gradient(to bottom, #fd8c4c, #ffb84d); /* 激活时恢复原渐变色 */
}
.product-item {
  display: flex;
  flex-direction: column;
  padding: 10px;
  border-bottom: 1px solid #eaeaea;
  margin-bottom: 10px;
  background-color: #fff;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.product-content {
  display: flex;
  flex-direction: column;
}

.product-image {
  width: 100%;
  height: auto;
  margin-bottom: 15px;
}

.product-image .img {
  width: 100%;
  height: auto;
  object-fit: cover;
  border-radius: 5px;
}

.product-info {
  display: flex;
  flex-direction: column;
  margin-bottom: 10px;
}

.product-title {
  margin-bottom: 8px;
}

.title {
  font-size: 16px;
  font-weight: bold;
  color: #333;
  margin: 0;
}

.source-jd, .source-taobao {
  color: #e74c3c;
  font-weight: bold;
}

.shop-name {
  font-size: 14px;
  color: #666;
  margin: 0;
}

.sales {
  font-size: 14px;
  color: #999;
  margin: 0;
}

.price {
  margin-top: 10px;
}

.old-price {
  font-size: 14px;
  text-decoration: line-through;
  color: #999;
}

.new-price {
  font-size: 18px;
  font-weight: bold;
  color: #e74c3c;
}

.product-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.product-actions el-button {
  width: 100%;
  margin-bottom: 10px;
}

/* 响应式调整 */
@media (min-width: 768px) {
  .product-content {
    flex-direction: row;
    justify-content: space-between;
  }

  .product-image {
    width: 100px;
    height: 100px;
    margin-right: 15px;
  }

  .product-info {
    flex: 1;
  }

  .product-actions {
    margin-left: 10px;
    align-items: flex-start;
  }
}

</style>