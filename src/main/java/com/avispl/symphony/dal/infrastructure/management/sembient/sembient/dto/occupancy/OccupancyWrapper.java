/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.sembient.sembient.dto.occupancy;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * OccupancyWrapper class
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 9/30/2022
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OccupancyWrapper {
	private String statusCode;

	@JsonAlias("body")
	private OccupancyRegionWrapper occupancyRegionWrappers;

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
	 * Retrieves {@link #occupancyRegionWrappers}
	 *
	 * @return value of {@link #occupancyRegionWrappers}
	 */
	public OccupancyRegionWrapper getOccupancyRegionWrappers() {
		return occupancyRegionWrappers;
	}

	/**
	 * Sets {@link #occupancyRegionWrappers} value
	 *
	 * @param occupancyRegionWrappers new value of {@link #occupancyRegionWrappers}
	 */
	public void setOccupancyRegionWrappers(OccupancyRegionWrapper occupancyRegionWrappers) {
		this.occupancyRegionWrappers = occupancyRegionWrappers;
	}

	@Override
	public String toString() {
		return "OccupancyWrapper{" +
				"statusCode='" + statusCode + '\'' +
				", occupancyRegionWrappers=" + occupancyRegionWrappers +
				'}';
	}
}
