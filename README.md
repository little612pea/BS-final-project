<div class="cover" style="page-break-after:always;font-family:方正公文仿宋;width:100%;height:100%;border:none;margin: 0 auto;text-align:center;">
    </br></br></br></br></br></br></br>
    <div style="width:60%;margin: 0 auto;height:0;padding-bottom:10%;">
        </br>
        <img src="https://gitee.com/nenhang/Document-Templates/raw/main/typora-markdown/mylatex/project-template/images/zju-name.svg" alt="校名" style="width:110%;"/>
    </div>
    </br></br></br></br></br>
    <p style="font-family:华文中宋;text-align:center;font-size:30pt;margin: 0 auto">本科实验报告 </p>
    </br></br></br></br></br></br></br></br></br>
    <table style="border:none;margin-left:-1%;text-align:center;width:80%;font-family:仿宋;font-size:16px;">
    <tbody style="font-family:方正公文仿宋;font-size:15pt;">
    <tbody style="font-family:方正公文仿宋;font-size:15pt;">
        <tr style="font-weight:normal;"> 
            <td style="width:20%;text-align:right;">课程名称</td>
            <td style="width:2%">：</td> 
            <td style="width:30%;font-weight:normal;border-bottom: 1px solid;text-align:center;font-family:华文仿宋">B/S 体系软件设计</td>     </tr>
        <tr style="font-weight:normal;"> 
            <td style="width:20%;text-align:right;">姓　　名</td>
            <td style="width:2%">：</td> 
            <td style="width:30%;font-weight:normal;border-bottom: 1px solid;text-align:center;font-family:华文仿宋"> 胡集源</td>     </tr>
        <tr style="font-weight:normal;"> 
            <td style="width:20%;text-align:right;">学   院</td>
            <td style="width:2%">：</td> 
            <td style="width:30%;font-weight:normal;border-bottom: 1px solid;text-align:center;font-family:华文仿宋">计算机科学与技术学院</td>     </tr>
        <tr style="font-weight:normal;"> 
            <td style="width:20%;text-align:right;">专   业</td>
            <td style="width:2%">：</td> 
            <td style="width:30%;font-weight:normal;border-bottom: 1px solid;text-align:center;font-family:华文仿宋">计算机科学与技术</td>     </tr>
        <tr style="font-weight:normal;"> 
            <td style="width:20%;text-align:right;">学   号</td>
            <td style="width:2%">：</td> 
            <td style="width:30%;font-weight:normal;border-bottom: 1px solid;text-align:center;font-family:华文仿宋">3220104116</td>     </tr>
        <tr style="font-weight:normal;"> 
            <td style="width:20%;text-align:right;">指导教师</td>
            <td style="width:2%">：</td> 
            <td style="width:30%;font-weight:normal;border-bottom: 1px solid;text-align:center;font-family:华文仿宋">胡晓军</td>     </tr>
    </tbody>              
    </table>
	</br></br></br></br>
	<p style="text-align:center;font-size:17pt;margin: 0 auto;font-family:华文仿宋">2024 年 7 月 11 日 </p>                       
	</br></br></br></br></br></br></br>
</div>



#    浙江大学实验报告

**$\ \ \ \ \ \ \ \ \ \ \ \ \ \  课程名称: \underline{\ \ \ \ \ \ \ \ \ \ \  \ \ \ \ B/S体系软件设计     \ \ \ \ \ \ \ \ \ \ \ \ \ \ \     } \hspace{1mm} \ \ \ \ \  实验类型: \underline{\ \\ \ \ \ \  \ \ \ \ \ \ \ \ \ \ \ \ \ 综合型\ \ \ \ \ \ \ \  \ \ \ \ \ \  \ \ \ \ \ \     } \hspace{1mm}$**   

**$ \ \ \ \ \ \ \ \ \ \ \ \ \ \  学生姓名: \underline{\ \ \ \ \ 胡集源 \ \ \ \ \     } \hspace{1mm} \ \ \ \ \  专业: \underline{\ \ \ \ \ 计算机科学与技术 \ \ \ \ \     } \hspace{1mm} \ \ \ \ \ 学号: \underline{\ \ \ \ \ \ \ \ \ \ 3220104116 \ \ \ \ \ \ \ \ \ \     } \hspace{1mm}$**        

**$ \ \ \ \ \ \ \ \ \ \ \ \ \ \  指导老师: \underline{\ \ \ \ \ \ \ \ \ \ 胡晓军 \ \ \ \ \ \ \ \ \ \     } \hspace{1mm}  实验日期: \underline{\ \ \ \ \ \ \ \ \ \ 2024\ \ \ \ \ \ \ \ \ \     } \hspace{1mm} 年 \hspace{1mm} \underline{\ \ \ \ \ \ \ \ \ \ 11\ \ \ \ \ \ \ \ \ \ } \hspace{1mm} 月 \hspace{1mm} \underline{\ \ \ \ \ \ \ \ \ \ 11\ \ \ \ \\ \ \ \ \  } \hspace{1mm} 日$**



## 功能要求

本次大作业要求任选Web开发技术实现一个商品价格比较的网站。

需要实现的基本功能和设计思路如下：

### 实现用户注册、登录功能，用户注册时需要填写必要的信息并验证，如用户名、密码要求在 6 字节以上，email 的格式验证，并保证用户名和 email 在系统中唯一，用户登录后可以进行以下操作。

初步设计的users表，包括唯一的user id，用户名，密码和邮箱等

```sql
create table `users` (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL
)engine=innodb charset=utf8mb4;
```

### 通过商品名称在主流电商平台上查询该商品实时价格

采用python提供的selenium实现爬虫，pyquery解析网页提取css/xml选择器访问元素

**浏览器驱动设置**：利用 `Selenium` 创建浏览器实例，并配置浏览器选项来隐藏自动化痕迹、禁用 GPU 加速等，确保稳定性和模拟正常用户行为。

**页面加载与登录**：脚本会访问京东主页，并提供一段暂停时间供用户手动扫码登录，以绕过登录验证。

**数据爬取与解析**：脚本通过 Selenium 控制浏览器模拟用户操作，加载商品搜索结果页，并使用 `WebDriverWait` 实现智能等待，确保页面加载完成后再提取数据。`PyQuery` 解析网页内容并提取所需的商品信息。

**反爬策略**：使用随机 `time.sleep()` 和页面加载的 `WebDriverWait` 以减少被网站识别为爬虫的风险。

#### 商品名称建议分词处理优化查询；

使用python提供的 `Jieba` 库进行分词，以便于进一步分析，最终数据会被存储为 JSON 格式或输出。

#### 查询多个结果的处理

查询到多个名称匹配的相同商品时，支持对商品按价格升/降序排序，按照来源过滤等功能

#### 很多平台需要平台用户登录验证后才可以进行查询

使用cookie缓存自动验证登录，保持登录状态，考虑设计用户用自己账户刷新cookie的入口（由于在设备上首次登录个人账户大部分需要手机验证码或扫码，所以采用弹出登录窗口由用户手动登录的形式）

### 支持至少 2 个以上平台查询价格进行比较（淘宝、京东等）。

### 建立商品库，将商品信息和商品价格保存在数据库中。商品信息包含名称、多级品类、规格、条码、图片等，方便后续查询。

数据库操作部分我计划在数据库系统原理课程的图书管理系统中实现的mysql图书数据库（各操作已通过正确性和并发访问控制单元测试验证）的基础上完成。初步设计的商品在数据库中的统一存储格式，包括唯一标识符productId，评论数（淘宝），商品名title，店铺名shop，成交量（京东），图片img_url，价格price，来源（0-淘宝，1-京东）等

```sql
create table product
(
    productId  int auto_increment primary key,
    comment    varchar(255) null,
    title      varchar(255) not null,
    shop       varchar(255) not null,
    deal       varchar(255) null,
    img_url    varchar(1000) null,
    price      double not null,
    source     varchar(1000) not null
)engine=innodb charset=utf8mb4;
```



### 提供商品查询界面能显示商品信息，把历史价格用图表形式显示（如价格走势图）。

初步实现了从第三方网站（慢慢买网）爬取商品历史价格记录，由于该网页采用canvas绘制价格走势图，只能采用直接截图的方式，后续考虑直接从淘宝源网站爬取

### 支持设置降价提醒，针对指定商品定时查询最新价格，如有降价发送提醒，可以通过邮件，App 推送等方式实现。

### 样式适配手机，开发手机 App 或能够在手机浏览器/微信等应用内置的浏览器中友好显示。

### 如开发手机端，可以用相机拍摄商品图片或扫码商品条码进行商品查询。

之前选修过微信小程序课程，可能采用小程序生态提供的拍照api接口实现，同时设计适用于小程序的ui

## 页面安排

### 用户注册与登录页面

- **注册页面**：用户填写注册信息，包括用户名、密码、电子邮件等。需要进行前端验证（如密码强度、邮箱格式）和后端验证（如用户名和邮箱的唯一性）。

![image-20241112140627599](C:\Users\23828\AppData\Roaming\Typora\typora-user-images\image-20241112140627599.png)

- **登录页面**：用户输入用户名和密码进行登录。

![image-20241112140602194](C:\Users\23828\AppData\Roaming\Typora\typora-user-images\image-20241112140602194.png)

### 商品查询，比较页面

- **查询输入**：用户输入商品名称，可以进行分词处理以优化查询结果。
- **查询结果展示**：展示从不同平台查询到的商品价格，处理多个查询结果的展示。
- **比较视图**：用户可以选择多个商品进行价格比较，显示不同平台的价格差异。

![image-20241111192936499](C:\Users\23828\AppData\Roaming\Typora\typora-user-images\image-20241111192936499.png)

![image-20241111192916026](C:\Users\23828\AppData\Roaming\Typora\typora-user-images\image-20241111192916026.png)

### 商品库管理/历史记录查询页面

- **商品列表**：展示数据库中的商品信息，包括名称、品类、规格、条码、图片等。
- **商品详情**：点击商品列表中的某个商品，可以查看详细信息和历史价格走势图。

### 我的个人空间-设置页面

- **提醒设置**：用户可以为特定商品设置降价提醒，选择提醒方式（邮件、App推送等）。

- **扫码查询**：在App中集成相机功能，允许用户通过拍摄商品图片或扫描条码来查询商品信息。
- **个人信息展示**：展示部分个人信息

![image-20241112142500599](C:\Users\23828\AppData\Roaming\Typora\typora-user-images\image-20241112142500599.png)

![image-20241112143230460](C:\Users\23828\AppData\Roaming\Typora\typora-user-images\image-20241112143230460.png)

提供个人信息（头像，密码）修改的界面

## 实现步骤和技术选型

1. **前端**：
   - 使用vue3框架构建基本页面。
   - 采用Bootstrap或Materialize等框架进行响应式设计。
   - 使用axios库进行前后端数据交互，提高用户体验。
2. **后端**：
   - 使用Java/Spring Boot、Python/Django或Node.js等框架来处理业务逻辑。
   - 实现用户注册、登录、商品查询、数据库操作等功能。
3. **数据库**：
   - 使用MySQL作为数据库，设计合适的数据表结构来存储用户信息、商品信息等。
   - 提供建库建表的SQL脚本文件。
4. **第三方API**：
   - 集成电商平台API（如淘宝、京东）进行商品价格查询。
   - 处理API请求和响应，提取所需数据。
5. **邮件/推送服务**：
   - 使用SMTP服务器或第三方邮件服务（如SendGrid）发送邮件提醒。
   - 使用推送服务（如Firebase）进行App推送。
6. **安全性**：
   - 密码加密存储（如使用bcrypt）。
   - 验证用户输入，防止SQL注入等安全问题。
7. **测试**：
   - 进行单元测试和集成测试，确保功能正确性。
8. **部署**：
   - 使用docker进行环境封装，将应用部署到服务器或云平台。

## 参考资料

[超详细python实现爬取淘宝商品信息(标题、销量、地区、店铺等)_爬取淘宝商品数据-CSDN博客](https://blog.csdn.net/weixin_48266589/article/details/135303310)

![image-20241002221120435](C:\Users\23828\AppData\Roaming\Typora\typora-user-images\image-20241002221120435.png)

