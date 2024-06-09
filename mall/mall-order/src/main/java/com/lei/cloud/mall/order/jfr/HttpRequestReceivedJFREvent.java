package com.lei.cloud.mall.order.jfr;

import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.StackTrace;

import javax.servlet.ServletRequest;

/**
 * <p>
 * 接收http请求 jfr事件
 * </p>
 *
 * @author 伍磊
 */
// 事件名称
@Label("Http Request Received")
// 事件类别
@Category({"Http Request"})
// 不追踪堆栈
@StackTrace(false)
public class HttpRequestReceivedJFREvent extends Event {
    //请求的 traceId，来自于 sleuth
    private final String traceId;
    //请求的 spanId，来自于 sleuth
    private final String spanId;

    public HttpRequestReceivedJFREvent(ServletRequest servletRequest, String traceId, String spanId) {
        this.traceId = traceId;
        this.spanId = spanId;
    }



}
