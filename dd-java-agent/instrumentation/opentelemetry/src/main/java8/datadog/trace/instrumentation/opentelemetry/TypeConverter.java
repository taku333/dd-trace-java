package datadog.trace.instrumentation.opentelemetry;

import datadog.trace.api.DDId;
import datadog.trace.bootstrap.ContextStore;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import datadog.trace.bootstrap.instrumentation.api.AgentTracer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;

// Centralized place to do conversions
public final class TypeConverter {
  private final ContextStore<SpanContext, AgentSpan.Context> spanContextStore;

  public TypeConverter(ContextStore<SpanContext, AgentSpan.Context> spanContextStore) {
    this.spanContextStore = spanContextStore;
  }

  // TODO maybe add caching to reduce new objects being created

  // static to allow direct access from context advice.
  public static AgentSpan toAgentSpan(final Span span) {
    if (span == null) {
      return null;
    }
    if (span instanceof OtelSpan) {
      return ((OtelSpan) span).getDelegate();
    }
    if (span.getSpanContext().isValid()) {}

    return AgentTracer.NoopAgentSpan.INSTANCE;
  }

  public Span toSpan(final AgentSpan agentSpan) {
    if (agentSpan == null) {
      return null;
    }
    return new OtelSpan(agentSpan, this);
  }

  public SpanContext toSpanContext(final AgentSpan.Context context) {
    if (context == null) {
      return null;
    }
    SpanContext spanContext;
    if (context.isRemote()) {
      spanContext =
          SpanContext.createFromRemoteParent(
              padHexId(context.getTraceId(), 32),
              padHexId(context.getSpanId(), 16),
              TraceFlags.getSampled(),
              TraceState.getDefault());
    } else {
      spanContext =
          SpanContext.create(
              padHexId(context.getTraceId(), 32),
              padHexId(context.getSpanId(), 16),
              TraceFlags.getSampled(),
              TraceState.getDefault());
    }
    spanContextStore.put(spanContext, context);
    return spanContext;
  }

  public AgentSpan.Context toAgentSpanContext(final SpanContext spanContext) {
    // Currently assuming the span context was created above so it should contain an existing
    // context. If the SpanContext was created elsewhere then the result will be null.
    return spanContextStore.get(spanContext);
    // TODO: if null, attempt to convert.
  }

  private static final ThreadLocal<StringBuilder> STRING_BUILDER =
      new ThreadLocal<StringBuilder>() {
        @Override
        protected StringBuilder initialValue() {
          return new StringBuilder(32);
        }
      };

  private String padHexId(DDId id, int requiredLength) {
    StringBuilder builder = STRING_BUILDER.get();
    builder.setLength(0); // reset the builder
    String hexString = id.toHexString();
    int padding = requiredLength - hexString.length();
    for (int i = 0; i < padding; i++) {
      builder.append('0');
    }
    builder.append(hexString);
    assert builder.length() == requiredLength;
    return builder.toString();
  }
}
