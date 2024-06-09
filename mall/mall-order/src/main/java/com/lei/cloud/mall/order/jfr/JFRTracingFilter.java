package com.lei.cloud.mall.order.jfr;

import brave.Tracer;
import brave.propagation.TraceContext;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * <p>
 * JFRTracingFilter
 * </p>
 *
 * @author 伍磊
 */
@Log4j2
public class JFRTracingFilter extends OncePerRequestFilter {

    private Tracer tracer;

    public JFRTracingFilter(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {
        HttpRequestJFREvent httpRequestJFREvent = null;
        try {
            // 从 sleuth 中获取 traceId 和 spanId
            TraceContext context = tracer.currentSpan().context();
            String traceId = context.traceIdString();
            String spanId = context.spanIdString();
            // 收到请求就创建 HttpRequestReceivedJFREvent 并直接提交
            HttpRequestReceivedJFREvent requestReceivedJFREvent = new HttpRequestReceivedJFREvent(httpServletRequest, traceId, spanId);
            requestReceivedJFREvent.commit();
            // 启动 jfr request事件
            httpRequestJFREvent = new HttpRequestJFREvent(httpServletRequest, traceId, spanId);
            httpRequestJFREvent.begin();

        } catch (Exception e) {
            log.error("JFRTracingFilter-doFilter failed: {}", e.getMessage(), e);
        }

        Throwable throwable = null;
        try {
            filterChain.doFilter(httpServletRequest, httpServletResponse);
        } catch (IOException | ServletException t) {
            throwable = t;
            throw t;
        } finally {
            try {
                //无论如何，都会提交 httpRequestJFREvent
                if (httpRequestJFREvent != null) {
                    httpRequestJFREvent.setResponseStatus(httpServletResponse, throwable);
                    httpRequestJFREvent.commit();
                }
            } catch (Exception e) {
                log.error("JFRTracingFilter-doFilter final failed: {}", e.getMessage(), e);
            }
        }
    }
}
