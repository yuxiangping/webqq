package org.yy.webqq;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StreamUtils;

public final class HttpUtils {

  private static final Logger logger = LoggerFactory.getLogger(HttpUtils.class);
  
  private static RequestConfig config = RequestConfig.custom()
      .setConnectTimeout(2000)
      .setConnectionRequestTimeout(10000)
      .setSocketTimeout(5000)
      .setCookieSpec(CookieSpecs.STANDARD)
      .build();
  
  private static HttpClient client = HttpClientBuilder.create()
      .setMaxConnTotal(20)
      .setMaxConnPerRoute(5)
      .disableAutomaticRetries()
      .disableRedirectHandling()
      .setDefaultRequestConfig(config)
      .build();
  
  
  public static String get(String url, Map<String, String> headers, Map<String, Object> params, HttpClientContext context) throws Exception {
    byte[] bytes = getByte(url, headers, params, context);
    if (bytes == null) {
      return null;
    }
    return new String(bytes, Consts.UTF_8);
  }
  
  public static byte[] getByte(String url, Map<String, String> headers, Map<String, Object> params, HttpClientContext context) throws Exception {
    HttpRequestBase m = null;
    InputStream is = null;
    try {
      List<NameValuePair> listParam = new ArrayList<NameValuePair>();
      if (!MapUtils.isEmpty(params)) {
        for (Entry<String, Object> entry : params.entrySet()) {
          listParam.add(new BasicNameValuePair(String.valueOf(entry.getKey()), String.valueOf(entry.getValue())));
        }
      }

      m = Method.buildMethod(Method.get, url, listParam);
      if (!MapUtils.isEmpty(headers)) {
        for (Entry<String, String> entry : headers.entrySet()) {
          m.addHeader(entry.getKey(), entry.getValue());
        }
      }
      
      HttpResponse response = client.execute(m, context);
      HttpEntity resEntity = response.getEntity();
      if (response != null && response.getStatusLine().getStatusCode() == HttpStatus.SC_OK && resEntity != null) {
        is = resEntity.getContent();
        if (resEntity.getContentEncoding() != null && resEntity.getContentEncoding().getValue().contains("gzip")) {
          is = new GZIPInputStream(is);
        }
        return StreamUtils.copyToByteArray(is);
      }
    } catch (Exception ex) {
      logger.error("Http get [{}] error. Cause:", url, ex);
    } finally {
      try {
        if (is != null) {
          is.close();
          is = null;
        }
      } catch (Exception ex) {
        // ignore exception
      }

      try {
        if (m != null) {
          m.releaseConnection();
          m = null;
        }
      } catch (Exception ex) {
        // ignore exception
      }
    }
    return null;
  }
  
  public static String post(String url, Map<String, String> headers, Map<String, Object> params, String reqEntity, HttpClientContext context) {
    HttpRequestBase m = null;
    InputStream is = null;
    HttpEntity resEntity = null;
    try {
      List<NameValuePair> listParam = new ArrayList<NameValuePair>();
      if (!MapUtils.isEmpty(params)) {
        for (Entry<String, Object> entry : params.entrySet()) {
          listParam.add(new BasicNameValuePair(String.valueOf(entry.getKey()), String.valueOf(entry.getValue())));
        }
      }

      m = Method.buildMethod(Method.post, url, listParam);
      if (!MapUtils.isEmpty(headers)) {
        for (Entry<String, String> entry : headers.entrySet()) {
          m.addHeader(entry.getKey(), entry.getValue());
        }
      }

      if (reqEntity != null && reqEntity.length() > 0 && (m instanceof HttpPut || m instanceof HttpPost)) {
        ((HttpEntityEnclosingRequestBase) m).setEntity(new StringEntity(reqEntity.toString(), Consts.UTF_8));
      }

      HttpResponse response = client.execute(m, context);
      if (response != null && response.getStatusLine().getStatusCode() == HttpStatus.SC_OK && (resEntity = response.getEntity()) != null) {
        is = resEntity.getContent();
        if (resEntity.getContentEncoding() != null && resEntity.getContentEncoding().getValue().contains("gzip")) {
          is = new GZIPInputStream(is);
        }
      }
      return new String(StreamUtils.copyToByteArray(is), Consts.UTF_8);
    } catch (Exception ex) {
      logger.error("Http post [{}] error. Cause:", url, ex);
    } finally {
      try {
        if (is != null) {
          is.close();
          is = null;
        }
      } catch (Exception ex) {
        // ignore exception
      }

      try {
        m.releaseConnection();
        m = null;
      } catch (Exception ex) {
        // ignore exception
      }
    }
    return null;
  }
  
  /**
   * Http method
   * @author yy
   * @date 2019年6月21日
   */
  public enum Method {
    get {
      @Override
      public HttpRequestBase create(String url, List<NameValuePair> params) throws Exception {
        if(!CollectionUtils.isEmpty(params)) {
          if(url.indexOf("?") != -1) {
            url += "&" + URLEncodedUtils.format(params, Charset.forName("UTF-8"));
          } else {
            url += "?" + URLEncodedUtils.format(params, Charset.forName("UTF-8"));
          }
        }
        return new HttpGet(url);
      }
    },
    post {
      @Override
      public HttpRequestBase create(String url, List<NameValuePair> params) throws Exception {
        HttpPost post = new HttpPost(url);
        if(!CollectionUtils.isEmpty(params)) {
          post.setEntity(new UrlEncodedFormEntity(params, Charset.forName("UTF-8")));
        }
        return post;
      }
    },
    put {
      @Override
      public HttpRequestBase create(String url, List<NameValuePair> params) throws Exception {
        HttpPut put = new HttpPut(url);
        if(!CollectionUtils.isEmpty(params)) {
          put.setEntity(new UrlEncodedFormEntity(params, Charset.forName("UTF-8")));
        }
        return put;
      }
    },
    delete {
      @Override
      public HttpRequestBase create(String url, List<NameValuePair> params) throws Exception {
        if(!CollectionUtils.isEmpty(params)) {
          if(url.indexOf("?") != -1) {
            url += "&" + URLEncodedUtils.format(params, Charset.forName("UTF-8"));
          } else {
            url += "?" + URLEncodedUtils.format(params, Charset.forName("UTF-8"));
          }
        }
        return new HttpDelete(url);
      }
    };
    /**
     * create http method
     */
    abstract HttpRequestBase create(String url, List<NameValuePair> params) throws Exception;

    /**
     * bulid method
     */
    public static HttpRequestBase buildMethod(Method method, String url, List<NameValuePair> params) throws Exception {
      if (method == null) {
        return Method.get.create(url, params);
      }
      return method.create(url, params);
    }
  }

}
