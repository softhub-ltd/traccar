package org.traccar.handler.events;

import io.netty.channel.ChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.database.IdentityManager;
import org.traccar.helper.DistanceCalculator;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Position;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * An event handler which generates new event if movement is detected.
 * This handler  is specific for hives and the algorithm depends on the motion status,
 * speed and distance between positions.
 * If a device in the hive is moving, this means the hive is being stolen.
 *
 * @author Hemed Ali
 * <p>
 * 18-03-2020, Lynghaugen 18B
 */
@ChannelHandler.Sharable
public class HiveMovementEventHandler extends BaseEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(HiveMovementEventHandler.class);
    private static final String DEFAULT_CATEGORY = "default";
    private static final String ANIMAL_CATEGORY = "animal";
    private final double minPositionDistance;

    private final IdentityManager identityManager;

    public HiveMovementEventHandler(Config config, IdentityManager identityManager) {
        this.identityManager = identityManager;
        minPositionDistance = config.getDouble(Keys.EVENT_MIN_POSITION_DISTANCE, 2.5);
    }

    @Override
    protected Map<Event, Position> analyzePosition(Position position) {
        Device device = identityManager.getById(position.getDeviceId());
        //Ensure validity
        if (Objects.isNull(device)
                || !position.getValid()
                || !identityManager.isLatestPosition(position)) {
            return Collections.emptyMap();
        }
        if (position.getAttributes().containsKey(Position.KEY_MOTION)) {
            Position lastPosition = identityManager.getLastPosition(position.getDeviceId());
            // boolean itWasMoving = lastPosition != null && lastPosition.getBoolean(Position.KEY_MOTION);
            boolean isMoving = position.getBoolean(Position.KEY_MOTION);
            boolean isHive = device.getCategory() != null
                    && device.getCategory().equalsIgnoreCase(DEFAULT_CATEGORY);

            double positionDistance = BigDecimal.valueOf(getDistance(lastPosition, position))
                    .setScale(2, RoundingMode.HALF_EVEN)
                    .doubleValue();

            if (isHive && isMoving && positionDistance > minPositionDistance) {
                LOGGER.info("Detected movement of speed {}k and distance {}m on device {} with id {}",
                        position.getSpeed(),
                        positionDistance,
                        device.getName(),
                        device.getUniqueId()
                );
                Event event = new Event(Event.TYPE_HIVE_MOVEMENT, position.getDeviceId(), position.getId());
                return Collections.singletonMap(event, position);
            }
        }
        return Collections.emptyMap();
    }

    /**
     * Gets distance between two positions. The distance is in meters.
     */
    private static double getDistance(Position lastPosition, Position newPosition) {
        if (Objects.nonNull(newPosition)) {
            if (Objects.isNull(lastPosition)) {
                return DistanceCalculator.distance(0.0, 0.0,
                        newPosition.getLatitude(), newPosition.getLongitude());
            }
            return DistanceCalculator.distance(
                    lastPosition.getLatitude(), lastPosition.getLongitude(),
                    newPosition.getLatitude(), newPosition.getLongitude());
        }
        return 0.0;
    }
}
