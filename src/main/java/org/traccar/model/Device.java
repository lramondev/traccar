/*
 * Copyright 2012 - 2022 Anton Tananaev (anton@traccar.org)
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.traccar.storage.QueryIgnore;
import org.traccar.storage.StorageName;

import java.util.Date;

@StorageName("geolocal_rastreador")
public class Device extends GroupedModel implements Disableable {

    private String descricao;

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String name) {
        this.descricao = name;
    }

    private String imei;

    public String getImei() {
        return imei;
    }

    public void setImei(String uniqueId) {
        this.imei = uniqueId;
    }

    public static final String STATUS_UNKNOWN = "unknown";
    public static final String STATUS_ONLINE = "online";
    public static final String STATUS_OFFLINE = "offline";

    private String status;

    @QueryIgnore
    public String getStatus() {
        return status != null ? status : STATUS_OFFLINE;
    }

    public void setStatus(String status) {
        this.status = status != null ? status.trim() : null;
    }

    /*
    private Date lastUpdate;

    @QueryIgnore
    public Date getLastUpdate() {
        return this.lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }
    */
    
    private long rastreador_posicao_id;

    @QueryIgnore
    public long getRastreador_posicao_id() {
        return rastreador_posicao_id;
    }

    public void setRastreador_posicao_id(long positionId) {
        this.rastreador_posicao_id = positionId;
    }

    /* 
    private String phone;

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
    */

    private String modelo;

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String model) {
        this.modelo = model;
    }

    /* 
    private String contact;

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }
    

    private String category;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
    */

    private int status_id;

    @Override
    public boolean getDisabled() {
        return this.status_id == 5 ? true : false;
    }

    @Override
    public void setDisabled(boolean disabled) {
        this.status_id = disabled == true ? 5 : 1;
    }

    /* 
    private Date expirationTime;

    @Override
    public Date getExpirationTime() {
        return expirationTime;
    }

    @Override
    public void setExpirationTime(Date expirationTime) {
        this.expirationTime = expirationTime;
    }

    private boolean motionStreak;

    @QueryIgnore
    @JsonIgnore
    public boolean getMotionStreak() {
        return motionStreak;
    }

    @JsonIgnore
    public void setMotionStreak(boolean motionStreak) {
        this.motionStreak = motionStreak;
    }

    private boolean motionState;

    @QueryIgnore
    @JsonIgnore
    public boolean getMotionState() {
        return motionState;
    }

    @JsonIgnore
    public void setMotionState(boolean motionState) {
        this.motionState = motionState;
    }

    private Date motionTime;

    @QueryIgnore
    @JsonIgnore
    public Date getMotionTime() {
        return motionTime;
    }

    @JsonIgnore
    public void setMotionTime(Date motionTime) {
        this.motionTime = motionTime;
    }

    private double motionDistance;

    @QueryIgnore
    @JsonIgnore
    public double getMotionDistance() {
        return motionDistance;
    }

    @JsonIgnore
    public void setMotionDistance(double motionDistance) {
        this.motionDistance = motionDistance;
    }

    private boolean overspeedState;

    @QueryIgnore
    @JsonIgnore
    public boolean getOverspeedState() {
        return overspeedState;
    }

    @JsonIgnore
    public void setOverspeedState(boolean overspeedState) {
        this.overspeedState = overspeedState;
    }

    private Date overspeedTime;

    @QueryIgnore
    @JsonIgnore
    public Date getOverspeedTime() {
        return overspeedTime;
    }

    @JsonIgnore
    public void setOverspeedTime(Date overspeedTime) {
        this.overspeedTime = overspeedTime;
    }

    private long overspeedGeofenceId;

    @QueryIgnore
    @JsonIgnore
    public long getOverspeedGeofenceId() {
        return overspeedGeofenceId;
    }

    @JsonIgnore
    public void setOverspeedGeofenceId(long overspeedGeofenceId) {
        this.overspeedGeofenceId = overspeedGeofenceId;
    }
    */
}
