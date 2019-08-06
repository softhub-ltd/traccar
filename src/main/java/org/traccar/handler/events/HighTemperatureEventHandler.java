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
 * An event handler which generates new event if high temperature is detected.
 *
 * @author Hemed Ali
 */
@ChannelHandler.Sharable
public class HighTemperatureEventHandler extends BaseEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(HighTemperatureEventHandler.class);
    private static final double TEMP_THRESHOLD = 37.5; // move to config file
    private final IdentityManager identityManager;
    private boolean monitorHighTemperature = true; // move to config file


    public HighTemperatureEventHandler(IdentityManager identityManager) {
        this.identityManager = identityManager;
    }

    @Override
    protected Map<Event, Position> analyzePosition(Position position) {
        if (monitorHighTemperature) {
            Device device = identityManager.getById(position.getDeviceId());
            //Ensure validity
            if (Objects.isNull(device)
                    || !position.getValid()
                    || !identityManager.isLatestPosition(position)) {
                return Collections.emptyMap();
            }
            if (position.getAttributes().containsKey(Position.KEY_TEMPERATURE)) {
                Position lastPosition = identityManager.getLastPosition(position.getDeviceId());
                double currentTemp = position.getDouble(Position.KEY_TEMPERATURE);
                double lastTemp = lastPosition == null ? 0.0 : lastPosition.getDouble(Position.KEY_TEMPERATURE);

                if (currentTemp > TEMP_THRESHOLD && currentTemp >= lastTemp) {
                    LOGGER.info("High temperature detected on device {}, current value {}, previous value {}",
                            device.getName(), currentTemp, lastTemp);
                    Event event = new Event(Event.TYPE_HIGH_TEMPERATURE, position.getDeviceId(), position.getId());
                    event.set(Position.KEY_TEMPERATURE, currentTemp);
                    return Collections.singletonMap(event, position);
                }
            }
        }
        return Collections.emptyMap();
    }

}
