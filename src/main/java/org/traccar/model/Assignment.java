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
package org.traccar.model;

import java.util.Date;

public class Assignment extends ExtendedModel {

    private long deviceId;

    public long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(long deviceId) {
        this.deviceId = deviceId;
    }

    private long companyId;

    public long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(long companyId) {
        this.companyId = companyId;
    }

    private long worksiteId;

    public long getWorksiteId() {
        return worksiteId;
    }

    public void setWorksiteId(long worksiteId) {
        this.worksiteId = worksiteId;
    }

    private double price;

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    private Date dateStart;

    public Date getDateStart() {
        return dateStart;
    }

    public void setDateStart(Date dateStart) {
        this.dateStart = dateStart;
    }

    private Date dateEnd;

    public Date getDateEnd() {
        return dateEnd;
    }

    public void setDateEnd(Date dateEnd) {
        this.dateEnd = dateEnd;
    }

    private double odometerStart;

    public double getOdometerStart() {
        return odometerStart;
    }

    public void setOdometerStart(double odometerStart) {
        this.odometerStart = odometerStart;
    }

    private double odometerEnd;

    public double getOdometerEnd() {
        return odometerEnd;
    }

    public void setOdometerEnd(double odometerEnd) {
        this.odometerEnd = odometerEnd;
    }

    private double hoursStart;

    public double getHoursStart() {
        return hoursStart;
    }

    public void setHoursStart(double hoursStart) {
        this.hoursStart = hoursStart;
    }

    private double hoursEnd;

    public double getHoursEnd() {
        return hoursEnd;
    }

    public void setHoursEnd(double hoursEnd) {
        this.hoursEnd = hoursEnd;
    }

    private double hoursMaxDay;

    public double getHoursMaxDay() {
        return hoursMaxDay;
    }

    public void setHoursMaxDay(double hoursMaxDay) {
        this.hoursMaxDay = hoursMaxDay;
    }

    private double hoursMaxMonth;

    public double getHoursMaxMonth() {
        return hoursMaxMonth;
    }

    public void setHoursMaxMonth(double hoursMaxMonth) {
        this.hoursMaxMonth = hoursMaxMonth;
    }

}
