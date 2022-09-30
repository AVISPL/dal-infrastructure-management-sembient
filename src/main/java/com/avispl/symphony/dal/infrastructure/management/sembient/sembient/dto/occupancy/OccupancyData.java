/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.sembient.sembient.dto.occupancy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * OccupancyData class
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 9/30/2022
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OccupancyData {
	private String hour;
	private String occupancy;
	private String usageTime;

	/**
	 * Retrieves {@link #hour}
	 *
	 * @return value of {@link #hour}
	 */
	public String getHour() {
		return hour;
	}

	/**
	 * Sets {@link #hour} value
	 *
	 * @param hour new value of {@link #hour}
	 */
	public void setHour(String hour) {
		this.hour = hour;
	}

	/**
	 * Retrieves {@link #occupancy}
	 *
	 * @return value of {@link #occupancy}
	 */
	public String getOccupancy() {
		return occupancy;
	}

	/**
	 * Sets {@link #occupancy} value
	 *
	 * @param occupancy new value of {@link #occupancy}
	 */
	public void setOccupancy(String occupancy) {
		this.occupancy = occupancy;
	}

	/**
	 * Retrieves {@link #usageTime}
	 *
	 * @return value of {@link #usageTime}
	 */
	public String getUsageTime() {
		return usageTime;
	}

	/**
	 * Sets {@link #usageTime} value
	 *
	 * @param usageTime new value of {@link #usageTime}
	 */
	public void setUsageTime(String usageTime) {
		this.usageTime = usageTime;
	}

	@Override
	public String toString() {
		return "OccupancyData{" +
				"hour='" + hour + '\'' +
				", occupancy='" + occupancy + '\'' +
				", usageTime='" + usageTime + '\'' +
				'}';
	}
}
