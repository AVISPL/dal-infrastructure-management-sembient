package com.avispl.symphony.dal.infrastructure.management.sembient.sembient.dto.occupancy;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OccupancyRegionWrapper {
	@JsonAlias("regions")
	private OccupancyRegionResponse[] occupancyRegionResponses;

	/**
	 * Retrieves {@link #occupancyRegionResponses}
	 *
	 * @return value of {@link #occupancyRegionResponses}
	 */
	public OccupancyRegionResponse[] getOccupancyRegionResponses() {
		return occupancyRegionResponses;
	}

	/**
	 * Sets {@link #occupancyRegionResponses} value
	 *
	 * @param occupancyRegionResponses new value of {@link #occupancyRegionResponses}
	 */
	public void setOccupancyRegionResponses(OccupancyRegionResponse[] occupancyRegionResponses) {
		this.occupancyRegionResponses = occupancyRegionResponses;
	}

	@Override
	public String toString() {
		return "OccupancyRegionWrapper{" +
				"occupancyRegionResponses=" + Arrays.toString(occupancyRegionResponses) +
				'}';
	}
}
