package net.hytaledepot.templates.plugin.security;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

public final class SecurityDemoCommand extends AbstractCommand {
  private final SecurityPluginState state;
  private final SecurityDemoService demoService;
  private final AtomicLong heartbeatTicks;

  public SecurityDemoCommand(SecurityPluginState state, SecurityDemoService demoService, AtomicLong heartbeatTicks) {
    super("hdsecuritydemo", "Runs a demo action for the Security template.");
    setAllowsExtraArguments(true);
    this.state = state;
    this.demoService = demoService;
    this.heartbeatTicks = heartbeatTicks;
  }

  @Override
  protected CompletableFuture<Void> execute(CommandContext ctx) {
    state.incrementCommandRequests();
    String sender = String.valueOf(ctx.sender().getDisplayName());
    String action = parseAction(ctx.getInputString());

    String result = demoService.applyAction(state, sender, action, heartbeatTicks.get());
    ctx.sendMessage(Message.raw(result));
    return CompletableFuture.completedFuture(null);
  }

  private static String parseAction(String input) {
    String normalized = String.valueOf(input == null ? "" : input).trim();
    if (normalized.isEmpty()) {
      return "sample";
    }
    String[] parts = normalized.split("\\s+");
    String first = parts[0].toLowerCase();
    if (first.startsWith("/")) {
      first = first.substring(1);
    }
    if (parts.length > 1 && first.startsWith("hd")) {
      return parts[1].toLowerCase();
    }
    return first;
  }
}
