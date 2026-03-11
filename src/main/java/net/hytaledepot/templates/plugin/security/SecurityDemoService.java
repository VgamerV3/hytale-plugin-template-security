package net.hytaledepot.templates.plugin.security;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class SecurityDemoService {
  private final Map<String, AtomicLong> actionCounters = new ConcurrentHashMap<>();
  private final Map<String, String> lastActionBySender = new ConcurrentHashMap<>();
  private final Map<String, String> runtimeValues = new ConcurrentHashMap<>();
  private final Map<String, String> domainState = new ConcurrentHashMap<>();
  private final Map<String, AtomicLong> numericState = new ConcurrentHashMap<>();

  private volatile Path dataDirectory;

  public void initialize(Path dataDirectory) {
    this.dataDirectory = dataDirectory;
    runtimeValues.put("category", "Security");
    runtimeValues.put("defaultAction", "audit-demo");
    runtimeValues.put("initialized", "true");
  }

  public void onHeartbeat(long tick) {
    actionCounters.computeIfAbsent("heartbeat", key -> new AtomicLong()).incrementAndGet();
    if (tick % 120 == 0) {
      runtimeValues.put("lastHeartbeat", String.valueOf(tick));
    }
  }

  public void recordExternalEvent(String key) {
    actionCounters.computeIfAbsent(String.valueOf(key), item -> new AtomicLong()).incrementAndGet();
  }

  public String applyAction(SecurityPluginState state, String sender, String action, long heartbeatTicks) {
    String normalizedSender = String.valueOf(sender == null ? "unknown" : sender);
    String normalizedAction = normalizeAction(action);

    actionCounters.computeIfAbsent(normalizedAction, key -> new AtomicLong()).incrementAndGet();
    lastActionBySender.put(normalizedSender, normalizedAction);

    if ("toggle".equals(normalizedAction)) {
      boolean enabled = state.toggleDemoFlag();
      runtimeValues.put("demoFlag", String.valueOf(enabled));
      return "[Security] demoFlag=" + enabled + ", heartbeatTicks=" + heartbeatTicks;
    }

    if ("info".equals(normalizedAction)) {
      return "[Security] " + diagnostics();
    }

    String domainResult = handleDomainAction(normalizedSender, normalizedAction, heartbeatTicks);
    if (domainResult != null) {
      return "[Security] " + domainResult;
    }

    return "[Security] unknown action='" + normalizedAction + "' (try: info, toggle, sample, audit-demo, fail-demo, unlock-demo)";
  }

  public String describeLastAction(String sender) {
    return lastActionBySender.getOrDefault(String.valueOf(sender), "none");
  }

  public long operationCount() {
    long total = 0;
    for (AtomicLong value : actionCounters.values()) {
      total += value.get();
    }
    return total;
  }

  public String diagnostics() {
    String directory = dataDirectory == null ? "unset" : dataDirectory.toString();
    return "ops="
        + operationCount()
        + ", trackedActions="
        + actionCounters.size()
        + ", domainEntries="
        + domainState.size()
        + ", numericEntries="
        + numericState.size()
        + ", dataDirectory="
        + directory;
  }

  public void shutdown() {
    runtimeValues.put("initialized", "false");
  }

  private String handleDomainAction(String sender, String action, long heartbeatTicks) {
    if ("sample".equals(action) || "audit-demo".equals(action)) {
      long audits = incrementNumber("security:audits", 1);
      return "audit checkpoints=" + audits;
    }
    if ("fail-demo".equals(action)) {
      long fails = incrementNumber("security:fails:" + sender.toLowerCase(), 1);
      if (fails >= 5) {
        domainState.put("security:lock:" + sender.toLowerCase(), "true");
      }
      return "failedAttempts=" + fails + ", locked=" + domainState.getOrDefault("security:lock:" + sender.toLowerCase(), "false");
    }
    if ("unlock-demo".equals(action)) {
      setNumber("security:fails:" + sender.toLowerCase(), 0);
      domainState.remove("security:lock:" + sender.toLowerCase());
      return "account unlocked";
    }
    return null;
  }

  private long incrementNumber(String key, long delta) {
    return numericState.computeIfAbsent(key, item -> new AtomicLong()).addAndGet(delta);
  }

  private long number(String key) {
    return numericState.computeIfAbsent(key, item -> new AtomicLong()).get();
  }

  private void setNumber(String key, long value) {
    numericState.computeIfAbsent(key, item -> new AtomicLong()).set(value);
  }

  private static String normalizeAction(String action) {
    String normalized = String.valueOf(action == null ? "" : action).trim().toLowerCase();
    return normalized.isEmpty() ? "sample" : normalized;
  }
}
