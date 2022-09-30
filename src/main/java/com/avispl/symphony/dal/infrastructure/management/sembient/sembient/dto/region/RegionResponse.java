/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.sembient.sembient.dto.region;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * RegionResponse class
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 9/30/2022
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RegionResponse {
	private String regionName;
	private String regionType;
	private String capacity;
	private String[] sensors;
	private String[] regionTags;

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
	 * Retrieves {@link #regionType}
	 *
	 * @return value of {@link #regionType}
	 */
	public String getRegionType() {
		return regionType;
	}

	/**
	 * Sets {@link #regionType} value
	 *
	 * @param regionType new value of {@link #regionType}
	 */
	public void setRegionType(String regionType) {
		this.regionType = regionType;
	}

	/**
	 * Retrieves {@link #capacity}
	 *
	 * @return value of {@link #capacity}
	 */
	public String getCapacity() {
		return capacity;
	}

	/**
	 * Sets {@link #capacity} value
	 *
	 * @param capacity new value of {@link #capacity}
	 */
	public void setCapacity(String capacity) {
		this.capacity = capacity;
	}

	/**
	 * Retrieves {@link #sensors}
	 *
	 * @return value of {@link #sensors}
	 */
	public String[] getSensors() {
		return sensors;
	}

	/**
	 * Sets {@link #sensors} value
	 *
	 * @param sensors new value of {@link #sensors}
	 */
	public void setSensors(String[] sensors) {
		this.sensors = sensors;
	}

	/**
	 * Retrieves {@link #regionTags}
	 *
	 * @return value of {@link #regionTags}
	 */
	public String[] getRegionTags() {
		return regionTags;
	}

	/**
	 * Sets {@link #regionTags} value
	 *
	 * @param regionTags new value of {@link #regionTags}
	 */
	public void setRegionTags(String[] regionTags) {
		this.regionTags = regionTags;
	}

	@Override
	public String toString() {
		return "RegionResponse{" +
				"regionName='" + regionName + '\'' +
				", regionType='" + regionType + '\'' +
				", capacity='" + capacity + '\'' +
				", sensors=" + Arrays.toString(sensors) +
				", regionTags=" + Arrays.toString(regionTags) +
				'}';
	}
}
