/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.sembient.sembient.dto.building;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Building wrapper class - A class that provide information about:
 * <ol>
 *   <li>Status code</li>
 *   <li>Array of {@link BuildingResponse}</li>
 * </ol>
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 9/30/2022
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BuildingWrapper {
	private String statusCode;
	@JsonAlias("body")
	private BuildingResponse[] buildingResponse;

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
	 * Retrieves {@link #buildingResponse}
	 *
	 * @return value of {@link #buildingResponse}
	 */
	public BuildingResponse[] getBuildingResponse() {
		return buildingResponse;
	}

	/**
	 * Sets {@link #buildingResponse} value
	 *
	 * @param buildingResponse new value of {@link #buildingResponse}
	 */
	public void setBuildingResponse(BuildingResponse[] buildingResponse) {
		this.buildingResponse = buildingResponse;
	}

	@Override
	public String toString() {
		return "BuildingWrapper{" +
				"statusCode='" + statusCode + '\'' +
				", buildingResponse=" + Arrays.toString(buildingResponse) +
				'}';
	}
}
