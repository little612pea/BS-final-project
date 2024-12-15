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
    <div style="margin-top: 20px; margin-left: 40px; font-size: 2em; font-weight: bold; ">商品比价
      <el-input v-model="toSearch" :prefix-icon="Search"
                style=" width: 15vw;min-width: 150px; margin-left: 30px; margin-right: 30px; float: right;" clearable />
      <!--在button文字的前面加上icon-->
      <!-- 标题和搜索框 -->
        <el-input
            v-model="keyword"
            :disabled="disabled"
            :placeholder="placeholder"
            prefix-icon="el-icon-search"
            style="width: 350px;margin-right: 10px"
            clearable></el-input>
      <el-button
          :disabled="disabled"
          icon="Search"
          type="primary"
          @click="search"
          class="button-gradient">
        搜索
      </el-button>
<!--      <el-button @click="multi_cond_ProductInfo.title = '',multi_cond_ProductInfo.img_url = '',multi_cond_ProductInfo.comment = '',  multi_cond_ProductInfo.shop = '', multi_cond_ProductInfo.deal = '',  multi_cond_ProductInfo.price = '', multi_cond_ProductInfo.source = '',-->
<!--      multiCondProductVisible = true" style="float: right;" type="primary":icon="Search">-->
<!--        多条件查询-->
<!--      </el-button>-->
      <el-button
          @click="QueryProducts"
          style="float: right;margin-right: 10px"
          type="primary"
          icon="UploadFilled"
          class="button-gradient">
        显示历史记录
      </el-button>

      <el-button
          @click="StoreSearchResults"
          style="float: right;margin-right: 10px"
          type="primary"
          icon="UploadFilled"
          class="button-gradient">
        保存搜索结果
      </el-button>
    </div>



    <!-- 商品卡片显示区 -->
    <div style="display: flex;flex-wrap: wrap; justify-content: start;">

      <!-- 商品卡片 -->
      <div class="productBox" v-for="product in products" v-show="product.title.includes(toSearch)" :key="product.id">
        <div @click="detailedProductInfo.title = product.title; detailedProductInfo.shop = product.shop; detailedProductInfo.price=product.price; detailedProductInfo.img_url=product.img_url; detailedProductInfo.source=product.source; detailedProductInfo.favorite = product.favorite; detailedProductVisible = true; similarProducts_id = product.id; this.similarProducts=[];this.crawled_already = false">
        <!-- 卡片标题 -->
          <div style="margin: 0px; padding: 0px; background-color: #fff; border-radius: 8px; box-shadow: 0 2px 8px rgba(0, 0, 0, 0);">
            <!-- 图片 -->
            <img :src="product.img_url" alt="Product Image" style="width: 100%; border-radius: 5px;"/>

            <div style="margin-top: 10px; text-align: left;">
              <p style="font-size: 18px; font-weight: bold; color: #e74c3c; margin: 5px 0;">
                <span style="color: #333; display: -webkit-box; -webkit-box-orient: vertical; overflow: hidden; -webkit-line-clamp: 3;">
                  <span v-if="product.source.includes('jd')||product.source.includes('360')" style="color: #e74c3c;font-weight: bold;">京东 </span>
                  <span v-else-if="product.source.includes('tmall') || product.source.includes('taobao')" style="color: #e74c3c;font-weight: bold;">淘宝 </span>
                  {{ product.title }}
                </span>
              </p>

              <!-- 价格和销量放在同一行 -->
              <div style="display: flex; align-items: center; justify-content: space-between;">
                <p style="font-size: 24px; font-weight: bold; color: #e74c3c; margin: 5px;">
                  ￥{{ product.price }}
                </p>
                <p style="font-size: 16px; color: #666; margin: 5px 0;">
                  <span v-if="product.source.includes('jd')">{{ product.comment }}</span>
                  <span v-else-if="product.source.includes('tmall') || product.source.includes('taobao')">{{ product.deal }}</span>
                </p>
              </div>

              <!-- 店铺名称 -->
              <p style="font-size: 15px; color: #666; margin: 5px 0; display: flex; justify-content: space-between; align-items: center;">
                <span style="font-weight: bold;">{{ product.shop }}</span>
                <!-- 收藏按钮，右侧对齐 -->
                <el-rate
                    v-model="product.favorite"
                    :max="1"
                    icon-classes="el-icon-heart"
                    @change="toggleFavorite(product)"
                    @click.stop
                    style="color: #f39c12;">
                </el-rate>
              </p>


            </div>
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

    <el-dialog
        v-model="detailedProductVisible"
        title="查看商品详细信息"
        :width="'90%'"
        :style="{ height: '90%' }"
    >
      <div style="display: flex; align-items: flex-start;">
        <!-- 左侧放置商品图片和简要信息 -->
        <div style="flex: 1; padding: 20px; display: flex; flex-direction: column; justify-content: space-between;">
          <!-- 商品图片 -->
          <img :src="detailedProductInfo.img_url" alt="商品图片" style="width: 100%; height: auto; max-width: 300px;" />

          <!-- 商品简要信息 -->
          <div style="margin-top: 20px;">
            <p style="font-weight: bold; font-size: 18px;">
              <span v-if="detailedProductInfo.source.includes('jd')" style="color: #e74c3c;font-weight: bold;">京东 </span>
              <span v-else-if="detailedProductInfo.source.includes('tmall') || detailedProductInfo.source.includes('taobao')" style="color: #e74c3c;font-weight: bold;">淘宝 </span>
              {{ detailedProductInfo.title }}
            </p>
            <p style="font-size: 14px; color: #555;">
              <span style="font-weight: bold;">店铺：</span>{{ detailedProductInfo.shop }}
            </p>
            <p style="font-size: 14px; color: #555;">
              <span style="font-weight: bold;">价格：￥</span>{{ detailedProductInfo.price }}
            </p>
          </div>
        </div>

        <!-- 右侧显示标签页 -->
        <div style="flex: 2; padding: 20px; display: flex; flex-direction: column;">
          <!-- 动画节点 -->
          <div id="loader-wrapper"  v-show="loading_history_Visible" >
            <div id="loader"></div>
            <div class="load_title" >正在加载,请耐心等待<br><span>爬取该商品历史价格中...</span></div>
          </div>
          <!-- 标签页切换 -->
          <el-tabs v-model="activeTab" @tab-click="handleTabClick" style="margin-bottom: 20px;">
            <!-- 历史记录查询 Tab -->
            <el-tab-pane label="历史记录查询" name="history">
              <div>
                <img v-show="priceHistoryVisible" :src="history_img_src" alt="历史价格走向图" style="margin-top: 10px; max-width: 100%; height: auto;" />
              </div>

            </el-tab-pane>

            <!-- 相似产品列表 Tab -->
            <el-tab-pane label="相似产品列表" name="similar">
              <div style="margin-bottom: 20px;">
                <el-input v-model="searchQuery" placeholder="搜索商品" clearable style="width: 300px; margin-right: 10px;"></el-input>
                <el-select v-model="sortMethod" placeholder="选择排序方式" style="width: 200px; margin-right: 10px;">
                  <el-option label="价格升序" value="priceAsc"></el-option>
                  <el-option label="价格降序" value="priceDesc"></el-option>
                  <el-option label="标题排序" value="title"></el-option>
                </el-select>
                <el-button type="primary" @click="sortedProducts" style="width: 60px;">确定</el-button>
              </div>
              <div style="max-height: 500px; overflow-y: auto;">
                <el-row :gutter="20">
                  <el-col v-for="(product, index) in filteredProductsList" :key="index" :span="24" style="margin-bottom: 15px;">
                    <el-card>
                      <div style="display: flex; align-items: center;">
                        <img :src="product.img_url" alt="商品图片" style="width: 100px; height: 100px; object-fit: cover;" />
                        <div style="flex: 1; padding-left: 20px;">
                          <p style="font-weight: bold; font-size: 16px; color: #333;">
                            <span v-if="product.source.includes('jd')" style="color: #e74c3c;font-weight: bold;">京东</span>
                            <span v-else-if="product.source.includes('tmall') || product.source.includes('taobao')" style="color: #e74c3c;font-weight: bold;">淘宝</span>
                            {{ product.title }}
                          </p>
                          <p style="font-size: 14px; color: #555;">店铺：{{ product.shop }}</p>
                          <p style="font-size: 14px; color: #555;">价格：￥{{ product.price }}</p>
                        </div>
                        <a :href="product.source" target="_blank" style="color: #3498db; text-decoration: none;">
                          跳转到原网页
                        </a>
                      </div>
                    </el-card>
                  </el-col>
                </el-row>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </div>
    </el-dialog>





  </el-scrollbar>
</template>

<script>
import {Delete, Edit, Search, UploadFilled} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'
// import { VueSimpleSpinner } from 'vue-simple-spinner';
// import history from '@/assets/img/history.png';
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
    UploadFilled() {
      return UploadFilled
    },
    filteredProductsList() {
      return this.filter ? this.filteredProducts : this.similarProducts;
    }
  },
  data() {
    return {
      products: [], // 商品列表
      similarProducts: [], // 相似商品列表
      filteredProducts: [], // 过滤后的商品列表
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
      }
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
      ElMessage.success("正在执行搜索") // 显示消息提醒
        this.products = [] // 清空列表
        this.loadingVisible = true;
        this.$emit("search", ['search', this.keyword])
      // 创建一个 EventSource 对象，连接到后端的 /search 路径
      const eventSource = new EventSource(`http://localhost:8000/search?keyword=${encodeURIComponent(this.keyword)}`);
      //打印eventSource
      // 当接收到数据时触发 'message' 事件
      eventSource.onmessage = function(event) {
        this.loadingVisible = false;
        const product = JSON.parse(event.data);
        console.log(product);
        this.products.push(product);
      }.bind(this);
      ElMessage.success("正在打开连接") // 显示消息提醒
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
        eventSource.close(); // 关闭连接以防止意外行为
      };
    },
    Multi_condition_search(){
      this.priceHistoryVisible = false;
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
      console.log("QueryProducts called")
      axios.get('/home/product',
          { params: { // 请求体
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
    StoreSearchResults(){
      console.log("StoreSearchResults called")
      axios.post('/home/product/', {
        params: {
          user_name: this.$store.state.username,
          product: this.products
        }
      }).then(res => {
        ElMessage.success("保存搜索结果成功")
      }).catch(err => {
        ElMessage.error("保存搜索结果失败")
      })
    },
    // mounted() { // 当页面被渲染时
    //   this.QueryProducts() // 查询商品
    // },
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


</style>