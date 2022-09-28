package com.avispl.symphony.dal.infrastructure.management.sembient.sembient.dto.airquality;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AirQualityWrapper {
    private String statusCode;
    @JsonAlias("body")
    private AirQualitySensorWrapper airQualitySensorWrapper;

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
