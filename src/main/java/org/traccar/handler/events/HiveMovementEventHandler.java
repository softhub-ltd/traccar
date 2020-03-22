package org.traccar.handler.events;

import io.netty.channel.ChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.database.IdentityManager;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Position;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * An event handler which generates new event if movement is detected.
 * This handler  is specific for hives and depends on the motion status,
 * if the device in the hive reports motion, it means the hive is being stolen.
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
    private final IdentityManager identityManager;

    public HiveMovementEventHandler(IdentityManager identityManager) {
        this.identityManager = identityManager;
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

            if (isHive && isMoving && didPositionChange(lastPosition, position)) {
                LOGGER.info("Detected movement of speed {} on device {} with id {}",
                        position.getSpeed(), device.getName(), device.getUniqueId());
                Event event = new Event(Event.TYPE_HIVE_MOVEMENT, position.getDeviceId(), position.getId());
                return Collections.singletonMap(event, position);
            }
        }
        return Collections.emptyMap();
    }

    private boolean didPositionChange(Position lastPosition, Position newPosition) {
        // This is a case for very first position where last position does not exist
        if (Objects.isNull(lastPosition) && Objects.nonNull(newPosition)) {
            return true;
        }
        return  lastPosition != null
                && newPosition != null
                && lastPosition.getLatitude() !=  newPosition.getLatitude()
                && lastPosition.getLongitude() != newPosition.getLongitude();
    }
}
