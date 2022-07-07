package com.xlkk.utils;

import com.xlkk.pojo.Content;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Component
public class HtmlParseUtil {
    public static void main(String[] args) throws IOException {
        HtmlParseUtil htmlParseUtil = new HtmlParseUtil();
        List<Content> contents = htmlParseUtil.parseJD("java");
        for (Content content : contents) {
            System.out.println(content);
        }
    }
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
            //source-data-lazy-img
            String img = el.getElementsByTag("img").eq(0).attr("data-lazy-img");
            String price = el.getElementsByClass("p-price").eq(0).text();
            String title = el.getElementsByClass("p-name").eq(0).text();
            contents.add(new Content(title,price,img));
        }
        return contents;
}
}
