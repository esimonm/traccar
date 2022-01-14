/*
 * Copyright 2018 Anton Tananaev (anton@traccar.org)
 * Copyright 2018 Andrey Kunitsyn (andrey@traccar.org)
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

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import org.traccar.Context;
import org.traccar.api.SimpleObjectResource;
import org.traccar.database.BaseObjectManager;
import org.traccar.database.ExtendedObjectManager;
import org.traccar.database.ManagableObjects;
import org.traccar.database.MaintenanceItemsManager;
import org.traccar.model.BaseModel;
import org.traccar.model.Maintenance;
import org.traccar.model.MaintenanceItem;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Path("maintenanceitems")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MaintenanceItemResource<T extends BaseModel> extends SimpleObjectResource<MaintenanceItem> {

    public MaintenanceItemResource() {
        super(MaintenanceItem.class);
    }

    @GET
    @Override
    public Collection<MaintenanceItem> get(
            @QueryParam("all") boolean all,
            @QueryParam("userId") long userId) throws SQLException {

        ExtendedObjectManager<T> maintenanceManager = (ExtendedObjectManager<T>) Context.getManager(Maintenance.class);
        Collection<T> maintenances = maintenanceManager.getItems(getRelatedManagerItems(maintenanceManager, all, userId));

        BaseObjectManager<T> manager = (BaseObjectManager<T>) Context.getManager(getBaseClass());
        Collection<MaintenanceItem> maintenanceItems = (Collection<MaintenanceItem>) manager.getItems(getManagerItems(manager, all, userId, maintenances));

        return maintenanceItems;
    }

    protected final Set<Long> getRelatedManagerItems(BaseObjectManager<T> manager, boolean all, long userId) {
        Set<Long> result;

        if (all) {
            if (Context.getPermissionsManager().getUserAdmin(getUserId())) {
                result = manager.getAllItems();
            } else {
                Context.getPermissionsManager().checkManager(getUserId());
                result = ((ManagableObjects) manager).getManagedItems(getUserId());
            }
        } else {
            if (userId == 0) {
                userId = getUserId();
            }
            Context.getPermissionsManager().checkUser(getUserId(), userId);
            result = ((ManagableObjects) manager).getUserItems(userId);
        }
        return result;
    }

    protected final Set<Long> getManagerItems(BaseObjectManager<T> manager, boolean all, long userId, Collection<T> related) {
        Set<Long> result;

        if (all) {
            if (Context.getPermissionsManager().getUserAdmin(getUserId())) {
                result = manager.getAllItems();
            } else {
                Context.getPermissionsManager().checkManager(getUserId());
                result = ((MaintenanceItemsManager) manager).getManagedItems(getUserId(), (Collection<Maintenance>) related);
            }
        } else {
            if (userId == 0) {
                userId = getUserId();
            }
            Context.getPermissionsManager().checkUser(getUserId(), userId);
            result = ((MaintenanceItemsManager) manager).getUserItems(userId, (Collection<Maintenance>) related);
        }
        return result;
    }
}
