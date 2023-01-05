/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.sembient.sembient.dto.airquality;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Arrays;

/**
 * AirQualitySensorResponse class - This class provide some basic information about air quality information in this region
 * <ol>
 *   <li>Sensor name</li>
 *   <li>Region name</li>
 *   <li>Array of {@link AirQualityData}</li>
 * </ol>
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 9/30/2022
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AirQualitySensorResponse {
	private String sensorName;

	private String regionName;
	@JsonAlias("data")
	private AirQualityData[] airQualityData;

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
	 * Retrieves {@link #airQualityData}
	 *
	 * @return value of {@link #airQualityData}
	 */
	public AirQualityData[] getAirQualityData() {
		return airQualityData;
	}

	/**
	 * Sets {@link #airQualityData} value
	 *
	 * @param airQualityData new value of {@link #airQualityData}
	 */
	public void setAirQualityData(AirQualityData[] airQualityData) {
		this.airQualityData = airQualityData;
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
		return "AirQualitySensorResponse{" +
				"sensorName='" + sensorName + '\'' +
				", regionName='" + regionName + '\'' +
				", airQualityData=" + Arrays.toString(airQualityData) +
				'}';
	}
}
