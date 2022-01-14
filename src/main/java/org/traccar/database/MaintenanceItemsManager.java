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
package org.traccar.database;

import org.traccar.Context;
import org.traccar.model.Maintenance;
import org.traccar.model.MaintenanceItem;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MaintenanceItemsManager extends BaseObjectManager<MaintenanceItem> implements ManagableObjects {

    public MaintenanceItemsManager(DataManager dataManager) {
        super(dataManager, MaintenanceItem.class);
    }

    @Override
    public Set<Long> getUserItems(long userId) {
        if (Context.getPermissionsManager() != null) {
            Set<Long> result = new HashSet<>();
            return result;
        } else {
            return new HashSet<>();
        }
    }

    public Set<Long> getUserItems(long userId, Collection<Maintenance> related) {
        if (Context.getPermissionsManager() != null) {
            Set<Long> result = new HashSet<>();
            for (Maintenance maintenance : related) {
                List<MaintenanceItem> maintenanceItems = getByRelatedId(maintenance.getId());
                for (MaintenanceItem maintenanceItem : maintenanceItems) {
                    result.add(maintenanceItem.getId());
                }
            }
            return result;
        } else {
            return new HashSet<>();
        }
    }

    @Override
    public Set<Long> getManagedItems(long userId) {
        Set<Long> result = new HashSet<>(getUserItems(userId));
        for (long managedUserId : Context.getUsersManager().getUserItems(userId)) {
            result.addAll(getUserItems(managedUserId));
        }
        return result;
    }

    public Set<Long> getManagedItems(long userId, Collection<Maintenance> related) {
        Set<Long> result = new HashSet<>(getUserItems(userId));
        for (long managedUserId : Context.getUsersManager().getUserItems(userId)) {
            result.addAll(getUserItems(managedUserId));
        }
        return result;
    }
}
