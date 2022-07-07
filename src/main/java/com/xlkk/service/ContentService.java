package com.xlkk.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;


public interface ContentService {
    boolean parseContent(String keywords);
    List<Map<String,Object>> searchPage(String keyword, int pageNo, int pageSize) throws IOException;
}
