package com.avispl.symphony.dal.infrastructure.management.sembient.sembient.dto.building;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonAlias;

import com.avispl.symphony.dal.infrastructure.management.sembient.sembient.dto.login.LoginResponse;

/**
 * Building wrapper class
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 8/24/2022
 * @since 1.0.0
 */
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
