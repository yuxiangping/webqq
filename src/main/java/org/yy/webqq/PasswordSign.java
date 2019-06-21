package org.yy.webqq;

import java.io.FileReader;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.springframework.core.io.ClassPathResource;

public class PasswordSign {

  private static Invocable engine;
  
  public static String sign(String qq, String pwd, String code) throws Exception {
    if (engine == null) {
      ScriptEngineManager manager = new ScriptEngineManager();
      ScriptEngine scriptEngine = manager.getEngineByName("js");

      ClassPathResource resource = new ClassPathResource("crypt.js");
      scriptEngine.eval(new FileReader(resource.getFile()));
      
      engine = (Invocable) scriptEngine;
    }
    return (String) engine.invokeFunction("getP", qq, pwd, code);
  }
}
