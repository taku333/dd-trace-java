package datadog.trace.instrumentation.axway;

import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.activateSpan;
import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.startSpan;
import static datadog.trace.instrumentation.axway.HTTPPluginDecorator.DECORATE;
import static java.util.Collections.singletonMap;
import static net.bytebuddy.matcher.ElementMatchers.named;

import com.google.auto.service.AutoService;
import datadog.trace.agent.tooling.Instrumenter;
import datadog.trace.bootstrap.instrumentation.api.AgentScope;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

@AutoService(Instrumenter.class)
public final class AxwayHTTPPluginInstrumentation extends Instrumenter.Default {

  public AxwayHTTPPluginInstrumentation() {
    super("axway-api");
  }

  @Override
  public ElementMatcher<? super TypeDescription> typeMatcher() {
    return named("com.vordel.dwe.http.HTTPPlugin");
  }

  @Override
  public String[] helperClassNames() {
    return new String[] {
      packageName + ".HTTPPluginDecorator",
    };
  }

  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    return singletonMap(
        named("invoke"), AxwayHTTPPluginInstrumentation.class.getName() + "$HTTPPluginAdvice");
  }

  public static class HTTPPluginAdvice {

    @Advice.OnMethodEnter
    public static AgentScope onEnter(
        @Advice.Argument(value = 0) final Object request, @Advice.This final Object thisObj) {
      final AgentSpan span = startSpan("axway.HTTPPlugin.invoke");
      DECORATE.afterStart(span);
      DECORATE.onRequest(span, request);
      try {
        Method m = request.getClass().getDeclaredMethod("getHeaders");
        m.setAccessible(true);
        Object headers = m.invoke(request);
      } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignore) {
      }
      return activateSpan(span);
    }
  }
}
