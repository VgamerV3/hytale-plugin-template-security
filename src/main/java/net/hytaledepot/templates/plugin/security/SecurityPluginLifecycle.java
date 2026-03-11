package net.hytaledepot.templates.plugin.security;

public enum SecurityPluginLifecycle {
  NEW,
  PRELOADING,
  SETTING_UP,
  READY,
  RUNNING,
  STOPPING,
  STOPPED,
  FAILED
}
