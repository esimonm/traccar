/*
 * Copyright 2016 Anton Tananaev (anton@traccar.org)
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
package org.traccar.model;

import java.text.ParseException;

import org.traccar.Context;
import org.traccar.config.Keys;
import org.traccar.database.QueryIgnore;
import org.traccar.geofence.GeofenceCircle;
import org.traccar.geofence.GeofenceGeometry;
import org.traccar.geofence.GeofencePolygon;
import org.traccar.geofence.GeofencePolyline;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Geofence extends ScheduledModel {

    public static final String TYPE_GEOFENCE_CIRCLE = "geofenceCircle";
    public static final String TYPE_GEOFENCE_POLYGON = "geofencePolygon";
    public static final String TYPE_GEOFENCE_POLYLINE = "geofencePolyline";

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private String description;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    private double centerLat;

    public double getCenterLat() {
        return centerLat;
    }

    public void setCenterLat(double centerLat) {
        this.centerLat = centerLat;
    }

    private double centerLon;

    public double getCenterLon() {
        return centerLon;
    }

    public void setCenterLon(double centerLon) {
        this.centerLon = centerLon;
    }

    private String area;

    public String getArea() {
        return area;
    }

    public void setArea(String area) throws ParseException {

        if (area.startsWith("CIRCLE")) {
            geometry = new GeofenceCircle(area);
        } else if (area.startsWith("POLYGON")) {
            geometry = new GeofencePolygon(area);
        } else if (area.startsWith("LINESTRING")) {
            final double distance = getDouble("polylineDistance");
            geometry = new GeofencePolyline(area, distance > 0 ? distance
                    : Context.getConfig().getDouble(Keys.GEOFENCE_POLYLINE_DISTANCE));
        } else {
            throw new ParseException("Unknown geometry type", 0);
        }

        this.area = area;
    }

    private GeofenceGeometry geometry;

    @QueryIgnore
    @JsonIgnore
    public GeofenceGeometry getGeometry() {
        return geometry;
    }

    @QueryIgnore
    @JsonIgnore
    public void setGeometry(GeofenceGeometry geometry) {
        area = geometry.toWkt();
        this.geometry = geometry;
    }
}
