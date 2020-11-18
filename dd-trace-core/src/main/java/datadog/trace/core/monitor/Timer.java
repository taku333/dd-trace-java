package datadog.trace.core.monitor;

import static datadog.trace.core.monitor.Utils.mergeTags;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.datadoghq.sketch.ddsketch.DDSketch;
import com.timgroup.statsd.StatsDClient;

/**
 * A timer which records times in a histogram, and flushes stats from the histogram after a
 * configurable period of time.
 */
public class Timer extends Recording {

  private static final long THIRTY_SECONDS_AS_NANOS = SECONDS.toNanos(30);

  private static final String[] P_50 = new String[] {"stat:p50"};
  private static final String[] P_99 = new String[] {"stat:p99"};
  private static final String[] MAX = new String[] {"stat:max"};

  private final String name;
  private final StatsDClient statsd;
  private final DDSketch histogram;
  private final long flushAfterNanos;

  private final String[] p50Tags;
  private final String[] p99Tags;
  private final String[] maxTags;

  private long start;
  private long lastFlush = 0;

  Timer(
      final String name,
      DDSketch histogram,
      final String[] tags,
      final StatsDClient statsd,
      long flushAfterNanos) {
    this.name = name;
    this.statsd = statsd;
    this.flushAfterNanos = flushAfterNanos;
    this.histogram = histogram;
    this.p50Tags = mergeTags(P_50, tags);
    this.p99Tags = mergeTags(P_99, tags);
    this.maxTags = mergeTags(MAX, tags);
  }

  Timer(final String name, DDSketch histogram, final StatsDClient statsd, long flushAfterNanos) {
    this(name, histogram, null, statsd, flushAfterNanos);
  }

  @Override
  public Recording start() {
    start = System.nanoTime();
    return this;
  }

  @Override
  public void reset() {
    long now = System.nanoTime();
    record(now);
    start = now;
  }

  @Override
  public void stop() {
    record(System.nanoTime());
  }

  private void record(long now) {
    // if it's longer than 30s, we have bigger problems
    histogram.accept(Math.min(now - start, THIRTY_SECONDS_AS_NANOS));
    if (now - lastFlush > flushAfterNanos) {
      lastFlush = now;
      flush();
    }
  }

  @Override
  public void flush() {
    statsd.gauge(name, histogram.getValueAtQuantile(0.5), p50Tags);
    statsd.gauge(name, histogram.getValueAtQuantile(0.99), p99Tags);
    statsd.gauge(name, histogram.getMaxValue(), maxTags);
  }
}
