package dk.ceti.jdentifiers.jackson;

import com.fasterxml.jackson.core.json.PackageVersion;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class JdentifierModule extends SimpleModule {
  private static final long serialVersionUID = 1L;

  public JdentifierModule() {
    super(PackageVersion.VERSION);
    // Deserializer

  }
}
