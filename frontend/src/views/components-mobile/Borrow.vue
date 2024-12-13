<template>
    <el-scrollbar height="100%" style="width: 100%;">

        <!-- 标题和搜索框 -->
        <div style="margin-top: 20px; margin-left: 40px; font-size: 2em; font-weight: bold;">
            借书记录查询
            <el-input v-model="toSearch" :prefix-icon="Search"
                style=" width: 15vw;min-width: 150px; margin-left: 30px; margin-right: 30px; float: right; ;"
                clearable />
        </div>

        <!-- 查询框 -->
        <div style="width:30%;margin:0 auto; padding-top:5vh;">

            <el-input v-model="this.toQuery" style="display:inline; " placeholder="输入借书证ID"></el-input>
            <el-button style="margin-left: 10px;" type="primary" @click="QueryBorrows">查询</el-button>

        </div>

        <!-- 结果表格 -->
        <el-table v-if="isShow" :data="fitlerTableData" height="600"
            :default-sort="{ prop: 'borrowTime', order: 'ascending' }" :table-layout="'auto'"
            style="width: 100%; margin-left: 50px; margin-top: 30px; margin-right: 50px; max-width: 80vw;">
            <el-table-column prop="cardID" label="借书证ID" />
            <el-table-column prop="productId" label="商品ID" sortable />
            <el-table-column prop="borrowTime" label="借出时间" sortable />
            <el-table-column prop="returnTime" label="归还时间" sortable />
          <!-- 添加还书按钮,fitlerTableData的类型为数组，通过row.cardID获取借书证ID -->
          <!--如何将row的值传给borrowProductInfo:可以通过borrowProductInfo.cardID=row.cardID获取借书证ID-->
            <el-table-column label="操作">
                <template #default="scope">
                    <el-button type="primary" size="small" @click="this.borrowProductInfo.cardID = scope.row.cardID, this.borrowProductInfo.productId = scope.row.productId, this.borrowProductInfo.unix_borrowTime = scope.row.unix_borrowTime, borrowProductVisible = true"
                     >还书</el-button>
                </template>
            </el-table-column>
        </el-table>

        <!-- 还书对话框 -->
        <el-dialog v-model="borrowProductVisible" title="还书" width="30%" align-center>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                确认归还商品？
            </div>
          <!-- 列出商品信息,包括借书卡号,姓名,借书时间,书名 -->
          <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                <p>借书卡号：{{borrowProductInfo.cardID}}</p>
                <p>商品ID：{{borrowProductInfo.productId}}</p>
            </div>
            <div style="margin-top: 20px; text-align: center;">
                <el-button type="primary" @click="ReturnProduct">确认</el-button>
                <el-button @click="borrowProductVisible = false">取消</el-button>
            </div>
        </el-dialog>
    </el-scrollbar>
</template>

<script>
import axios from 'axios';
import { Search } from '@element-plus/icons-vue'
import {ElMessage} from "element-plus";

export default {
    data() {
        return {
            isShow: false, // 结果表格展示状态
            tableData: [{ // 列表项
                cardID: "",
                productId: "",
                borrowTime: "",
                unix_borrowTime: 0,
                returnTime: "",
                unix_returnTime: 0
            }],
            toQuery: '', // 待查询内容(对某一借书证号进行查询)
            toSearch: '', // 待搜索内容(对查询到的结果进行搜索)
            Search,
            borrowProductVisible: false, // 还书对话框显示状态
            borrowProductInfo: { // 还书信息
                id: 0,
                cardID: 0,
                unix_borrowTime: 0,
            },
        }
    },
    computed: {
        fitlerTableData() { // 搜索规则
            return this.tableData.filter(
                (tuple) =>
                    (this.toSearch == '') || // 搜索框为空，即不搜索
                    tuple.productId == this.toSearch || // 商品号与搜索要求一致
                    tuple.borrowTime.toString().includes(this.toSearch) || // 借出时间包含搜索要求
                    tuple.returnTime.toString().includes(this.toSearch) // 归还时间包含搜索要求
            )
        }
    },
    methods: {
        // 还书操作：
        ReturnProduct() {
          console.log("row:")
            axios.post('/home/borrow/',
                {
                    cardID: this.borrowProductInfo.cardID,
                    productId: this.borrowProductInfo.productId,
                    borrowTime:this.borrowProductInfo.unix_borrowTime
                }) // 向/borrow发出DELETE请求，参数为cardID=row.cardID, productId=row.productId
               // 向/borrow发出DELETE请求，参数为cardID=row.cardID, productId=row.productId
                .then(response => {
                  ElMessage.success("商品归还成功") // 显示消息提醒
                  this.borrowProductVisible = false
                  this.QueryBorrows() // 重新查询商品以刷新页面
                })
                .catch(error=>{
                  ElMessage.error("商品归还失败,可能本书已经被归还了")
                  this.borrowProductVisible = false
                  this.QueryBorrows()
                })
        },

        async QueryBorrows() {
            this.tableData = [] // 清空列表
            let response = await axios.get('/home/borrow/', { params: { cardID: this.toQuery } }) // 向/borrow发出GET请求，参数为cardID=this.toQuery
            let borrows = response.data // 获取响应负载
            borrows.forEach(borrow => { // 对于每一个借书记录
                this.tableData.push(borrow) // 将它加入到列表项中
            });
            this.isShow = true // 显示结果列表
        }
    }
}
</script>