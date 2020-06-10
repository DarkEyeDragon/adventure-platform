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
package net.kyori.adventure.platform.viaversion;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.impl.Handler;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.boss.BossColor;
import us.myles.ViaVersion.api.boss.BossStyle;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.platform.ViaPlatform;
import us.myles.ViaVersion.api.protocol.ProtocolRegistry;
import us.myles.ViaVersion.api.protocol.ProtocolVersion;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.protocols.protocol1_11to1_10.Protocol1_11To1_10;
import us.myles.ViaVersion.protocols.protocol1_16to1_15_2.ClientboundPackets1_16;
import us.myles.ViaVersion.protocols.protocol1_16to1_15_2.Protocol1_16To1_15_2;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.ClientboundPackets1_9_3;

public class ViaVersionHandlers {

  private ViaVersionHandlers() {}

  /**
   * A provider for the ViaVersion api objects
   *
   * @param <V> native player type
   */
  public interface ViaAPIProvider<V> {

    /**
     * Whether ViaVersion is available and should be used (generally true if the running game version is less than what is supported).
     *
     * @return if available
     */
    boolean isAvailable();

    ViaPlatform<? extends V> platform();

    default UserConnection connection(final V viewer) {
      final UUID viewerId = id(viewer);
      return viewerId == null ? null : platform().getConnectionManager().getConnectedClient(viewerId);
    }

    @Nullable UUID id(final V viewer);
  }

  /**
   * base class for handlers that send ViaVersion packet wrappers
   *
   * @param <V> user type
   */
  static abstract class ConnectionBased<V> implements Handler<V> {
    private final ProtocolVersion targetVersion;
    protected final ViaAPIProvider<? super V> via;

    /**
     * Create a new handler targeting the default version (1.16).
     *
     * @param via api provider
     */
    protected ConnectionBased(final ViaAPIProvider<? super V> via) {
      this(via, ProtocolVersion.v1_16);
    }

    /**
     * Create a new handler targeting the specific protocol version.
     *
     * <p>This handler will only be active for a user if the server version is
     * &lt; the target version, AND the client version is &ge; the target version</p>
     *
     * @param via api provider
     * @param targetVersion minimum client version to handle.
     */
    protected ConnectionBased(final ViaAPIProvider<? super V> via, ProtocolVersion targetVersion) {
      this.via = via;
      this.targetVersion = targetVersion;
    }

    @Override
    public boolean isAvailable() {
      if(!via.isAvailable()) return false;
      if(ProtocolRegistry.SERVER_PROTOCOL >= this.targetVersion.getId()) return false; // using the protocol of this version, only adapt for older servers

      try {
        Class.forName("us.myles.ViaVersion.protocols.protocol1_16to1_15_2.Protocol1_16To1_15_2"); // make sure we're on a new version
        return true;
      } catch(ClassNotFoundException e) {
        return false;
      }
    }

    @Override
    public boolean isAvailable(final @NonNull V viewer) {
      final ViaPlatform<?> platform = this.via.platform();
      if(platform == null) {
        return false;
      }
      final UUID viewerId = this.via.id(viewer);
      if(viewerId == null) {
        return false;
      }

      return platform.getApi().isInjected(viewerId) && platform.getApi().getPlayerVersion(viewerId) >= this.targetVersion.getId();
    }

    protected UserConnection connection(final V viewer) {
      return this.via.connection(viewer);
    }

    protected void send(final @NonNull PacketWrapper wrapper) {
      try {
        wrapper.send(Protocol1_16To1_15_2.class);
      } catch(Exception ignore) {
      }
    }
  }

  public static class Chat<V> extends ConnectionBased<V> implements Handler.Chat<V, String> {
    protected static final byte CHAT_TYPE_CHAT = 0;
    protected static final byte CHAT_TYPE_SYSTEM = 1;
    protected static final byte CHAT_TYPE_ACTIONBAR = 2;

    private final byte chatType;

    public Chat(final ViaAPIProvider<? super V> provider) {
      this(provider, CHAT_TYPE_SYSTEM);
    }

    protected Chat(final ViaAPIProvider<? super V> provider, byte chatType) {
      super(provider);
      this.chatType = chatType;
    }

    @Override
    public String initState(@NonNull final Component component) {
      return GsonComponentSerializer.INSTANCE.serialize(component);
    }

    @Override
    public void send(@NonNull final V target, @NonNull final String message) {
      final PacketWrapper wrapper = new PacketWrapper(ClientboundPackets1_16.CHAT_MESSAGE.ordinal(), null, connection(target));
      wrapper.write(Type.STRING, message);
      wrapper.write(Type.BYTE, this.chatType);
      wrapper.write(Type.UUID, NIL_UUID);
      send(wrapper);
    }
  }

  public static class ActionBar<V> extends Chat<V> implements Handler.ActionBar<V, String> {
    public ActionBar(final ViaAPIProvider<? super V> provider) {
      super(provider, CHAT_TYPE_ACTIONBAR);
    }
  }

  public static class Title<V> extends ConnectionBased<V> implements Handler.Title<V> {
    protected static final int ACTION_TITLE = 0;
    protected static final int ACTION_SUBTITLE = 1;
    protected static final int ACTION_ACTIONBAR = 2;
    protected static final int ACTION_TIMES = 3;
    protected static final int ACTION_CLEAR = 4;
    protected static final int ACTION_RESET = 5;

    public Title(final ViaAPIProvider<? super V> via) {
      super(via);
    }

    private PacketWrapper make(final @NonNull V viewer, final int action) {
      final PacketWrapper wrapper = new PacketWrapper(ClientboundPackets1_16.TITLE.ordinal(), null, connection(viewer));
      wrapper.write(Type.VAR_INT, action);
      return wrapper;
    }

    @Override
    public void send(final @NonNull V viewer, final net.kyori.adventure.title.@NonNull Title title) {
      final int fadeIn = Title.ticks(title.fadeInTime());
      final int stay = Title.ticks(title.stayTime());
      final int fadeOut = Title.ticks(title.fadeOutTime());
      if(fadeIn != -1 || stay != -1 || fadeOut != -1) {
        final PacketWrapper wrapper = make(viewer, ACTION_TIMES);
        wrapper.write(Type.INT, fadeIn);
        wrapper.write(Type.INT, stay);
        wrapper.write(Type.INT, fadeOut);
        send(wrapper);
      }

      if(!TextComponent.empty().equals(title.subtitle())) {
        final String subtitleJson = GsonComponentSerializer.INSTANCE.serialize(title.title());
        final PacketWrapper wrapper = make(viewer, ACTION_SUBTITLE);
        wrapper.write(Type.STRING, subtitleJson);
        send(wrapper);
      }

      if(!TextComponent.empty().equals(title.title())) {
        final String titleJson = GsonComponentSerializer.INSTANCE.serialize(title.title());
        final PacketWrapper wrapper = make(viewer, ACTION_TITLE);
        wrapper.write(Type.STRING, titleJson);
        send(wrapper);
      }

    }

    @Override
    public void clear(final @NonNull V viewer) {
      send(make(viewer, ACTION_CLEAR)); // no extra data
    }

    @Override
    public void reset(final @NonNull V viewer) {
      send(make(viewer, ACTION_RESET)); // no extra data
    }
  }

  public static final class BossBar<V> extends ConnectionBased<V> implements Handler.BossBar<V>, net.kyori.adventure.bossbar.BossBar.Listener {
    protected static int ACTION_ADD = 0; // (name: String, percent: float, color: varint, overlay: varint, flags: ubyte)
    protected static int ACTION_REMOVE = 1; // ()
    protected static int ACTION_PERCENT = 2; // (float)
    protected static int ACTION_NAME = 3; // (String)
    protected static int ACTION_STYLE = 4; // (color: varint, overlay: varint)
    protected static int ACTION_FLAGS = 5; // (thickenFog | dragonMusic | darkenSky): ubyte

    private static final byte FLAG_DARKEN_SCREEN = 1;
    private static final byte FLAG_BOSS_MUSIC = 1 << 1;
    private static final byte FLAG_CREATE_WORLD_FOG = 1 << 2;

    private final Map<net.kyori.adventure.bossbar.BossBar, Instance> bars = new ConcurrentHashMap<>();

    public BossBar(final ViaAPIProvider<? super V> via) {
      super(via);
    }


    @Override
    public void show(final @NonNull V viewer, final net.kyori.adventure.bossbar.@NonNull BossBar bar) {
      final Instance barInstance = this.bars.computeIfAbsent(bar, adventure -> {
        adventure.addListener(this);
        return new Instance();
      });
      if(barInstance.subscribedPlayers.add(this.via.id(viewer))) {
        final PacketWrapper addPkt = barInstance.make(connection(viewer), ACTION_ADD);
        addPkt.write(Type.STRING, GsonComponentSerializer.INSTANCE.serialize(bar.name()));
        addPkt.write(Type.FLOAT, bar.percent());
        addPkt.write(Type.VAR_INT, color(bar.color()).getId());
        addPkt.write(Type.VAR_INT, overlay(bar.overlay()).getId());
        addPkt.write(Type.BYTE, flagBits(bar.flags()));
        send(addPkt);
      }
    }

    @Override
    public void hide(final @NonNull V viewer, final net.kyori.adventure.bossbar.@NonNull BossBar bar) {
      final Instance barInstance = this.bars.computeIfPresent(bar, (adventure, instance) -> {
        instance.subscribedPlayers.remove(this.via.id(viewer));
        if(instance.subscribedPlayers.isEmpty()) {
          adventure.removeListener(this);
          return null;
        } else {
          return instance;
        }
      });

      if(barInstance != null) {
        send(barInstance.make(connection(viewer), ACTION_REMOVE));
      }
    }

    @Override
    public void bossBarNameChanged(final net.kyori.adventure.bossbar.@NonNull BossBar bar, final @NonNull Component oldName, final @NonNull Component newName) {
      final Instance instance = this.bars.get(bar);
      if(instance != null) {
        instance.sendToSubscribers(bar, ACTION_NAME, (pkt, adv) -> {
          pkt.write(Type.STRING, GsonComponentSerializer.INSTANCE.serialize(adv.name()));
        });
      }
    }

    @Override
    public void bossBarPercentChanged(final net.kyori.adventure.bossbar.@NonNull BossBar bar, final float oldPercent, final float newPercent) {
      final Instance instance = this.bars.get(bar);
      if(instance != null) {
        instance.sendToSubscribers(bar, ACTION_PERCENT, (pkt, adv) -> {
          pkt.write(Type.FLOAT, adv.percent());
        });
      }
    }

    @Override
    public void bossBarColorChanged(final net.kyori.adventure.bossbar.@NonNull BossBar bar, final net.kyori.adventure.bossbar.BossBar.@NonNull Color oldColor, final net.kyori.adventure.bossbar.BossBar.@NonNull Color newColor) {
      styleChanged(bar);
    }

    @Override
    public void bossBarOverlayChanged(final net.kyori.adventure.bossbar.@NonNull BossBar bar, final net.kyori.adventure.bossbar.BossBar.@NonNull Overlay oldOverlay, final net.kyori.adventure.bossbar.BossBar.@NonNull Overlay newOverlay) {
      styleChanged(bar);
    }

    private void styleChanged(final net.kyori.adventure.bossbar.@NonNull BossBar bar) {
      final Instance instance = this.bars.get(bar);
      if(instance != null) {
        instance.sendToSubscribers(bar, ACTION_STYLE, (pkt, adv) -> {
          pkt.write(Type.VAR_INT, color(adv.color()).getId());
          pkt.write(Type.VAR_INT, overlay(adv.overlay()).getId());
        });
      }
    }

    @Override
    public void bossBarFlagsChanged(final net.kyori.adventure.bossbar.@NonNull BossBar bar, final @NonNull Set<net.kyori.adventure.bossbar.BossBar.Flag> oldFlags, final @NonNull Set<net.kyori.adventure.bossbar.BossBar.Flag> newFlags) {
      final Instance instance = this.bars.get(bar);
      if(instance != null) {
        instance.sendToSubscribers(bar, ACTION_FLAGS, (pkt, adv) -> {
          pkt.write(Type.BYTE, flagBits(adv.flags()));
        });
      }
    }

    private static byte flagBits(final Set<net.kyori.adventure.bossbar.BossBar.Flag> flags) {
      byte ret = 0;
      if(flags.contains(net.kyori.adventure.bossbar.BossBar.Flag.DARKEN_SCREEN)) {
        ret |= FLAG_DARKEN_SCREEN;
      }
      if(flags.contains(net.kyori.adventure.bossbar.BossBar.Flag.PLAY_BOSS_MUSIC)) {
        ret |= FLAG_BOSS_MUSIC;
      }
      if(flags.contains(net.kyori.adventure.bossbar.BossBar.Flag.CREATE_WORLD_FOG)) {
        ret |= FLAG_CREATE_WORLD_FOG;
      }
      return ret;
    }

    private static BossColor color(final net.kyori.adventure.bossbar.BossBar.Color color) {
      switch(color) {
        case PINK: return BossColor.PINK;
        case BLUE: return BossColor.BLUE;
        case RED: return BossColor.RED;
        case GREEN: return BossColor.GREEN;
        case YELLOW: return BossColor.YELLOW;
        case WHITE: return BossColor.WHITE;
        case PURPLE: /* fall-through */
        default: return BossColor.PURPLE;
      }
    }

    private static BossStyle overlay(final net.kyori.adventure.bossbar.BossBar.Overlay overlay) {
      switch(overlay) {
        case NOTCHED_6: return BossStyle.SEGMENTED_6;
        case NOTCHED_10: return BossStyle.SEGMENTED_10;
        case NOTCHED_12: return BossStyle.SEGMENTED_12;
        case NOTCHED_20: return BossStyle.SEGMENTED_20;
        case PROGRESS: /* fall-through */
        default: return BossStyle.SOLID;
      }
    }

    /**
     * A single boss bar instance.
     */
    class Instance {
      final UUID barId = UUID.randomUUID();
      final Set<UUID> subscribedPlayers = ConcurrentHashMap.newKeySet();

      PacketWrapper make(final UserConnection user, final int action) {
        final PacketWrapper wrapper = new PacketWrapper(ClientboundPackets1_16.BOSSBAR.ordinal(), null, user);
        wrapper.write(Type.UUID, barId);
        wrapper.write(Type.VAR_INT, action);
        return wrapper;
      }

      void sendToSubscribers(final net.kyori.adventure.bossbar.BossBar adventure, final int action, final BiConsumer<PacketWrapper, net.kyori.adventure.bossbar.BossBar> populator) {
        for(UUID id : subscribedPlayers) {
          final UserConnection conn = ViaVersionHandlers.BossBar.this.via.platform().getConnectionManager().getConnectedClient(id);
          if(conn != null) {
            final PacketWrapper wrapper = make(conn, action);
            populator.accept(wrapper, adventure);
            send(wrapper);
          }
        }
      }
    }
  }


  /**
   * Not super vital, but does allow a 1.8 server to issue sound stops, and for categories to be respected on older servers.
   *
   * @param <V> player type
   */
  public static final class PlaySound<V> extends ConnectionBased<V> implements Handler.PlaySound<V> {
    private final Function<V, Pos> positionGetter;

    public PlaySound(final ViaAPIProvider<? super V> via, final Function<V, Pos> positionGetter) {
      super(via, ProtocolVersion.v1_11);
      this.positionGetter = positionGetter;
    }

    @Override
    public void play(@NonNull final V viewer, @NonNull final Sound sound) {
      final Pos position = this.positionGetter.apply(viewer);
      if(position != null) {
        play(viewer, sound, position.x, position.y, position.z);
      }
    }

    @Override
    public void play(@NonNull final V viewer, @NonNull final Sound sound, final double x, final double y, final double z) {
      final PacketWrapper playSound = new PacketWrapper(ClientboundPackets1_9_3.NAMED_SOUND.ordinal(), null, connection(viewer));

      playSound.write(Type.STRING, sound.name().asString());
      playSound.write(Type.VAR_INT, sound.source().ordinal()); // TODO: proper ids
      playSound.write(Type.INT, fixed(x));
      playSound.write(Type.INT, fixed(y));
      playSound.write(Type.INT, fixed(z));
      playSound.write(Type.FLOAT, sound.volume());
      playSound.write(Type.FLOAT, sound.pitch());

      send(playSound);
    }

    private static int fixed(final double value) {
      return (int) (value * 8.0D);
    }

    @Override
    public void stop(@NonNull final V viewer, @NonNull final SoundStop sound) {
      final PacketWrapper pkt = new PacketWrapper(ClientboundPackets1_9_3.PLUGIN_MESSAGE.ordinal(), null, connection(viewer));
      pkt.write(Type.STRING, "MC|StopSound");
      pkt.write(Type.STRING, name(sound.sound()));
      pkt.write(Type.STRING, source(sound.source()));

      send(pkt);
    }

    protected @NonNull String name(final @Nullable Key name) {
      return name == null ? "" : name.value();
    }

    protected @NonNull String source(final Sound.@Nullable Source source) {
      return source == null ? "" : Sound.Source.NAMES.name(source);
    }

    @Override
    protected void send(final @NonNull PacketWrapper wrapper) {
      try {
        wrapper.send(Protocol1_11To1_10.class);
      } catch(Exception ignore) {
      }
    }

    /**
     * A (x, y, z) position in the game world.
     */
    public static final class Pos {
      public static final Pos ZERO = new Pos(0, 0, 0);

      final double x;
      final double y;
      final double z;

      public Pos(final double x, final double y, final  double z) {
        this.x = x;
        this.y = y;
        this.z = z;
      }
    }
  }
}
