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
 * This handler generates a new event if high temperature is detected.
 *
 * @author Hemed Ali
 */
@ChannelHandler.Sharable
public class HighTemperatureEventHandler extends BaseEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(HighTemperatureEventHandler.class);
    private final static double TEMP_THRESHOLD = 37.0;
    private final IdentityManager identityManager;


    public HighTemperatureEventHandler(IdentityManager identityManager) {
        this.identityManager = identityManager;
    }

    @Override
    protected Map<Event, Position> analyzePosition(Position position) {
        Device device = identityManager.getById(position.getDeviceId());

        //Ensure validity
        if (Objects.isNull(device) ||
                !position.getValid() ||
                !identityManager.isLatestPosition(position)) {
            return Collections.emptyMap();
        }

        if (position.getAttributes().containsKey(Position.KEY_TEMPERATURE)) {
            Position lastPosition = identityManager.getLastPosition(position.getDeviceId());
            double currentTemp = position.getDouble(Position.KEY_TEMPERATURE);
            double lastTemp = lastPosition == null? 0.0 : lastPosition.getDouble(Position.KEY_TEMPERATURE);

            if (currentTemp > TEMP_THRESHOLD && currentTemp >= lastTemp) {
                logger.info("High temperature on device {}, current value {}, last value {}",
                        device.getName(), currentTemp, lastTemp);
                Event event = new Event(Event.HIGH_TEMPERATURE, position.getDeviceId(), position.getId());
                event.set(Position.KEY_TEMPERATURE, currentTemp);
                return Collections.singletonMap(event, position);
            }
        }
        return Collections.emptyMap();
    }
}
