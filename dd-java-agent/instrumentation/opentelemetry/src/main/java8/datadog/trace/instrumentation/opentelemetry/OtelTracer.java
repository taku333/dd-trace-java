package datadog.trace.instrumentation.opentelemetry;

import datadog.trace.bootstrap.instrumentation.api.AgentTracer;
import datadog.trace.bootstrap.instrumentation.api.Tags;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import java.util.concurrent.TimeUnit;

public final class OtelTracer implements Tracer {
  private final String tracerName;
  private final AgentTracer.TracerAPI tracer;
  private final TypeConverter converter;

  OtelTracer(
      final String tracerName, final AgentTracer.TracerAPI tracer, final TypeConverter converter) {
    this.tracerName = tracerName;
    this.tracer = tracer;
    this.converter = converter;
  }

  @Override
  public Span.Builder spanBuilder(final String spanName) {
    return new SpanBuilder(spanName);
  }

  private class SpanBuilder implements Span.Builder {
    private final AgentTracer.SpanBuilder delegate;
    private boolean parentSet = false;

    public SpanBuilder(final String spanName) {
      delegate = tracer.buildSpan(tracerName).withResourceName(spanName);
    }

    @Override
    public Span.Builder setParent(Context context) {
      parentSet = true;
      delegate.asChildOf(converter.toAgentSpanContext(Span.fromContext(context).getSpanContext()));
      return this;
    }

    @Override
    public Span.Builder setNoParent() {
      parentSet = true;
      delegate.asChildOf(null);
      delegate.ignoreActiveSpan();
      return this;
    }

    @Override
    public Span.Builder addLink(final SpanContext spanContext) {
      if (!parentSet) {
        delegate.asChildOf(converter.toAgentSpanContext(spanContext));
      }
      return this;
    }

    @Override
    public Span.Builder addLink(SpanContext spanContext, Attributes attributes) {
      if (!parentSet) {
        delegate.asChildOf(converter.toAgentSpanContext(spanContext));
      }
      return this;
    }

    @Override
    public Span.Builder setAttribute(final String key, final String value) {
      delegate.withTag(key, value);
      return this;
    }

    @Override
    public Span.Builder setAttribute(final String key, final long value) {
      delegate.withTag(key, value);
      return this;
    }

    @Override
    public Span.Builder setAttribute(final String key, final double value) {
      delegate.withTag(key, value);
      return this;
    }

    @Override
    public Span.Builder setAttribute(final String key, final boolean value) {
      delegate.withTag(key, value);
      return this;
    }

    @Override
    public <T> Span.Builder setAttribute(AttributeKey<T> key, T t) {
      delegate.withTag(key.getKey(), t);
      return this;
    }

    @Override
    public Span.Builder setSpanKind(final Span.Kind spanKind) {
      // TODO: update delegate.operationName
      delegate.withTag(Tags.SPAN_KIND, spanKind.name());
      return this;
    }

    @Override
    public Span.Builder setStartTimestamp(final long startTimestamp) {
      delegate.withStartTimestamp(TimeUnit.NANOSECONDS.toMicros(startTimestamp));
      return this;
    }

    @Override
    public Span startSpan() {
      return converter.toSpan(delegate.start());
    }
  }
}
