package datadog.trace.instrumentation.axway;

import static datadog.trace.instrumentation.axway.HTTPPluginDecorator.DECORATE;
import static java.util.Collections.singletonMap;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import datadog.trace.agent.tooling.Instrumenter;
import datadog.trace.bootstrap.instrumentation.api.AgentScope;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class AxwayStateInstrumentation extends Instrumenter.Default {

  public AxwayStateInstrumentation() {
    super("axway-api");
  }

  @Override
  public ElementMatcher<? super TypeDescription> typeMatcher() {
    return named("com.vordel.circuit.net.State");
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
        named("tryTransaction").and(takesArguments(0)),
        AxwayStateInstrumentation.class.getName() + "$StateAdvice");
  }

  public static class StateAdvice {
    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void onExit(
        @Advice.Enter final AgentScope scope, @Advice.Thrown final Throwable throwable) {
      if (scope == null) {
        return;
      }
      final AgentSpan span = scope.span();
      DECORATE.onError(span, throwable);
      DECORATE.beforeFinish(span);
      scope.close();
      scope.span().finish();
    }
  }
}
