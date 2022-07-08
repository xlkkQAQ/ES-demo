这是一个训练elasticsearch的小demo。
使用Jsoup来爬取JD商城的html，并进行解析获取我们想要的数据，如：商品的img、title、price等等；
将这些数据添加到elasticsearch中，以便我们之后进行搜索；

接下来就是核心功能搜索了，我们通过关键字，如java，来从elasticsearch中搜索出title与java相关的商品的数据，然后进行返回。
前端通过通过表单获取keyword，然后axios发送请求获取通过keyword查询到的相关数据，并将这些数据进行双向绑定，渲染到页面中。