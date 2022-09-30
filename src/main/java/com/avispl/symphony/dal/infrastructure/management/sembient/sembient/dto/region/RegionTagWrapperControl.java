/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.sembient.sembient.dto.region;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * RegionTagWrapperControl class
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 9/30/2022
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RegionTagWrapperControl {
	private String statusCode;
	@JsonAlias("body")
	private RegionTagResponse regionResponse;

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
	public RegionTagResponse getRegionResponse() {
		return regionResponse;
	}

	/**
	 * Sets {@link #regionResponse} value
	 *
	 * @param regionResponse new value of {@link #regionResponse}
	 */
	public void setRegionResponse(RegionTagResponse regionResponse) {
		this.regionResponse = regionResponse;
	}
}
