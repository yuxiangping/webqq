package org.yy.webqq;

public interface CaptchaCallback {

  /**
   * Return captcha char value
   * @param data image bytes
   */
  String captcha(byte[] data);
  
}
