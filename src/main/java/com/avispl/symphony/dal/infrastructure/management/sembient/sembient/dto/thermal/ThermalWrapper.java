/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.sembient.sembient.dto.thermal;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * ThermalWrapper class - A wrapper class contain information about:
 * <ol>
 *   <li>Status code</li>
 *   <li>{@link ThermalSensorWrapper}</li>
 * </ol>
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 9/30/2022
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ThermalWrapper {
	private String statusCode;
	@JsonAlias("body")
	private ThermalSensorWrapper thermalSensorWrappers;

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
	 * Retrieves {@link #thermalSensorWrappers}
	 *
	 * @return value of {@link #thermalSensorWrappers}
	 */
	public ThermalSensorWrapper getThermalSensorWrappers() {
		return thermalSensorWrappers;
	}

	/**
	 * Sets {@link #thermalSensorWrappers} value
	 *
	 * @param thermalSensorWrappers new value of {@link #thermalSensorWrappers}
	 */
	public void setThermalSensorWrappers(ThermalSensorWrapper thermalSensorWrappers) {
		this.thermalSensorWrappers = thermalSensorWrappers;
	}

	@Override
	public String toString() {
		return "ThermalWrapper{" +
				"statusCode='" + statusCode + '\'' +
				", thermalSensorWrappers=" + thermalSensorWrappers +
				'}';
	}
}
