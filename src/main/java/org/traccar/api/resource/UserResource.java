/*
 * Copyright 2015 - 2017 Anton Tananaev (anton@traccar.org)
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
package org.traccar.api.resource;

import org.traccar.Context;
import org.traccar.api.BaseObjectResource;
import org.traccar.database.UsersManager;
import org.traccar.helper.LogAction;
import org.traccar.model.ManagedUser;
import org.traccar.model.User;
import org.traccar.model.Notification;
import org.traccar.model.Event;
import org.traccar.model.Typed;

import javax.annotation.security.PermitAll;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.Set;

@Path("users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource extends BaseObjectResource<User> {

    public UserResource() {
        super(User.class);
    }

    @GET
    public Collection<User> get(@QueryParam("userId") long userId) throws SQLException {
        UsersManager usersManager = Context.getUsersManager();
        Set<Long> result = null;
        if (Context.getPermissionsManager().getUserAdmin(getUserId())) {
            if (userId != 0) {
                result = usersManager.getUserItems(userId);
            } else {
                result = usersManager.getAllItems();
            }
        } else if (Context.getPermissionsManager().getUserManager(getUserId())) {
            result = usersManager.getManagedItems(getUserId());
        } else {
            throw new SecurityException("Admin or manager access required");
        }
        return usersManager.getItems(result);
    }

    @Override
    @PermitAll
    @POST
    public Response add(User entity) throws SQLException {
        if (!Context.getPermissionsManager().getUserAdmin(getUserId())) {
            Context.getPermissionsManager().checkUserUpdate(getUserId(), new User(), entity);
            if (Context.getPermissionsManager().getUserManager(getUserId())) {
                Context.getPermissionsManager().checkUserLimit(getUserId());
            } else {
                Context.getPermissionsManager().checkRegistration(getUserId());
                entity.setDeviceLimit(Context.getConfig().getInteger("users.defaultDeviceLimit", -1));
                int expirationDays = Context.getConfig().getInteger("users.defaultExpirationDays");
                if (expirationDays > 0) {
                    entity.setExpirationTime(
                        new Date(System.currentTimeMillis() + (long) expirationDays * 24 * 3600 * 1000));
                }
            }
        }
        Context.getUsersManager().addItem(entity);

        subscribeToNotifications(entity.getId());

        LogAction.create(getUserId(), entity);
        if (Context.getPermissionsManager().getUserManager(getUserId())) {
            Context.getDataManager().linkObject(User.class, getUserId(), ManagedUser.class, entity.getId(), true);
            LogAction.link(getUserId(), User.class, getUserId(), ManagedUser.class, entity.getId());
        }
        Context.getNotificationManager().refreshItems();
        Context.getUsersManager().refreshUserItems();
        return Response.ok(entity).build();
    }

    private void subscribeToNotifications(long userId) throws SQLException {
        String commaSeparatedNotificatorTypes =
                getCommaSeparatedNotificatorTypes(Context.getNotificatorManager().getAllNotificatorTypes());
        createAndLinkNotification(Event.TYPE_HIGH_TEMPERATURE, userId, commaSeparatedNotificatorTypes);
        createAndLinkNotification(Event.TYPE_LOW_TEMPERATURE, userId, commaSeparatedNotificatorTypes);
        createAndLinkNotification(Event.TYPE_HIGH_HUMIDITY, userId, commaSeparatedNotificatorTypes);
        createAndLinkNotification(Event.TYPE_LOW_HUMIDITY, userId, commaSeparatedNotificatorTypes);
    }

    private void createAndLinkNotification(String eventType, long userId, String commaSeparatedNotificatorTypes)
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

    private String getCommaSeparatedNotificatorTypes(Set<Typed> typedSet) {
        String commaSeparatedNotificatorTypes = "";
        for (Typed typed : typedSet) {
            commaSeparatedNotificatorTypes = commaSeparatedNotificatorTypes.concat(typed.getType() + ",");
        }
        if (!commaSeparatedNotificatorTypes.endsWith(",")) {
            return commaSeparatedNotificatorTypes;
        }
        return commaSeparatedNotificatorTypes.substring(0, commaSeparatedNotificatorTypes.length() - 1);
    }
}
