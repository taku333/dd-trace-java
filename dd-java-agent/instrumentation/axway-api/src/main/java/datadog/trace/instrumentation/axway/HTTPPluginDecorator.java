package datadog.trace.instrumentation.axway;

import datadog.trace.bootstrap.instrumentation.decorator.HttpClientDecorator;
import java.net.URI;

public class HTTPPluginDecorator extends HttpClientDecorator<Object, Object> {
  public static final HTTPPluginDecorator DECORATE = new HTTPPluginDecorator();

  @Override
  protected String[] instrumentationNames() {
    return new String[] {"axway-api"};
  }

  @Override
  protected String component() {
    return "axway.HTTPPlugin";
  }

  @Override
  protected String method(final Object httpRequest) {
    return "GET"; // "httpRequest.getMethod() ??";
  }

  @Override
  protected URI url(final Object httpRequest) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected int status(final Object clientResponse) {
    return 0;
  }
}
