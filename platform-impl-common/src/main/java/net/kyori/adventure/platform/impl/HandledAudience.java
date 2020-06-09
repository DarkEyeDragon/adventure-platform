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
package net.kyori.adventure.platform.impl;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import static java.util.Objects.requireNonNull;

public class HandledAudience<V> implements Audience {
  protected final V viewer;
  private final Handler.@Nullable Chat<? super V, ?> chatHandler;
  private final Handler.@Nullable ActionBar<? super V, ?> actionBarHandler;
  private final Handler.@Nullable Title<? super V> titleHandler;
  private final Handler.@Nullable BossBar<? super V> bossBarHandler;
  private final Handler.@Nullable PlaySound<? super V> soundHandler;

  public HandledAudience(final @NonNull V viewer, final @Nullable HandlerCollection<? super V, ? extends Handler.Chat<? super V, ?>> chat, final @Nullable HandlerCollection<? super V, ? extends Handler.ActionBar<? super V, ?>> actionBar,
                            final @Nullable HandlerCollection<? super V, ? extends Handler.Title<? super V>> title, final @Nullable HandlerCollection<? super V, ? extends Handler.BossBar<? super V>> bossBar, final @Nullable HandlerCollection<? super V, ? extends Handler.PlaySound<? super V>> sound) {
    this.viewer = requireNonNull(viewer, "viewer");
    this.chatHandler = chat == null ? null : chat.get(this.viewer);
    this.actionBarHandler = actionBar == null ? null : actionBar.get(this.viewer);
    this.titleHandler = title == null ? null : title.get(this.viewer);
    this.bossBarHandler = bossBar == null ? null : bossBar.get(this.viewer);
    this.soundHandler = sound == null ? null : sound.get(this.viewer);
  }

  @Override
  public void sendMessage(final @NonNull Component message) {
    sendMessage0(this.chatHandler, requireNonNull(message, "message"));
  }

  private <S> void sendMessage0(final Handler.@Nullable Chat<? super V, S> handler, final @NonNull Component message) {
    if (handler != null) {
      handler.send(this.viewer, handler.initState(message));
    }
  }

  @Override
  public void showBossBar(final @NonNull BossBar bar) {
    if (this.bossBarHandler != null) {
      this.bossBarHandler.show(this.viewer, requireNonNull(bar, "bar"));
    }
  }

  @Override
  public void hideBossBar(final @NonNull BossBar bar) {
    if (this.bossBarHandler != null) {
      this.bossBarHandler.hide(this.viewer, requireNonNull(bar, "bar"));
    }
  }

  @Override
  public void sendActionBar(final @NonNull Component message) {
    sendActionBar0(this.actionBarHandler, requireNonNull(message, "message"));
  }

  private <S> void sendActionBar0(final Handler.@Nullable ActionBar<? super V, S> handler, final @NonNull Component message) {
    if (handler != null) {
      handler.send(this.viewer, handler.initState(message));
    }
  }

  @Override
  public void playSound(final @NonNull Sound sound) {
    if(this.soundHandler != null) {
      this.soundHandler.play(this.viewer, requireNonNull(sound, "sound"));
    }
  }

  @Override
  public void playSound(final @NonNull Sound sound, final double x, final double y, final double z) {
    if(this.soundHandler != null) {
      this.soundHandler.play(this.viewer, requireNonNull(sound, "sound"), x, y, z);
    }
  }

  @Override
  public void stopSound(final @NonNull SoundStop stop) {
    if(this.soundHandler != null) {
      this.soundHandler.stop(this.viewer, requireNonNull(stop, "stop"));
    }
  }

  @Override
  public void showTitle(final @NonNull Title title) {
    if(this.titleHandler != null) {
      this.titleHandler.send(this.viewer, requireNonNull(title, "title"));
    }
  }

  @Override
  public void clearTitle() {
    if(this.titleHandler != null) {
      this.titleHandler.clear(this.viewer);
    }
  }

  @Override
  public void resetTitle() {
    if(this.titleHandler != null) {
      this.titleHandler.reset(this.viewer);
    }
  }
}
