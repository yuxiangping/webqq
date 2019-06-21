package org.yy.webqq.test;

import org.yy.webqq.CaptchaCallback;
import org.yy.webqq.QQLogin;

public class LoginTest {

  public static void main(String[] args) {
    try {
      String cookie = QQLogin.login("qq", "password", new CaptchaCallback() {
        @Override
        public String captcha(byte[] data) {
          return null;
        }
      });
      System.out.println(cookie);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
