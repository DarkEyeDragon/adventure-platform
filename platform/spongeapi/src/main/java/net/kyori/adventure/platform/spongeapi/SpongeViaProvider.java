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

import java.util.UUID;
import net.kyori.adventure.platform.viaversion.ViaAPIProvider;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.plugin.PluginManager;
import us.myles.ViaVersion.api.platform.ViaPlatform;
import us.myles.ViaVersion.api.protocol.ProtocolVersion;

/**
 * Sponge provider for ViaVersion API
 */
class SpongeViaProvider implements ViaAPIProvider<Object> { // too many interfaces :(

  private final PluginManager plugins;
  private volatile ViaPlatform<?> platform = null;

  SpongeViaProvider(final PluginManager plugins) {
    this.plugins = plugins;
  }

  @Override
  public boolean isAvailable() {
    return this.plugins.isLoaded("viaversion");
  }

  @Override
  public ViaPlatform<?> platform() {
    if(!this.isAvailable()) {
      return null;
    }
    ViaPlatform<?> platform = this.platform;
    if(platform == null) {
      final PluginContainer container = this.plugins.getPlugin("viaversion").orElse(null);
      if(container == null) return null;
      this.platform = platform = (ViaPlatform<?>) container.getInstance().orElse(null);
    }
    return platform;
  }

  @Override
  public @Nullable UUID id(final @NonNull Object viewer) {
    if(!(viewer instanceof Player)) return null;

    return ((Player) viewer).getUniqueId();
  }

  @Override
  public @NonNull GsonComponentSerializer serializer(final @NonNull Object viewer) {
    if(this.isAvailable()) {
      final UUID id = this.id(viewer);
      if(id != null) {
        return this.gsonSerializer(id);
      }
    }
    return SpongeAudienceProvider.LEGACY_GSON_SERIALIZER;
  }
  
  private @NonNull GsonComponentSerializer gsonSerializer(final UUID id) {
    if(this.platform().getApi().getPlayerVersion(id) >= ProtocolVersion.v1_16.getId()) {
      return SpongeAudienceProvider.MODERN_GSON_SERIALIZER;
    } else {
      return SpongeAudienceProvider.LEGACY_GSON_SERIALIZER;
    }
  }
}
