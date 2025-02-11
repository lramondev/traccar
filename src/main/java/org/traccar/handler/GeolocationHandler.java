/*
 * Copyright 2015 - 2022 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.database.StatisticsManager;
import org.traccar.geolocation.GeolocationProvider;
import org.traccar.model.Position;
import org.traccar.session.cache.CacheManager;

@ChannelHandler.Sharable
public class GeolocationHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeolocationHandler.class);

    private final GeolocationProvider geolocationProvider;
    private final CacheManager cacheManager;
    private final StatisticsManager statisticsManager;
    private final boolean processInvalidPositions;
    private final boolean reuse;

    public GeolocationHandler(
            Config config, GeolocationProvider geolocationProvider, CacheManager cacheManager,
            StatisticsManager statisticsManager) {
        this.geolocationProvider = geolocationProvider;
        this.cacheManager = cacheManager;
        this.statisticsManager = statisticsManager;
        processInvalidPositions = config.getBoolean(Keys.GEOLOCATION_PROCESS_INVALID_POSITIONS);
        reuse = config.getBoolean(Keys.GEOLOCATION_REUSE);
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object message) {
        if (message instanceof Position) {
            final Position position = (Position) message;
            if ((processInvalidPositions && !position.getValido())
                    && position.getRede() != null) {
                if (reuse) {
                    Position lastPosition = cacheManager.getPosition(position.getRastreador_id());
                    if (lastPosition != null && position.getRede().equals(lastPosition.getRede())) {
                        updatePosition(
                                position, lastPosition.getLatitude(), lastPosition.getLongitude(),
                                lastPosition.getPrecisao());
                        ctx.fireChannelRead(position);
                        return;
                    }
                }

                if (statisticsManager != null) {
                    statisticsManager.registerGeolocationRequest();
                }

                geolocationProvider.getLocation(position.getRede(),
                        new GeolocationProvider.LocationProviderCallback() {
                    @Override
                    public void onSuccess(double latitude, double longitude, double accuracy) {
                        updatePosition(position, latitude, longitude, accuracy);
                        ctx.fireChannelRead(position);
                    }

                    @Override
                    public void onFailure(Throwable e) {
                        LOGGER.warn("Geolocation network error", e);
                        ctx.fireChannelRead(position);
                    }
                });
            } else {
                ctx.fireChannelRead(position);
            }
        } else {
            ctx.fireChannelRead(message);
        }
    }

    private void updatePosition(Position position, double latitude, double longitude, double accuracy) {
        position.set(Position.KEY_APPROXIMATE, true);
        position.setValido(true);
        position.setDatahora_calculada(position.getDatahora_rastreador());
        position.setLatitude(latitude);
        position.setLongitude(longitude);
        position.setPrecisao(accuracy);
        position.setAltitude(0);
        position.setVelocidade(0);
        position.setCurso(0);
    }

}
