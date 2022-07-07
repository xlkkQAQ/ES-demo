package com.xlkk.service.impl;

import com.alibaba.fastjson.JSON;
import com.xlkk.config.ESConfig;
import com.xlkk.pojo.Content;
import com.xlkk.service.ContentService;
import com.xlkk.utils.HtmlParseUtil;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

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

}
