/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.sembient.sembient.dto.occupancy;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * OccupancyRegionResponse class - A class contain information about region name and array of {@link OccupancyData}
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 9/30/2022
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OccupancyRegionResponse {
	private String regionName;

	@JsonAlias("data")
	private OccupancyData[] occupancyData;

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

	/**
	 * Retrieves {@link #occupancyData}
	 *
	 * @return value of {@link #occupancyData}
	 */
	public OccupancyData[] getOccupancyData() {
		return occupancyData;
	}

	/**
	 * Sets {@link #occupancyData} value
	 *
	 * @param occupancyData new value of {@link #occupancyData}
	 */
	public void setOccupancyData(OccupancyData[] occupancyData) {
		this.occupancyData = occupancyData;
	}

	@Override
	public String toString() {
		return "OccupancyRegionResponse{" +
				"regionName='" + regionName + '\'' +
				", occupancyData=" + Arrays.toString(occupancyData) +
				'}';
	}
}
