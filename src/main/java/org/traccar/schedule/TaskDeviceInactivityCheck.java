/*
 * Copyright 2020 - 2022 Anton Tananaev (anton@traccar.org)
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
package org.traccar.schedule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.database.NotificationManager;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Request;
import org.traccar.storage.query.Condition;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TaskDeviceInactivityCheck implements ScheduleTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskDeviceInactivityCheck.class);

    public static final String ATTRIBUTE_DEVICE_INACTIVITY_START = "deviceInactivityStart";
    public static final String ATTRIBUTE_DEVICE_INACTIVITY_PERIOD = "deviceInactivityPeriod";
    public static final String ATTRIBUTE_LAST_UPDATE = "lastUpdate";

    private static final long CHECK_PERIOD_MINUTES = 15;

    private final Storage storage;
    private final NotificationManager notificationManager;

    @Inject
    public TaskDeviceInactivityCheck(Storage storage, NotificationManager notificationManager) {
        this.storage = storage;
        this.notificationManager = notificationManager;
    }

    @Override
    public void schedule(ScheduledExecutorService executor) {
        executor.scheduleAtFixedRate(this, CHECK_PERIOD_MINUTES, CHECK_PERIOD_MINUTES, TimeUnit.MINUTES);
    }

    @Override
    public void run() {
        long currentTime = System.currentTimeMillis();
        long checkPeriod = TimeUnit.MINUTES.toMillis(CHECK_PERIOD_MINUTES);

        Map<Event, Position> events = new HashMap<>();

        try {
            for (Device device : storage.getObjects(Device.class, new Request(new Columns.All()))) {
                Position lastPosition = storage.getObject(Position.class, new Request(new Columns.All(), new Condition.LatestPositions(device.getId())));
                if (lastPosition.getDatahora_rastreador() != null && checkDevice(device, currentTime, checkPeriod)) {
                    Event event = new Event(Event.TYPE_DEVICE_INACTIVE, device.getId());
                    event.set(ATTRIBUTE_LAST_UPDATE, lastPosition.getDatahora_rastreador().getTime());
                    events.put(event, null);
                }
            }
        } catch (StorageException e) {
            LOGGER.warn("Get devices error", e);
        }

        notificationManager.updateEvents(events);
    }

    private boolean checkDevice(Device device, long currentTime, long checkPeriod) {
        try {
            long deviceInactivityStart = device.getLong(ATTRIBUTE_DEVICE_INACTIVITY_START);
            Position lastPosition = storage.getObject(Position.class, new Request(new Columns.All(), new Condition.LatestPositions(device.getId())));
    
            if (deviceInactivityStart > 0) {
                long timeThreshold = lastPosition.getDatahora_rastreador().getTime() + deviceInactivityStart;
                if (currentTime >= timeThreshold) {
    
                    if (currentTime - checkPeriod < timeThreshold) {
                        return true;
                    }
    
                    long deviceInactivityPeriod = device.getLong(ATTRIBUTE_DEVICE_INACTIVITY_PERIOD);
                    if (deviceInactivityPeriod > 0) {
                        long count = (currentTime - timeThreshold - 1) / deviceInactivityPeriod;
                        timeThreshold += count * deviceInactivityPeriod;
                        return currentTime - checkPeriod < timeThreshold;
                    }
    
                }
            }
        } catch (StorageException e) {
            LOGGER.warn("Get devices error", e);
        }

        return false;
    }

}
