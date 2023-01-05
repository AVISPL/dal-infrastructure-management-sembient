/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.sembient.sembient.dto.thermal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * ThermalData class - A class contain information of thermal:
 * <ol>
 *   <li>Temperature(F)</li>
 *   <li>Humidity(%)</li>
 *   <li>Timestamp(s)</li>
 * </ol>
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 9/30/2022
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ThermalData {
	private int temperature;
	private int humidity;
	private Long timestamp;

	/**
	 * Retrieves {@link #temperature}
	 *
	 * @return value of {@link #temperature}
	 */
	public int getTemperature() {
		return temperature;
	}

	/**
	 * Sets {@link #temperature} value
	 *
	 * @param temperature new value of {@link #temperature}
	 */
	public void setTemperature(int temperature) {
		this.temperature = temperature;
	}

	/**
	 * Retrieves {@link #humidity}
	 *
	 * @return value of {@link #humidity}
	 */
	public int getHumidity() {
		return humidity;
	}

	/**
	 * Sets {@link #humidity} value
	 *
	 * @param humidity new value of {@link #humidity}
	 */
	public void setHumidity(int humidity) {
		this.humidity = humidity;
	}

	/**
	 * Retrieves {@link #timestamp}
	 *
	 * @return value of {@link #timestamp}
	 */
	public Long getTimestamp() {
		return timestamp;
	}

	/**
	 * Sets {@link #timestamp} value
	 *
	 * @param timestamp new value of {@link #timestamp}
	 */
	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	public String toString() {
		return "ThermalData{" +
				"temperature='" + temperature + '\'' +
				", humidity='" + humidity + '\'' +
				", timestamp=" + timestamp +
				'}';
	}
}
