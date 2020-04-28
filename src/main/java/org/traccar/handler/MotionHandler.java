/*
 * Copyright 2017 - 2019 Anton Tananaev (anton@traccar.org)
 * Copyright 2017 Andrey Kunitsyn (andrey@traccar.org)
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
import org.traccar.BaseDataHandler;
import org.traccar.database.IdentityManager;
import org.traccar.helper.DistanceCalculator;
import org.traccar.model.Position;

@ChannelHandler.Sharable
public class MotionHandler extends BaseDataHandler {

    private double speedThreshold;
    private double minPositionDistance;
    private final IdentityManager identityManager;


    public MotionHandler(double speedThreshold, double minPositionDistance, IdentityManager identityManager) {
        this.speedThreshold = speedThreshold;
        this.identityManager = identityManager;
        this.minPositionDistance = minPositionDistance;
    }

    public MotionHandler(double speedThreshold, IdentityManager identityManager) {
        this.speedThreshold = speedThreshold;
        this.identityManager = identityManager;
        this.minPositionDistance = 1.0;
    }

    /**
     * Most GPS devices report speed errors. The device may show high speed, but it is not actually moving.
     * To tackle this, I have introduced position distance.
     * This means the device is moving if the its speed is above the threshold AND
     * its distance from the previous position is greater than the minimum position distance.
     * Hemed, 2020-05-28
     */

    @Override
    protected Position handlePosition(Position position) {
        if (!position.getAttributes().containsKey(Position.KEY_MOTION)) {
            Position lastPosition = null;
            if (identityManager != null) {
                lastPosition = identityManager.getLastPosition(position.getDeviceId());
            }
            double positionDistance = DistanceCalculator.calculateDistance(lastPosition, position);
            boolean isMoving = position.getSpeed() > speedThreshold
                    && positionDistance > minPositionDistance;
            position.set(Position.KEY_MOTION, isMoving);
        }
        return position;
    }

}
