/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.sembient.sembient.dto.region;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * RegionWrapper class - A wrapper class contain information about:
 * <ol>
 *   <li>Status code</li>
 *   <li>List of {@link RegionResponse}</li>
 * </ol>
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 9/30/2022
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RegionWrapper {
	private String statusCode;
	@JsonAlias("body")
	private RegionResponse[] regionResponse;

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
	 * Retrieves {@link #regionResponse}
	 *
	 * @return value of {@link #regionResponse}
	 */
	public RegionResponse[] getRegionResponse() {
		return regionResponse;
	}

	/**
	 * Sets {@link #regionResponse} value
	 *
	 * @param regionResponse new value of {@link #regionResponse}
	 */
	public void setRegionResponse(RegionResponse[] regionResponse) {
		this.regionResponse = regionResponse;
	}

	@Override
	public String toString() {
		return "RegionWrapper{" +
				"statusCode='" + statusCode + '\'' +
				", regionResponse=" + Arrays.toString(regionResponse) +
				'}';
	}
}
