/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.sembient.sembient.dto.airquality;

/**
 * AirQualityData class - Provide some information relating to air quality in a specific region
 * <ol>
 *   <li>C02(C)</li>
 *   <li>TVOc(micromet)</li>
 *   <li>PM25(microgram/m3)</li>
 *   <li>Timestamp(s)</li>
 * </ol>
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 9/30/2022
 * @since 1.0.0
 */
public class AirQualityData {
	private String co2;
	private String tvoc;
	private String pm25;
	private Long timestamp;

	/**
	 * Retrieves {@link #co2}
	 *
	 * @return value of {@link #co2}
	 */
	public String getCo2() {
		return co2;
	}

	/**
	 * Sets {@link #co2} value
	 *
	 * @param co2 new value of {@link #co2}
	 */
	public void setCo2(String co2) {
		this.co2 = co2;
	}

	/**
	 * Retrieves {@link #tvoc}
	 *
	 * @return value of {@link #tvoc}
	 */
	public String getTvoc() {
		return tvoc;
	}

	/**
	 * Sets {@link #tvoc} value
	 *
	 * @param tvoc new value of {@link #tvoc}
	 */
	public void setTvoc(String tvoc) {
		this.tvoc = tvoc;
	}

	/**
	 * Retrieves {@link #pm25}
	 *
	 * @return value of {@link #pm25}
	 */
	public String getPm25() {
		return pm25;
	}

	/**
	 * Sets {@link #pm25} value
	 *
	 * @param pm25 new value of {@link #pm25}
	 */
	public void setPm25(String pm25) {
		this.pm25 = pm25;
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
		return "AirQualityData{" +
				"co2='" + co2 + '\'' +
				", tvoc='" + tvoc + '\'' +
				", pm25=" + pm25 +
				", timestamp=" + timestamp +
				'}';
	}
}
