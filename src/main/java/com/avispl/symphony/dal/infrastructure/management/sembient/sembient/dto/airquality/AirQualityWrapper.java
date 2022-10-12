/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.sembient.sembient.dto.airquality;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * AirQualityWrapper class - A wrapper class contain information about:
 * <ol>
 *   <li>Status code</li>
 *   <li>A body wrapper {@link AirQualitySensorWrapper}</li>
 * </ol>
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 9/30/2022
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AirQualityWrapper {
    private String statusCode;
    @JsonAlias("body")
    private AirQualitySensorWrapper airQualitySensorWrapper;

//    @JsonAlias("body")
//    private ErrorResponse errorResponse;

    /**
     * Retrieves {@link #statusCode}
     *
     * @return value of {@link #statusCode}
     */
    public String getStatusCode() {
        return statusCode;
    }

    /**
     * Sets {@link #statusCode} value
     *
     * @param statusCode new value of {@link #statusCode}
     */
    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * Retrieves {@link #airQualitySensorWrapper}
     *
     * @return value of {@link #airQualitySensorWrapper}
     */
    public AirQualitySensorWrapper getAirQualitySensorWrapper() {
        return airQualitySensorWrapper;
    }

    /**
     * Sets {@link #airQualitySensorWrapper} value
     *
     * @param airQualitySensorWrapper new value of {@link #airQualitySensorWrapper}
     */
    public void setAirQualitySensorWrapper(AirQualitySensorWrapper airQualitySensorWrapper) {
        this.airQualitySensorWrapper = airQualitySensorWrapper;
    }

    @Override
    public String toString() {
        return "AirQualityWrapper{" +
            "statusCode='" + statusCode + '\'' +
            ", airQualitySensorWrapper=" + airQualitySensorWrapper +
            '}';
    }
}
