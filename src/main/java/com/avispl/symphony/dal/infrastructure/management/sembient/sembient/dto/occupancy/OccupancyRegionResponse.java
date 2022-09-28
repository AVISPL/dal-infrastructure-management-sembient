package com.avispl.symphony.dal.infrastructure.management.sembient.sembient.dto.occupancy;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
