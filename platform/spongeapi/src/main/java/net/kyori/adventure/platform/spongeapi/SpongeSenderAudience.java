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
package net.kyori.adventure.platform.spongeapi;

import java.util.Locale;

import net.kyori.adventure.platform.AudienceInfo;
import net.kyori.adventure.platform.impl.HandledAudience;
import net.kyori.adventure.platform.impl.Handler;
import net.kyori.adventure.platform.impl.HandlerCollection;
import net.kyori.adventure.text.renderer.ComponentRenderer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.channel.MessageReceiver;

/* package */ class SpongeSenderAudience<V extends MessageReceiver> extends HandledAudience<V> {
  
  public SpongeSenderAudience(@NonNull final V viewer,
                              final @NonNull ComponentRenderer<AudienceInfo> renderer,
                              final @Nullable HandlerCollection<? super V, ? extends Handler.Chat<? super V, ?>> chat,
                              final @Nullable HandlerCollection<? super V, ? extends Handler.ActionBar<? super V, ?>> actionBar,
                              final @Nullable HandlerCollection<? super V, ? extends Handler.Titles<? super V>> title,
                              final @Nullable HandlerCollection<? super V, ? extends Handler.BossBars<? super V>> bossBar,
                              final @Nullable HandlerCollection<? super V, ? extends Handler.PlaySound<? super V>> sound,
                              final @Nullable HandlerCollection<? super V, ? extends Handler.Books<? super V>> books) {
    super(viewer, renderer, chat, actionBar, title, bossBar, sound, books);
  }

  @Override
  public @Nullable Locale getLocale() {
    return this.viewer instanceof CommandSource ? ((CommandSource) viewer).getLocale() : null;
  }

  @Override
  public boolean hasPermission(final @NonNull String permission) {
    return this.viewer instanceof Subject && ((Subject) this.viewer).hasPermission(permission);
  }

  @Override
  public boolean isConsole() {
    return this.viewer instanceof ConsoleSource;
  }
}
