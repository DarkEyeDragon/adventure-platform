/*
 * This file is part of adventure-platform, licensed under the MIT License.
 *
 * Copyright (c) 2018-2020 KyoriPowered
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

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.platform.AudienceInfo;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.renderer.ComponentRenderer;
import net.kyori.adventure.title.Title;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import static java.util.Objects.requireNonNull;
import static net.kyori.adventure.platform.impl.Handler.Titles.ticks;

public class HandledAudience<V> extends AbstractAudience {
  protected final V viewer;
  private final ComponentRenderer<AudienceInfo> renderer;
  private final Handler.@Nullable Chat<? super V, ?> chatHandler;
  private final Handler.@Nullable ActionBar<? super V, ?> actionBarHandler;
  private final Handler.Titles<? super V> titleHandler;
  private final Handler.BossBars<? super V> bossBarHandler;
  private final Handler.@Nullable PlaySound<? super V> soundHandler;
  private final Handler.@Nullable Books<? super V> bookHandler;

  public HandledAudience(
    final @NonNull V viewer,
    final @NonNull ComponentRenderer<AudienceInfo> renderer,
    final @Nullable HandlerCollection<? super V, ? extends Handler.Chat<? super V, ?>> chat,
    final @Nullable HandlerCollection<? super V, ? extends Handler.ActionBar<? super V, ?>> actionBar,
    final @Nullable HandlerCollection<? super V, ? extends Handler.Titles<? super V>> title,
    final @Nullable HandlerCollection<? super V, ? extends Handler.BossBars<? super V>> bossBar,
    final @Nullable HandlerCollection<? super V, ? extends Handler.PlaySound<? super V>> sound,
    final @Nullable HandlerCollection<? super V, ? extends Handler.Books<? super V>> books
  ) {
    this.viewer = requireNonNull(viewer, "viewer");
    this.renderer = requireNonNull(renderer, "renderer");
    this.chatHandler = handler(chat, viewer);
    this.actionBarHandler = handler(actionBar, viewer);
    this.titleHandler = handler(title, viewer);
    this.bossBarHandler = handler(bossBar, viewer);
    this.soundHandler = handler(sound, viewer);
    this.bookHandler = handler(books, viewer);
  }

  private static <V, H extends Handler<? super V>> H handler(final HandlerCollection<? super V, H> collection, final V viewer) {
    return collection != null ? collection.get(viewer) : null;
  }

  @Override
  public void sendMessage(final @NonNull Component message) {
    this.sendMessage0(this.chatHandler, requireNonNull(message, "message"));
  }

  private <S> void sendMessage0(final Handler.@Nullable Chat<? super V, S> handler, final @NonNull Component message) {
    if(handler != null) {
      handler.sendMessage(this.viewer, handler.initState(this.renderer.render(message, this)));
    }
  }

  @Override
  public void showBossBar(final @NonNull BossBar bar) {
    if(this.bossBarHandler != null) {
      this.bossBarHandler.showBossBar(this.viewer, requireNonNull(bar, "bar"));
    }
  }

  @Override
  public void hideBossBar(final @NonNull BossBar bar) {
    if(this.bossBarHandler != null) {
      this.bossBarHandler.hideBossBar(this.viewer, requireNonNull(bar, "bar"));
    }
  }

  @Override
  public void sendActionBar(final @NonNull Component message) {
    this.sendActionBar0(this.actionBarHandler, requireNonNull(message, "message"));
  }

  private <S> void sendActionBar0(final Handler.@Nullable ActionBar<? super V, S> handler, final @NonNull Component message) {
    if(handler != null) {
      handler.sendActionBar(this.viewer, handler.initState(this.renderer.render(message, this)));
    }
  }

  @Override
  public void playSound(final @NonNull Sound sound) {
    if(this.soundHandler != null) {
      this.soundHandler.playSound(this.viewer, requireNonNull(sound, "sound"));
    }
  }

  @Override
  public void playSound(final @NonNull Sound sound, final double x, final double y, final double z) {
    if(this.soundHandler != null) {
      this.soundHandler.playSound(this.viewer, requireNonNull(sound, "sound"), x, y, z);
    }
  }

  @Override
  public void stopSound(final @NonNull SoundStop stop) {
    if(this.soundHandler != null) {
      this.soundHandler.stopSound(this.viewer, requireNonNull(stop, "stop"));
    }
  }

  @Override
  public void openBook(final @NonNull Book book) {
    if(this.bookHandler != null) { // TODO: render pages, may want to move that lazy iterable into a util somewheree
      this.bookHandler.openBook(this.viewer, this.renderer.render(book.title(), this), this.renderer.render(book.author(), this), book.pages());
    }
  }

  @Override
  public void showTitle(final @NonNull Title title) {
    if(this.titleHandler != null) {
      requireNonNull(title, "title");
      this.titleHandler.showTitle(this.viewer, this.renderer.render(title.title(), this), this.renderer.render(title.subtitle(), this), ticks(title.fadeInTime()), ticks(title.stayTime()), ticks(title.fadeOutTime()));
    }
  }

  @Override
  public void clearTitle() {
    if(this.titleHandler != null) {
      this.titleHandler.clearTitle(this.viewer);
    }
  }

  @Override
  public void resetTitle() {
    if(this.titleHandler != null) {
      this.titleHandler.resetTitle(this.viewer);
    }
  }
}
