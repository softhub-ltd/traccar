package org.traccar.handler.events;

import io.netty.channel.ChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.database.IdentityManager;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Position;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * An event handler which generates new event if low temperature is detected.
 *
 * @author Hemed Ali
 */
@ChannelHandler.Sharable
public class LowTemperatureEventHandler extends BaseEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(LowTemperatureEventHandler.class);
    private  final double tempThreshold;
    private final IdentityManager identityManager;
    private boolean monitorLowTemperature = true; // move to config file


    public LowTemperatureEventHandler(Config config, IdentityManager identityManager) {
        this.identityManager = identityManager;
        tempThreshold = config.getDouble(Keys.EVENT_LOW_TEMP_THRESHOLD, 29);
    }

    @Override
    protected Map<Event, Position> analyzePosition(Position position) {
        if (monitorLowTemperature) {
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

                if (currentTemp < tempThreshold && currentTemp <= lastTemp) {
                    LOGGER.info("Low temperature detected on device {}, current value {}, previous value {}",
                            device.getName(), currentTemp, lastTemp);
                    Event event = new Event(Event.TYPE_LOW_TEMPERATURE, position.getDeviceId(), position.getId());
                    event.set(Position.KEY_TEMPERATURE, currentTemp);
                    return Collections.singletonMap(event, position);
                }
            }
        }
        return Collections.emptyMap();
    }

}
