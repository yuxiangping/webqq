# webqq
* 模拟web qq登陆协议，模拟登录lol。

* 注意事项：
1. 本实例用于模拟登录 LOL官方站点 ‘http://lol.qq.com/main.shtml’， 获取登录状态Cookie信息，用于借助登录形态获取相关信息。本程序仅限于学习。
2. 登录流程：模拟唤起快捷登录组件 -> 核验参数 -> 识别验证码 -> 登录QQ账号 -> 登录目标站点
3. LOL官方站点 appid为`21000501`, 若需要登录其他QQ账号体系的网站，可替换相应的appid及目标地址。
4. 验证码识别提供回调函数实现。可调用三方打码平台 或 输出页面手动输入。
5. 若用于批量登录，需注意QQ的安全验证等级及账号安全风控。否则容易被封号。

代码使用
```java
public static void main(String[] args) {
  try {
    String cookie = QQLogin.login("qq", "password", new CaptchaCallback() {
      @Override
      public String captcha(byte[] data) {
        return null;  // data 为验证码图片流，可借助打码平台识别
      }
    });
    System.out.println(cookie);
  } catch (Exception e) {
    e.printStackTrace();
  }
}
```
