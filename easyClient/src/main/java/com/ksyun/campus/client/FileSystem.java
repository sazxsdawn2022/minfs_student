package com.ksyun.campus.client;

import com.ksyun.campus.client.util.HttpClientConfig;
import com.ksyun.campus.client.util.HttpClientUtil;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.protocol.BasicHttpContext;
import org.apache.hc.core5.http.protocol.HttpContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class FileSystem {
    private String fileSystem;
    private static HttpClient httpClient;



    protected String callRemote(String option, String url, byte[] data) throws Exception {
        httpClient = HttpClientUtil.createHttpClient(new HttpClientConfig());

        if ("post".equalsIgnoreCase(option)) {
            System.out.println("到了callRemote的post");
            // 创建 HTTP POST 请求

            HttpPost httpPost = new HttpPost(url);

            // 设置请求头，返回json数据
            httpPost.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());

            // 设置请求体
            ByteArrayEntity requestEntity = new ByteArrayEntity(data, ContentType.APPLICATION_OCTET_STREAM);
            httpPost.setEntity(requestEntity);

            // 执行POST请求
            HttpContext context = new BasicHttpContext();
            CloseableHttpResponse response = null;
            try {
                response = (CloseableHttpResponse) httpClient.execute(httpPost, context);
            } catch (IOException e) {
                System.out.println("callRemote中捕获到连接异常");

                throw new RuntimeException(e);
            }


            // 处理响应
            if(response != null) {
                int statusCode = response.getCode();
                InputStream content = response.getEntity().getContent();
                String responseBody = readResponseContent(content);
//            System.out.println("post responseBody = " + responseBody);
                return responseBody;
            }
//            return "response为null，什么也不返回";
            return null;

        } else if ("get".equalsIgnoreCase(option)) {
            // 发起 GET 请求
            HttpGet httpGet = new HttpGet(url);

            // 设置 GET 请求头 ，返回json数据
            httpGet.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());

            // 执行 GET 请求
            HttpContext getHttpContext = new BasicHttpContext();
            CloseableHttpResponse response = (CloseableHttpResponse) httpClient.execute(httpGet, getHttpContext);

            // 处理响应
            int statusCode = response.getCode();
            InputStream content = response.getEntity().getContent();
            String responseBody = readResponseContent(content);

            // 根据需要进行进一步处理...
            System.out.println("get responseBody = " + responseBody);
            return responseBody;
        }

        return null;
        // 根据需要进行进一步处理...
    }

    //把输入流转化为字符串
    private String readResponseContent(InputStream content) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(content));
        StringBuilder responseContent = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            responseContent.append(line);
        }
        reader.close();
        return responseContent.toString();
    }

}