/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.sembient.sembient.dto.thermal;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * ThermalSensorResponse class - A class contain information of thermal:
 * <ol>
 *   <li>Sensor name</li>
 *   <li>Region name</li>
 *   <li>List of {@link ThermalData}</li>
 * </ol>
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 9/30/2022
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ThermalSensorResponse {
	private String sensorName;
	private String regionName;
	@JsonAlias("data")
	private ThermalData[] thermalData;

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
	 * Retrieves {@link #thermalData}
	 *
	 * @return value of {@link #thermalData}
	 */
	public ThermalData[] getThermalData() {
		return thermalData;
	}

	/**
	 * Sets {@link #thermalData} value
	 *
	 * @param thermalData new value of {@link #thermalData}
	 */
	public void setThermalData(ThermalData[] thermalData) {
		this.thermalData = thermalData;
	}

	/**
	 * Retrieves {@link #regionName}
	 *
	 * @return value of {@link #regionName}
	 */
	public String getRegionName() {
		return regionName;
	}

	/**
	 * Sets {@link #regionName} value
	 *
	 * @param regionName new value of {@link #regionName}
	 */
	public void setRegionName(String regionName) {
		this.regionName = regionName;
	}

	@Override
	public String toString() {
		return "ThermalSensorResponse{" +
				"sensorName='" + sensorName + '\'' +
				", regionName='" + regionName + '\'' +
				", thermalData=" + Arrays.toString(thermalData) +
				'}';
	}
}
