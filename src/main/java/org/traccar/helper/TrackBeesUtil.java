package org.traccar.helper;

import org.traccar.Context;
import org.traccar.model.Event;
import org.traccar.model.Notification;
import org.traccar.model.Typed;
import org.traccar.model.User;

import java.sql.SQLException;
import java.util.Set;

/**
 * @author Ahmed Ali Rashid
 */

public final class TrackBeesUtil {

    private TrackBeesUtil() {
    }

    public static void subscribeToNotifications(long userId) throws SQLException {
        String commaSeparatedNotificatorTypes =
                getCommaSeparatedNotificatorTypes(Context.getNotificatorManager().getAllNotificatorTypes());
        createAndLinkNotification(Event.TYPE_HIGH_TEMPERATURE, userId, commaSeparatedNotificatorTypes);
        createAndLinkNotification(Event.TYPE_LOW_TEMPERATURE, userId, commaSeparatedNotificatorTypes);
        createAndLinkNotification(Event.TYPE_HIGH_HUMIDITY, userId, commaSeparatedNotificatorTypes);
        createAndLinkNotification(Event.TYPE_LOW_HUMIDITY, userId, commaSeparatedNotificatorTypes);
        createAndLinkNotification(Event.TYPE_HIVE_MOVEMENT, userId, commaSeparatedNotificatorTypes);

        Context.getNotificationManager().refreshItems();
    }
    private static String getCommaSeparatedNotificatorTypes(Set<Typed> typedSet) {
        String commaSeparatedNotificatorTypes = "";
        for (Typed typed : typedSet) {
            commaSeparatedNotificatorTypes = commaSeparatedNotificatorTypes.concat(typed.getType() + ",");
        }
        if (!commaSeparatedNotificatorTypes.endsWith(",")) {
            return commaSeparatedNotificatorTypes;
        }
        return commaSeparatedNotificatorTypes.substring(0, commaSeparatedNotificatorTypes.length() - 1);
    }
    private static void createAndLinkNotification(String eventType, long userId, String commaSeparatedNotificatorTypes)
            throws SQLException {
        Notification notification = new Notification();
        notification.setAlways(true);
        notification.setNotificators(commaSeparatedNotificatorTypes);
        notification.setType(eventType);
        notification.setCalendarId(0L);
        notification.setId(-1L);
        Context.getNotificationManager().addItem(notification);
        Context.getDataManager().linkObject(User.class, userId, Notification.class, notification.getId(), true);
        LogAction.link(userId, User.class, userId, Notification.class, notification.getId());
    }
}
