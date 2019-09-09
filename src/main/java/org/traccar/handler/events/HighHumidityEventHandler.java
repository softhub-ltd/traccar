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
 * An event handler which generates new event if high humidity is detected.
 *
 * @author Hemed Ali
 */
@ChannelHandler.Sharable
public class HighHumidityEventHandler extends BaseEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(HighHumidityEventHandler.class);
    private  final double humidityThreshold;
    private final IdentityManager identityManager;
    private boolean monitorHighHumidity = true; // move to config file


    public HighHumidityEventHandler(Config config, IdentityManager identityManager) {
        this.identityManager = identityManager;
        this.humidityThreshold = config.getDouble(Keys.EVENT_HIGH_HUMIDITY_THRESHOLD, 80.0);
    }

    @Override
    protected Map<Event, Position> analyzePosition(Position position) {
        if (monitorHighHumidity) {
            Device device = identityManager.getById(position.getDeviceId());
            //Ensure validity
            if (Objects.isNull(device)
                    || !position.getValid()
                    || !identityManager.isLatestPosition(position)) {
                return Collections.emptyMap();
            }
            if (position.getAttributes().containsKey(Position.KEY_HUMIDITY)) {
                Position lastPosition = identityManager.getLastPosition(position.getDeviceId());
                double currentHumidity = position.getDouble(Position.KEY_HUMIDITY);
                double lastHumidity = lastPosition == null ? 0.0 : lastPosition.getDouble(Position.KEY_HUMIDITY);

                if (currentHumidity > humidityThreshold && currentHumidity >= lastHumidity) {
                    LOGGER.info("High humidity detected on device {}, current value {}, previous value {}",
                            device.getName(), currentHumidity, lastHumidity);
                    Event event = new Event(Event.TYPE_HIGH_HUMIDITY, position.getDeviceId(), position.getId());
                    event.set(Position.KEY_HUMIDITY, currentHumidity);
                    return Collections.singletonMap(event, position);
                }
            }
        }
        return Collections.emptyMap();
    }

}
