/*
 * Copyright 2017 - 2020 Anton Tananaev (anton@traccar.org)
 * Copyright 2017 Andrey Kunitsyn (andrey@traccar.org)
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

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.Context;
import org.traccar.model.Device;
import org.traccar.model.Group;
import org.traccar.model.Maintenance;
import org.traccar.model.Item;
import org.traccar.model.Permission;
import org.traccar.model.BaseModel;

public abstract class ExtendedObjectManager<T extends BaseModel> extends SimpleObjectManager<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExtendedObjectManager.class);

    private final Map<Long, Set<Long>> maintenanceItems = new ConcurrentHashMap<>();
    private final Map<Long, Set<Long>> deviceItems = new ConcurrentHashMap<>();
    private final Map<Long, Set<Long>> deviceItemsWithGroups = new ConcurrentHashMap<>();
    private final Map<Long, Set<Long>> groupItems = new ConcurrentHashMap<>();

    protected ExtendedObjectManager(DataManager dataManager, Class<T> baseClass) {
        super(dataManager, baseClass);
        refreshExtendedPermissions();
    }

    public final Set<Long> getGroupItems(long groupId) {
        try {
            readLock();
            Set<Long> result = groupItems.get(groupId);
            if (result != null) {
                return new HashSet<>(result);
            } else {
                return new HashSet<>();
            }
        } finally {
            readUnlock();
        }
    }

    public final Set<Long> getDeviceItems(long deviceId) {
        try {
            readLock();
            Set<Long> result = deviceItems.get(deviceId);
            if (result != null) {
                return new HashSet<>(result);
            } else {
                return new HashSet<>();
            }
        } finally {
            readUnlock();
        }
    }

    public Set<Long> getAllDeviceItems(long deviceId) {
        try {
            readLock();
            Set<Long> result = deviceItemsWithGroups.get(deviceId);
            if (result != null) {
                return new HashSet<>(result);
            } else {
                return new HashSet<>();
            }
        } finally {
            readUnlock();
        }
    }

    public final Set<Long> getMaintenanceItems(long maintenanceId) {
        try {
            readLock();
            Set<Long> result = maintenanceItems.get(maintenanceId);
            if (result != null) {
                return new HashSet<>(result);
            } else {
                return new HashSet<>();
            }
        } finally {
            readUnlock();
        }
    }

    @Override
    public void removeItem(long itemId) throws SQLException {
        super.removeItem(itemId);
        refreshExtendedPermissions();
    }

    public void refreshExtendedPermissions() {
        if (getDataManager() != null) {
            try {
                Collection<Permission> databaseGroupPermissions =
                        getDataManager().getPermissions(Group.class, getBaseClass());

                Collection<Permission> databaseDevicePermissions =
                        getDataManager().getPermissions(Device.class, getBaseClass());

                Collection<Permission> databaseMaintenancePermissions = null;

                if (getBaseClass() == Item.class) {
                    databaseMaintenancePermissions = getDataManager().getPermissions(Maintenance.class, getBaseClass());
                } else if (getBaseClass() == Maintenance.class) {
                    databaseMaintenancePermissions = getDataManager().getPermissions(getBaseClass(), Item.class);
                }

                writeLock();

                groupItems.clear();
                deviceItems.clear();
                deviceItemsWithGroups.clear();
                maintenanceItems.clear();

                for (Permission groupPermission : databaseGroupPermissions) {
                    groupItems
                            .computeIfAbsent(groupPermission.getOwnerId(), key -> new HashSet<>())
                            .add(groupPermission.getPropertyId());
                }

                for (Permission devicePermission : databaseDevicePermissions) {
                    deviceItems
                            .computeIfAbsent(devicePermission.getOwnerId(), key -> new HashSet<>())
                            .add(devicePermission.getPropertyId());
                    deviceItemsWithGroups
                            .computeIfAbsent(devicePermission.getOwnerId(), key -> new HashSet<>())
                            .add(devicePermission.getPropertyId());
                }

                if (databaseMaintenancePermissions != null) {
                    for (Permission maintenancePermission : databaseMaintenancePermissions) {
                        maintenanceItems
                                .computeIfAbsent(maintenancePermission.getOwnerId(), key -> new HashSet<>())
                                .add(maintenancePermission.getPropertyId());
                    }
                }

                for (Device device : Context.getDeviceManager().getAllDevices()) {
                    long groupId = device.getGroupId();
                    while (groupId > 0) {
                        deviceItemsWithGroups
                                .computeIfAbsent(device.getId(), key -> new HashSet<>())
                                .addAll(groupItems.getOrDefault(groupId, new HashSet<>()));
                        Group group = Context.getGroupsManager().getById(groupId);
                        groupId = group != null ? group.getGroupId() : 0;
                    }
                }

            } catch (SQLException | ClassNotFoundException error) {
                LOGGER.warn("Refresh permissions error", error);
            } finally {
                writeUnlock();
            }
        }
    }
}
