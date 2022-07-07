package com.xlkk.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class HtmlParseUtil {
    public static void main(String[] args) throws IOException {
        String url = "https://search.jd.com/Search?keyword=java";
        //解析网页jsoup实现,解析出来的就是js中的document对象
        Document document = Jsoup.parse(new URL(url), 30000);
        //所有在js中使用的方法这里都能用
        Element element = document.getElementById("J_goodsList");
        System.out.println(element.html());
    }

}
