# es-jd-demo
这是一个训练elasticsearch的小demo。
## 结果展示
![图片](https://user-images.githubusercontent.com/60306671/177946214-8c815177-bb85-46c7-b9e4-fd88439e2790.png)
可以看到，我们搜索的关键字为java，展示出来的结果也是与java相关的，同时title中的keyword也做了高亮的处理；

## 实现过程
### 数据爬取
使用Jsoup来爬取JD商城的html，并进行解析获取我们想要的数据，如：商品的img、title、price等等；
~~~java
public List<Content> parseJD(String keywords) throws IOException {
        String url = "https://search.jd.com/Search?keyword="+keywords;
        //解析网页jsoup实现,解析出来的就是js中的document对象
        Document document = Jsoup.parse(new URL(url), 30000);
        //所有在js中使用的方法这里都能用
        Element element = document.getElementById("J_goodsList");
//        System.out.println(element.html());
        //获取所有的li元素
        Elements elements = element.getElementsByTag("li");
        List<Content> contents = new ArrayList<>();

        for (Element el : elements) {
            //data-lazy-img
            String img = el.getElementsByTag("img").eq(0).attr("data-lazy-img");
            String price = el.getElementsByClass("p-price").eq(0).text();
            String title = el.getElementsByClass("p-name").eq(0).text();
            contents.add(new Content(title,price,img));
        }
        return contents;
}
~~~
具体一点就是通过Jsoup解析指定url，得到一个Document对象，这个Document对象类似于js中的Document。

我们可以通过Document的api来通过id来获取Element对象，我们只需要筛选出我们需要的element并转换为String，然后对其进行封装，然后返回就可以了。



### 数据添加

我们将爬取到的数据导入到es中。

~~~java
  @Override
    public boolean parseContent(String keywords) {
        List<Content> contents = null;
        try {
            contents = new HtmlParseUtil().parseJD(keywords);
            //把查询出来的数据放入es中
            BulkRequest bulkRequest = new BulkRequest();
            bulkRequest.timeout("2m");
            for (int i = 0; i < contents.size(); i++) {
                bulkRequest.add(new IndexRequest("jd_goods")
                        .source(JSON.toJSONString(contents.get(i)), XContentType.JSON)
                );
            }
            BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
            boolean hasFailures = bulkResponse.hasFailures();
            return !hasFailures;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
~~~

因为数据很多，所以我们使用批量导入，使用BulkRequest，由client发送请求，得到response。

### 高亮搜索

实现高亮搜索是先实现搜索后完成高亮：

#### 实现精确搜索：

~~~java
if(pageNo<=1){
            pageNo = 1;
        }
        //条件搜索
        SearchRequest searchRequest = new SearchRequest("jd_goods");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //分页
        searchSourceBuilder.from(pageNo);
        searchSourceBuilder.size(pageSize);
        //精准匹配关键字
        TermQueryBuilder query = QueryBuilders.termQuery("title", keyword);
        searchSourceBuilder.query(query);
~~~

通过`QueryBuilders.termQuery("title",keyword);`来匹配关键字，然后通过`searchSourceBuilder`的`query()`方法调用。

#### 实现高亮：

~~~java
//高亮搜索
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("title");
        //关闭多个高亮
//        highlightBuilder.requireFieldMatch(false);
        highlightBuilder.preTags("<span style='color:red'>");
        highlightBuilder.postTags("</span>");
        searchSourceBuilder.highlighter(highlightBuilder);
~~~

高亮是通过HighlightBuilder实现的，指定`title`为高亮字段，然后为其添加前缀和后缀（也就是添加html样式）来实现高亮。

#### 解析替换

接下来要做的事情就是获取高亮字段，并替换搜索结果中的title字段；

~~~java
				searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        SearchHits hits = searchResponse.getHits();
        ArrayList<Map<String, Object>> list = new ArrayList<>();
        for (SearchHit hit : hits) {
            //获取高亮字段
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            HighlightField title = highlightFields.get("title");
            Map<String, Object> source = hit.getSourceAsMap(); //原来的结果
            //解析高亮的字段
            if(title!=null){
                Text[] fragments = title.fragments();
                String n_title = "";
                //将原来的字段替换为我们高亮的字段
                for (Text fragment : fragments) {
                    n_title += fragment;
                }
                source.put("title",n_title);//替换原来的内容
            }
            list.add(source);
        }
~~~

client提交请求后获取response响应，可以通过response来获取击中目标对象SearchHits，我们遍历它，通过`hit.getHighlightFields()`获取高亮字段内容，当获取的这个不为空时，我们就可以将其替换为原来的内容，然后放到list中，最后返回。



### 前端

因为我们传过去的高亮字段内容是一段带样式的html，所以在前端需要使用v-html解析，这样才能最终完成高亮。
