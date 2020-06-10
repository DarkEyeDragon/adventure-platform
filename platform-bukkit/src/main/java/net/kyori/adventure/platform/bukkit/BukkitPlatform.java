/*
 * This file is part of text-extras, licensed under the MIT License.
 *
 * Copyright (c) 2018 KyoriPowered
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package net.kyori.adventure.platform.bukkit;

import com.google.common.collect.ImmutableList;
import com.google.common.graph.MutableGraph;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.kyori.adventure.platform.impl.AdventurePlatformImpl;
import net.kyori.adventure.platform.impl.Handler;
import net.kyori.adventure.platform.impl.HandlerCollection;
import net.kyori.adventure.platform.impl.VersionedGsonComponentSerializer;
import net.kyori.adventure.platform.viaversion.ViaVersionHandlers;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import us.myles.ViaVersion.api.platform.ViaPlatform;

import static java.util.Objects.requireNonNull;

// TODO: implement Listener and use singletons
public final class BukkitPlatform extends AdventurePlatformImpl implements Listener {

  private static final String PLUGIN_VIAVERSION = "ViaVersion";

  private static final Plugin PLUGIN_SELF;

  // A derivative of the Gson serializer that will serialize text appropriately based on the server version
  /* package */ static final VersionedGsonComponentSerializer GSON_SERIALIZER;

  static {
    if(Crafty.enumValue(Material.class, "NETHERITE_PICKAXE", Integer.MAX_VALUE) != null) { // we are 1.16
      GSON_SERIALIZER = VersionedGsonComponentSerializer.MODERN;
    } else {
      GSON_SERIALIZER = VersionedGsonComponentSerializer.PRE_1_16;
    }
    PLUGIN_SELF = (Plugin) Proxy.newProxyInstance(BukkitPlatform.class.getClassLoader(), new Class<?>[] {Plugin.class}, (proxy, method, args) -> {
      switch(method.getName()) {
        case "isEnabled":
          return true;
        case "getServer":
          return Bukkit.getServer();
        case "equals":
          return proxy == args[0];
        default:
          return null;
      }
    });

    injectSoftdepend("ViaVersion");
  }

  /**
   * Add the provided plugin as a softdepend of ourselves.
   *
   * <p>This removes the PluginClassLoader warning added by Spigot without
   * requiring every user to add ViaVersion to their own plugin.yml.</p>
   *
   * <p>We do assume here that each copy of Adventure belongs to a JavaPlugin.
   * If that is not true, we will silently fail to inject.</p>
   *
   * <p>If we are a {@link JavaPlugin}, we attempt to log if any errors occur at a debug level.</p>
   *
   *
   * @param pluginName plugin to add
   */
  @SuppressWarnings("unchecked")
  private static void injectSoftdepend(String pluginName) { // begone, warnings!
    JavaPlugin plugin = null;
    try {
      plugin = JavaPlugin.getProvidingPlugin(BukkitPlatform.class);

      PluginDescriptionFile pdf = plugin.getDescription();
      if(pdf.getName().equals(pluginName)) return; // don't depend on ourselves?

      final Field softdepend = Crafty.field(pdf.getClass(), "softDepend");

      final List<String> dependencies = (List<String>) softdepend.get(pdf);
      if(!dependencies.contains(pluginName)) { // add to plugin.yml
        final List<String> newList = ImmutableList.<String>builder().addAll(dependencies).add(pluginName).build();
        softdepend.set(pdf, newList);
      }

      // add to dependency graph (added 14c9d275141 during MC 1.15).
      // even if this fails (on older versions), we'll still have added ourselves to the PluginDescriptionFile
      PluginManager manager = plugin.getServer().getPluginManager();
      final Field dependencyGraphField = Crafty.field(manager.getClass(), "dependencyGraph");
      final MutableGraph<String> graph = (MutableGraph<String>) dependencyGraphField.get(manager);
      graph.putEdge(pdf.getName(), pluginName);
    } catch(Throwable ignore) { // fail silently
      if(plugin != null) {
        final @Nullable Logger pluginLog = plugin.getLogger();
        if (pluginLog != null) {
          if(pluginLog.isLoggable(Level.FINEST)) {
            plugin.getLogger().log(Level.FINEST, "Unable to add " + pluginName + " as a soft depend during Adventure injection", ignore);
          }
        }
      }
    }
  }

  // Type handlers
  // TODO: re-add handlers into Bukkit audiences


  private final Server server;
  private final BukkitViaProvider viaProvider;
  private final HandlerCollection<? super CommandSender, ? extends Handler.Chat<? super CommandSender, ?>> chat;
  private final HandlerCollection<Player, Handler.ActionBar<Player, ?>> actionBar;
  private final HandlerCollection<Player, Handler.Title<Player>> title;
  private final HandlerCollection<Player, Handler.BossBar<Player>> bossBar;
  private final HandlerCollection<Player, Handler.PlaySound<Player>> playSound;

  public BukkitPlatform() {
    this(Bukkit.getServer());
  }

  public BukkitPlatform(final @NonNull Server server) {
    this.server = requireNonNull(server, "server");
    Crafty.registerEvent(PLUGIN_SELF, PlayerLoginEvent.class, EventPriority.LOWEST, false, this::onPlayerLogin);
    Crafty.registerEvent(PLUGIN_SELF, PlayerQuitEvent.class, EventPriority.MONITOR, false, this::onPlayerQuit);
    Crafty.registerEvent(PLUGIN_SELF, PluginEnableEvent.class, this::onPluginLoad);
    Crafty.registerEvent(PLUGIN_SELF, PluginDisableEvent.class, this::onPluginDisable);

    viaProvider = new BukkitViaProvider();
    chat = new HandlerCollection<>(
      new ViaVersionHandlers.Chat<>(this.viaProvider),
      new SpigotHandlers.Chat(),
      new CraftBukkitHandlers.Chat(),
      new BukkitHandlers.Chat());
    actionBar = new HandlerCollection<>(
      new ViaVersionHandlers.ActionBar<>(this.viaProvider),
      new SpigotHandlers.ActionBar(),
      new CraftBukkitHandlers.ActionBarModern(),
      new CraftBukkitHandlers.ActionBar1_8thru1_11());
    title = new HandlerCollection<>(
      new ViaVersionHandlers.Title<>(this.viaProvider),
      new PaperHandlers.Title(),
      new CraftBukkitHandlers.Title());
    bossBar = new HandlerCollection<>(
      new ViaVersionHandlers.BossBar<>(this.viaProvider),
      new BukkitHandlers.BossBar());
    playSound = new HandlerCollection<>(
      new BukkitHandlers.PlaySound_WithCategory(),
      new ViaVersionHandlers.PlaySound<>(this.viaProvider, player -> {
        final Location pos = player.getLocation();
        return new ViaVersionHandlers.PlaySound.Pos(pos.getX(), pos.getY(), pos.getZ());
      }),
      new BukkitHandlers.PlaySound_NoCategory());
  }

  public void onPlayerLogin(PlayerLoginEvent event) {
    this.add(new BukkitPlayerAudience(event.getPlayer(), chat, actionBar, title, bossBar, playSound));
  }

  public void onPlayerQuit(PlayerQuitEvent event) {
    this.remove(event.getPlayer().getUniqueId());
    BukkitHandlers.BossBar.handleQuit(event.getPlayer());
  }

  public void onPluginLoad(PluginEnableEvent event) {
    if(event.getPlugin().getName().equals(PLUGIN_VIAVERSION)) {
      this.viaProvider.platform(); // init
    }
  }

  public void onPluginDisable(PluginDisableEvent event) {
    if(event.getPlugin().getName().equals(PLUGIN_VIAVERSION)) {
      this.viaProvider.dirtyVia();
    }
  }

  /* package */ static class BukkitViaProvider implements ViaVersionHandlers.ViaAPIProvider<CommandSender> {

    private volatile ViaPlatform<Player> platform = null;

    @Override
    public boolean isAvailable() {
      try {
        final Class<?> apiKlass = Crafty.findClass("us.myles.ViaVersion.api.ViaAPI");
        if(apiKlass == null) {
          return false;
        }
        final Plugin owningPlugin = JavaPlugin.getProvidingPlugin(apiKlass);
        if(owningPlugin != null && owningPlugin.getName().equals(PLUGIN_VIAVERSION)) {
          return true;
        }
      } catch(Exception ignore) {
      }
      return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ViaPlatform<Player> platform() {
      ViaPlatform<Player> platform = this.platform;
      if(platform == null) {
        this.platform = platform = (ViaPlatform<Player>) Bukkit.getServer().getPluginManager().getPlugin(PLUGIN_VIAVERSION);
      }
      return platform;
    }

    @Override
    public UUID id(final CommandSender viewer) {
      if(!(viewer instanceof Player)) return null;

      return ((Player) viewer).getUniqueId();
    }

    /* package */ void dirtyVia() {
      this.platform = null;
    }
  }

}
