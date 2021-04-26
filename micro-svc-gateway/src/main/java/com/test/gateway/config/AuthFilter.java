package com.test.gateway.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;

/**
 * 自定义过滤器，向下游服务请求加header认证信息.
 * 与敏感头(设置向内部服务不传递哪些header正好相反)，
 * 这种方式好像不能传递名称为 Authorization,Cookie,Set-Cookie 的请求头，这三个传递不到下游服务，这三个由敏感头管理，只能传递token这种自定义的头
 */
public class AuthFilter extends ZuulFilter {

//	@Autowired(required=true)
//	private LoginCheckApi loginCheckApi;

    // 请求路径白名单，不校验登录，在application-url配置
    private static Set<String> urlSet;
    // 请求资源类型白名单，不校验登录，在application-url配置
    private static Set<String> fileSet;

    @Override
    public String filterType() {
        //pre型过滤器，路由到下级服务前执行
        return FilterConstants.PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        //优先级，数字越大，优先级越低
        return 0;
    }

    @Override
    public boolean shouldFilter() {
        //是否执行该过滤器，true代表需要过滤
        return true;
    }

    /**
     * 过滤逻辑
     * pre过滤器在route过滤前执行，RequestContext负责通信包含了请求等信息，debug发现，context.addZuulRequestHeader，
     * 但在RibbonRoutingFilter 这个向下游服务发起请求的路由过滤器，自定义的header没有添加上。
     * RibbonRoutingFilter是默认的过滤器，run方法可以看到,逻辑是从原来的RequestContext生产新的RibbonCommandContext发起请求
     *
     * @return
     * @throws ZuulException
     */
    @Override
    public Object run() {

        //Zull的Filter链间通过RequestContext传递通信，内部采用ThreadLocal 保存每个请求的信息，
        //包括请求路由、错误信息、HttpServletRequest、response等
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = this.getHttpServletRequest();

        // option请求，直接放行
        if (request.getMethod().equals(RequestMethod.OPTIONS.name())) {
            return null;
        }

//		// 判断需要放行的url或者静态资源文件
//		String url = request.getRequestURI();
//		String end  = "";
//		if(url.lastIndexOf("/") >= 0 ) {	// 判断需要放行的请求
//			end  = url.substring(url.lastIndexOf("/"));
//			if(urlSet.contains(end)) {
//				return null;
//			}
//		}
//		if(end.lastIndexOf(".") > 0) {	//判断需要放行的静态文件
//			end  = end.substring(end.lastIndexOf(".") + 1);
//			if(fileSet.contains(end)) {
//				return null;
//			}
//		}


        //String token = ctx.getRequest().getParameter("token");	//获取到 值
        String token = ctx.getRequest().getParameter("inputName");    //获取到 值
        //过滤该请求，不往下级服务去转发请求，到此结束
        if (StringUtils.isEmpty(token)) { // 会报跨域问题
            this.setCORS(ctx);
            ctx.setSendZuulResponse(false);
            ctx.setResponseStatusCode(200);
            Map<String, Object> result = Maps.newHashMap();
            result.put("code", 401);
            result.put("msg", "未登录");
            result.put("obj", "来自网关的消息：未获取到有效的Token");
            result.put("success", false);
            ctx.setResponseBody(JSONObject.toJSONString(result));
            ctx.getResponse().setContentType("text/html;charset=UTF-8");
            return null;
        }

        return null;
//		// 增加请求头
//		ctx.addZuulRequestHeader("Cookie", cookie);
//
//		// 调用统一认证接口，判断是否登录 && 判断是否有功能权限
//		// 优先校验cookie，，不通过则校验token //cookie从request里面拿
//		Object check = new Object();//loginCheckApi.checkPermission(token, this.getUrl(request));
//
//		if(check instanceof HashMap) {
//			HashMap<String, Object> result = (HashMap) check;
//			if(Boolean.parseBoolean(result.get("success").toString())) {
//				// 添加序列化之后的用户信息
//				// 白名单url的请求，不能获取到该信息
//				setReqParams(ctx, request, "userEntity", result.get("obj").toString());
//				return null;
//			}
//			this.setCORS(ctx);
//			ctx.setSendZuulResponse(false);
//			ctx.setResponseStatusCode(200);
//			// 权限校验接口异常
//			ctx.setResponseBody(JSONObject.toJSONString(check));
//			ctx.getResponse().setContentType("text/html;charset=UTF-8");
//			return null;
//		} else {
//			this.setCORS(ctx);
//			ctx.setSendZuulResponse(false);
//			ctx.setResponseStatusCode(200);
//			Map<String, Object> result = Maps.newHashMap();
//			result.put("code", 401);
//			result.put("msg", "无权限");
//			result.put("obj", "来自网关的消息：该用户无当前请求权限");
//			result.put("success", false);
//			ctx.setResponseBody(JSONObject.toJSONString(result));
//			ctx.getResponse().setContentType("text/html;charset=UTF-8");
//			return null;
//		}
    }

    private String getUrl(HttpServletRequest request) {
        // 获取到请求的相关数据  uri是斜杠开头
        String uri = request.getRequestURI().toLowerCase().replaceAll("//", "/");
        String method = request.getMethod().toLowerCase();
        return method.concat(uri);
    }


    private HttpServletRequest getHttpServletRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = attributes.getRequest();
            return request;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    public static void setReqParams(RequestContext ctx, HttpServletRequest request, String key, String value) {
        // 一定要get一下,下面这行代码才能取到值... [注1]
        request.getParameterMap();
        Map<String, List<String>> requestQueryParams = ctx.getRequestQueryParams();
        if (requestQueryParams == null) {
            requestQueryParams = new HashMap<>();
        }

        //将要新增的参数添加进去,被调用的微服务可以直接 去取,就想普通的一样,框架会直接注入进去
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add(value);
        requestQueryParams.put(key, arrayList);
        ctx.setRequestQueryParams(requestQueryParams);
    }


    private void setCORS(RequestContext ctx) {
        //处理跨域问题
        HttpServletRequest request = ctx.getRequest();
        HttpServletResponse response = ctx.getResponse();

        // 这些是对请求头的匹配，网上有很多解释
        response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin"));
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Methods", "GET, HEAD, POST, PUT, DELETE, OPTIONS, PATCH");
        response.setHeader("Access-Control-Allow-Headers", "authorization, content-type");
        response.setHeader("Access-Control-Expose-Headers", "X-forwared-port, X-forwarded-host");
        response.setHeader("Vary", "Origin,Access-Control-Request-Method,Access-Control-Request-Headers");
    }

//	@Value("${whitelist.urlset}")
//	public void setUtlSet(Set<String> urlSet) {
//		this.urlSet = urlSet;
//	}
//
//	@Value("${whitelist.fileset}")
//	public void setFileSet(Set<String> fileSet) {
//		this.fileSet = fileSet;
//	}
}
