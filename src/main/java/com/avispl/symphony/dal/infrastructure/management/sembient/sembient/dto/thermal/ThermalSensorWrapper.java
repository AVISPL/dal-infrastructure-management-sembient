/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.sembient.sembient.dto.thermal;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * ThermalSensorWrapper class - A wrapper class contain information about:
 * <ol>
 *   <li>Sensor name</li>
 *   <li>List of thermal response from sensor{@link ThermalSensorResponse}</li>
 * </ol>
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 9/30/2022
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ThermalSensorWrapper {
	private String sensorName;
	@JsonAlias("sensors")
	private ThermalSensorResponse[] thermalSensorResponses;

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
	 * Retrieves {@link #thermalSensorResponses}
	 *
	 * @return value of {@link #thermalSensorResponses}
	 */
	public ThermalSensorResponse[] getThermalSensorResponses() {
		return thermalSensorResponses;
	}

	/**
	 * Sets {@link #thermalSensorResponses} value
	 *
	 * @param thermalSensorResponses new value of {@link #thermalSensorResponses}
	 */
	public void setThermalSensorResponses(ThermalSensorResponse[] thermalSensorResponses) {
		this.thermalSensorResponses = thermalSensorResponses;
	}

	@Override
	public String toString() {
		return "ThermalSensorWrapper{" +
				"sensorName='" + sensorName + '\'' +
				", thermalSensorResponses=" + Arrays.toString(thermalSensorResponses) +
				'}';
	}
}

