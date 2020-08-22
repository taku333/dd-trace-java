package datadog.trace.mlt;

import static datadog.trace.mlt.Invocation.INVOCATION_OFFSET;

import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation of the invocation caller detection. Works for all Java versions from 7
 * till the latest current one. The downside is a slight performance hit and the inability to report
 * the caller method name.
 */
@Slf4j
final class InvocationImplDefault implements Invocation.Impl {
  private static final class WhoCalledSM extends SecurityManager {
    @Override
    public Class<?>[] getClassContext() {
      return super.getClassContext();
    }
  }

  private static final WhoCalledSM ACCESSOR = new WhoCalledSM();

  @Override
  @NonNull
  public Invocation.Caller getCaller(int offset) {
    if (offset < 0) {
      throw new IllegalArgumentException();
    }

    offset += INVOCATION_OFFSET;

    Class<?>[] list = ACCESSOR.getClassContext();

    if (list.length <= offset) {
      return null;
    }

    return new Invocation.Caller(list[offset].getName(), null);
  }

  @Override
  @NonNull
  public List<Invocation.Caller> getCallers() {
    Class<?>[] list = ACCESSOR.getClassContext();
    int callerLength = Math.max(list.length - INVOCATION_OFFSET, 0);
    List<Invocation.Caller> callers = new ArrayList<>(callerLength);

    for (int i = INVOCATION_OFFSET; i < list.length; i++) {
      callers.add(new Invocation.Caller(list[i].getName(), null));
    }
    return callers;
  }
}