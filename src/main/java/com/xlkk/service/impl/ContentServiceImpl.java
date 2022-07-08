package com.xlkk.service.impl;

import com.alibaba.fastjson.JSON;
import com.xlkk.config.ESConfig;
import com.xlkk.pojo.Content;
import com.xlkk.service.ContentService;
import com.xlkk.utils.HtmlParseUtil;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.common.TimeUtil;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author xlkk
 * @date 2020/7/7
 */
@Service
public class ContentServiceImpl implements ContentService {
    @Autowired
    @Qualifier("restHighLevelClient")
    RestHighLevelClient client;

    /**
     *
     * @param keywords
     * @return
     */
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

    @Override
    public List<Map<String,Object>> searchPage(String keyword,int pageNo,int pageSize) throws IOException {
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
        searchSourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));

        //执行

        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        SearchHits hits = searchResponse.getHits();
        ArrayList<Map<String, Object>> list = new ArrayList<>();
        for (SearchHit hit : hits) {
            Map<String, Object> source = hit.getSourceAsMap();
            list.add(source);
        }
        return list;
    }

    /**
     * 实现高亮搜索
     * @param  keyword
     * @param  pageNo
     * @param  pageSize
     * @return List<Map<String,Object>>
     * @throws IOException
     */
    @Override
    public List<Map<String,Object>> highLightSeach(String keyword,int pageNo,int pageSize) throws IOException {
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
        searchSourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));

        //高亮搜索
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("title");
        //关闭多个高亮
//        highlightBuilder.requireFieldMatch(false);
        highlightBuilder.preTags("<span style='color:red'>");
        highlightBuilder.postTags("</span>");
        searchSourceBuilder.highlighter(highlightBuilder);
        //执行
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
        return list;
    }


}
