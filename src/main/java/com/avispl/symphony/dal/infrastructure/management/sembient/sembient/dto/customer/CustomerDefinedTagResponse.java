/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.sembient.sembient.dto.customer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * CustomerDefinedTagResponse class - A class that provide information about region name and tags
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 9/30/2022
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomerDefinedTagResponse {
	private String regionName;
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
}
