package com.ksyun.campus.client.util;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

import java.lang.reflect.Method;

/**
 * 接受一个 HttpClientConfig 对象作为参数，并返回一个已经配置好的 HttpClient 实例
 *  SocketConfig 和 ConnectionConfig 对象，用于配置 HttpClient 的 socket 和连接属性
 *  创建连接管理器（PoolingHttpClientConnectionManager），负责管理与目标主机的连接池
 *
 */

public class HttpClientUtil {
    private static HttpClient httpClient;
    public static HttpClient createHttpClient(HttpClientConfig config) {

        int socketSendBufferSizeHint = config.getSocketSendBufferSizeHint();
        int socketReceiveBufferSizeHint = config.getSocketReceiveBufferSizeHint();
        int buffersize = 0;
        if (socketSendBufferSizeHint > 0 || socketReceiveBufferSizeHint > 0) {
            buffersize = Math.max(socketSendBufferSizeHint, socketReceiveBufferSizeHint);
        }

        // 创建 SocketConfig 对象，设置整个请求-响应过程的超时时间，缓冲池的大小
        SocketConfig soConfig = SocketConfig.custom()
                .setTcpNoDelay(true).setSndBufSize(buffersize)
                .setSoTimeout(Timeout.ofMilliseconds(config.getSocketTimeOut()))
                .build();

        // 创建 ConnectionConfig 对象，设置与服务器建立连接的超时时间
        ConnectionConfig coConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(config.getConnectionTimeOut()))
                .build();

        // 构建的一个请求配置对象，设置连接超时时间和响应超时时间
        RequestConfig reConfig;
        RequestConfig.Builder builder= RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(config.getConnectionTimeOut()))
                .setResponseTimeout(Timeout.ofMilliseconds(config.getSocketTimeOut()))
                ;
        reConfig=builder.build();

        //连接管理器，设置最大连接数、SocketConfig、ConnectionConfig
        PlainConnectionSocketFactory sf = PlainConnectionSocketFactory.getSocketFactory();
        Registry<ConnectionSocketFactory> r = RegistryBuilder.<ConnectionSocketFactory> create().register("http", sf).build();
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(r);
        connectionManager.setMaxTotal(config.getMaxConnections());
        connectionManager.setDefaultMaxPerRoute(connectionManager.getMaxTotal());
        connectionManager.setDefaultConnectionConfig(coConfig);
        connectionManager.setDefaultSocketConfig(soConfig);


        // 设置连接管理器、设置重试策略、设置请求配置对象
        httpClient = HttpClients.custom().setConnectionManager(connectionManager)
                .setRetryStrategy(new DefaultHttpRequestRetryStrategy(config.getMaxRetry(), TimeValue.ZERO_MILLISECONDS))
                .setDefaultRequestConfig(reConfig)
                .build();

        // 返回构建的自定义 HttpClient 实例。
        return httpClient;

    }
}
