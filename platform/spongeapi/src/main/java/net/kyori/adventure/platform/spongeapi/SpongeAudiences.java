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

import com.google.inject.ImplementedBy;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.AudienceInfo;
import net.kyori.adventure.platform.AudienceProvider;
import net.kyori.adventure.text.renderer.ComponentRenderer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.Game;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.channel.MessageReceiver;

/**
 * A provider of {@link Audience}s for the Sponge API.
 */
@ImplementedBy(SpongeAudienceProvider.class)
public interface SpongeAudiences extends AudienceProvider {
  /**
   * Creates a {@link SpongeAudiences} provider for the given plugin.
   *
   * <p>Prefer obtaining an instance using Guice.</p>
   *
   * @param container a plugin container
   * @param game a game
   * @param renderer a component renderer
   * @return an audience factory
   */
  static @NonNull SpongeAudiences create(final @NonNull PluginContainer container, final @NonNull Game game, final @Nullable ComponentRenderer<AudienceInfo> renderer) {
    return SpongeAudienceProvider.create(container, game, renderer);
  }

  /**
   * Gets an audience for an individual player.
   *
   * <p>If the player is not online, messages are silently dropped.</p>
   *
   * @param player a player
   * @return a player audience
   */
  @NonNull Audience player(final @NonNull Player player);

  /**
   * Gets an audience for a message receiver.
   *
   * @param source the source
   * @return an audience
   */
  @NonNull Audience audience(final @NonNull MessageReceiver source);
}
