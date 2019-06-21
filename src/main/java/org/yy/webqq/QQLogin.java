package org.yy.webqq;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.CookieStore;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;

/**
 * QQ login omponent.
 * <p>
 * 1. 本实例用于模拟登录 LOL官方站点 ‘http://lol.qq.com/main.shtml’， 获取登录状态Cookie信息，用于借助登录形态获取相关信息。本程序仅限于学习。
 * 2. 登录流程：模拟唤起快捷登录组件 -> 核验参数 -> 识别验证码 -> 登录QQ账号 -> 登录目标站点
 * 3. LOL官方站点 appid为`21000501`, 若需要登录其他QQ账号体系的网站，可替换相应的appid及目标地址。
 * 4. 验证码识别提供回调函数实现。可调用三方打码平台 或 输出页面手动输入。
 * 5. 若用于批量登录，需注意QQ的安全验证等级及账号安全风控。否则容易被封号。
 * </p>
 * @author yy
 * @date 2019年6月19日
 */
public class QQLogin {
  
  private static final Pattern check_pattern = Pattern.compile("^ptui_checkVC\\((.+)\\)");
  private static final Pattern cb_pattern = Pattern.compile("^ptuiCB\\((.+)\\)");
  
  public static String login(String qq, String password, CaptchaCallback callback) throws Exception {
    HttpClientContext context = HttpClientContext.create();
    context.setCookieStore(new BasicCookieStore());

    Map<String, String> headers = new HashMap<String, String>();
    headers.put("Pragma", "no-cache");
    headers.put("X_FORWARDED_FOR", "127.0.0.1");
    headers.put("Referer", "http://lol.qq.com/main.shtml");
    headers.put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.101 Safari/537.36");
    
    // $1 xlogin
    String login_sig = xlogin(headers, context);
    
    // $2 check
    String[] checkValue = check(qq, password, login_sig, headers, context);

    String ptuiCB = "";
    // $3-1 no captcha login
    if ("0".equals(checkValue[0])) {
      ptuiCB = login(qq, password, checkValue, login_sig, headers, context);
    // $3-2 login with captcha
    } else {
      checkValue = getCaptcha(qq, checkValue, headers, context, callback);
      ptuiCB = login(qq, password, checkValue, login_sig, headers, context);
      if(ptuiCB.indexOf("您输入的验证码不正确，请重新输入。") >= 0) {
        throw new RuntimeException("Captcha error.");
      }
    }
    
    // $4 login game
    return loginGame(ptuiCB, headers, context);
  }

  /**
   * ptlogin2 xlogin
   */
  private static String xlogin(Map<String, String> headers, HttpClientContext context) {
    String url = "https://xui.ptlogin2.qq.com/cgi-bin/xlogin?proxy_url=http://game.qq.com/comm-htdocs/milo/proxy.html&appid=21000501&target=top&s_url=http%3A%2F%2Flol.qq.com%2Fweb201310%2Fpersonal.shtml%3Fid%3D4035139683%26area%3D27&daid=8,%20Strict-Transport-Security:%20max-age=31536000";
    try {
      headers.put("Referer", "ui.ptlogin2.qq.com");
      HttpUtils.get(url, headers, null, context);
      
      CookieStore cookieStore = context.getCookieStore();
      List<Cookie> cookies = cookieStore.getCookies();
      for (Cookie cookie : cookies) {
        if ("pt_login_sig".equals(cookie.getName())) {
          return cookie.getValue();
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Call ptlogin2 xlogin error.", e);
    }
    throw new RuntimeException("Call ptlogin2 xlogin error. Login sign is null.");
  }
  
  /**
   * check login
   */
  private static String[] check(String qq, String password, String login_sig, Map<String, String> headers, HttpClientContext context) throws Exception {
    CookieStore cookieStore = context.getCookieStore();
    cookieStore.addCookie(new BasicClientCookie("qqnum", qq));
    cookieStore.addCookie(new BasicClientCookie("qqpwd", password));

    String url = "http://check.ptlogin2.qq.com/check?pt_tea=1&uin=" + qq + "&appid=21000501&r=" + new Random().nextInt() + "&login_sig=" + login_sig;
    
    String check_result = HttpUtils.get(url, headers, null, context);
    Matcher check_matcher = check_pattern.matcher(check_result);
    String check_ma = "";
    if (check_matcher.find()) {
      check_ma = check_matcher.group(1);
    }
    check_ma = check_ma.replaceAll("'", "");
    check_ma = check_ma.replaceAll(" ", "");
    return check_ma.split(",");
  }
  
  /**
   * login qq
   */
  private static String login(String qq, String password, String[] checkValue, String login_sig, Map<String, String> headers, HttpClientContext context) throws Exception {
    String p = PasswordSign.sign(qq, password, checkValue[1]);
    String url = "https://ptlogin2.qq.com/login?u=" + qq + "&verifycode=" + checkValue[1] + "&pt_vcode_v1=0&pt_verifysession_v1=" + checkValue[3] + "&p=" + p
            + "&pt_randsalt=0&ptredirect=0&u1=http%3A%2F%2Flol.qq.com%2Fweb201310%2Fpersonal.shtml%3Fid%3D4035139683%26area%3D27&from_ui=1&aid=21000501&daid=8&login_sig=" + login_sig;
    
    headers.put("Host", "ptlogin2.qq.com");
    return HttpUtils.get(url, headers, null, context);
  }

  /**
   * captcha login qq
   */
  private static String[] getCaptcha(String qq, String[] checkValue, Map<String, String> headers, HttpClientContext context, CaptchaCallback callback) throws Exception {
    headers.put("Host", "captcha.qq.com");
    String url = "http://captcha.qq.com/getimage?uin=" + qq + "&aid=21000501&cap_cd=" + checkValue[1] + "&0." + new Random().nextInt();
    byte[] data = HttpUtils.getByte(url, headers, null, context);
    
    String verifysession = "";
    CookieStore cookieStore = context.getCookieStore();
    List<Cookie> cookies = cookieStore.getCookies();
    for (Cookie cookie : cookies) {
      if ("verifysession".equals(cookie.getName())) {
        verifysession = cookie.getValue();
        break;
      }
    }
    return new String[] {"0", callback.captcha(data), "", verifysession};
  }

  /**
   * login game
   */
  private static String loginGame(String ptuiCB, Map<String, String> headers, HttpClientContext context) throws Exception {
    Matcher cb_matcher = cb_pattern.matcher(ptuiCB);
    String cb_ma = "";
    if (cb_matcher.find()) {
      cb_ma = cb_matcher.group(1);
    }
    cb_ma = cb_ma.replaceAll("'", "");
    String[] cbValue = cb_ma.split(",");
    if ("0".equals(cbValue[0])) {
      headers.put("Host", "ptlogin4.game.qq.com");
      
      HttpUtils.get(cbValue[2], headers, null, context);

      StringBuilder buff = new StringBuilder();
      CookieStore cookieStore = context.getCookieStore();
      List<Cookie> cookies = cookieStore.getCookies();
      for (Cookie cookie : cookies) {
        buff.append(cookie.getName()).append("=").append(cookie.getValue()).append(";");
      }
      return buff.toString();
    }
    throw new RuntimeException("Login game error. Response is " + ptuiCB);
  }
  
}
