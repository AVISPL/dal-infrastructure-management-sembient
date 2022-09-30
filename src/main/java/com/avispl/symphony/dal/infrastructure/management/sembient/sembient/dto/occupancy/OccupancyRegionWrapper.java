/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.sembient.sembient.dto.occupancy;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * OccupancyRegionWrapper class
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 9/30/2022
 * @since 1.0.0
 */
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
