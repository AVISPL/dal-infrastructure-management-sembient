/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.sembient.sembient.dto.airquality;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Arrays;

/**
 * AirQualitySensorWrapper class - A wrapper class contains information about sensor name and array of {@link AirQualitySensorResponse}
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 9/30/2022
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AirQualitySensorWrapper {
	private String sensorName;
	@JsonAlias("sensors")
	private AirQualitySensorResponse[] airQualitySensorResponses;

	@JsonAlias("Error")
	private String error;

	/**
	 * Retrieves {@link #sensorName}
	 *
	 * @return value of {@link #sensorName}
	 */
	public String getSensorName() {
		return sensorName;
	}

	/**
	 * Sets {@link #sensorName} value
	 *
	 * @param sensorName new value of {@link #sensorName}
	 */
	public void setSensorName(String sensorName) {
		this.sensorName = sensorName;
	}

	/**
	 * Retrieves {@link #airQualitySensorResponses}
	 *
	 * @return value of {@link #airQualitySensorResponses}
	 */
	public AirQualitySensorResponse[] getAirQualitySensorResponses() {
		return airQualitySensorResponses;
	}

	/**
	 * Sets {@link #airQualitySensorResponses} value
	 *
	 * @param airQualitySensorResponses new value of {@link #airQualitySensorResponses}
	 */
	public void setAirQualitySensorResponses(AirQualitySensorResponse[] airQualitySensorResponses) {
		this.airQualitySensorResponses = airQualitySensorResponses;
	}

	/**
	 * Retrieves {@link #error}
	 *
	 * @return value of {@link #error}
	 */
	public String getError() {
		return error;
	}

	/**
	 * Sets {@link #error} value
	 *
	 * @param error new value of {@link #error}
	 */
	public void setError(String error) {
		this.error = error;
	}

	@Override
	public String toString() {
		return "AirQualitySensorWrapper{" +
				"sensorName='" + sensorName + '\'' +
				", airQualitySensorResponses=" + Arrays.toString(airQualitySensorResponses) +
				'}';
	}
}

