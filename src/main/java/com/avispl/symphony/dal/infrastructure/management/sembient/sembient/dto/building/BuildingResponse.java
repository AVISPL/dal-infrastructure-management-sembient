package com.avispl.symphony.dal.infrastructure.management.sembient.sembient.dto.building;

import java.util.Arrays;

public class BuildingResponse {
	private String buildingID;
	private String buildingName;
	private String address;
	private String[] floors;
	private String[] regionTypes;

	/**
	 * Retrieves {@link #buildingID}
	 *
	 * @return value of {@link #buildingID}
	 */
	public String getBuildingID() {
		return buildingID;
	}

	/**
	 * Sets {@link #buildingID} value
	 *
	 * @param buildingID new value of {@link #buildingID}
	 */
	public void setBuildingID(String buildingID) {
		this.buildingID = buildingID;
	}

	/**
	 * Retrieves {@link #buildingName}
	 *
	 * @return value of {@link #buildingName}
	 */
	public String getBuildingName() {
		return buildingName;
	}

	/**
	 * Sets {@link #buildingName} value
	 *
	 * @param buildingName new value of {@link #buildingName}
	 */
	public void setBuildingName(String buildingName) {
		this.buildingName = buildingName;
	}

	/**
	 * Retrieves {@link #address}
	 *
	 * @return value of {@link #address}
	 */
	public String getAddress() {
		return address;
	}

	/**
	 * Sets {@link #address} value
	 *
	 * @param address new value of {@link #address}
	 */
	public void setAddress(String address) {
		this.address = address;
	}

	/**
	 * Retrieves {@link #floors}
	 *
	 * @return value of {@link #floors}
	 */
	public String[] getFloors() {
		return floors;
	}

	/**
	 * Sets {@link #floors} value
	 *
	 * @param floors new value of {@link #floors}
	 */
	public void setFloors(String[] floors) {
		this.floors = floors;
	}

	/**
	 * Retrieves {@link #regionTypes}
	 *
	 * @return value of {@link #regionTypes}
	 */
	public String[] getRegionTypes() {
		return regionTypes;
	}

	/**
	 * Sets {@link #regionTypes} value
	 *
	 * @param regionTypes new value of {@link #regionTypes}
	 */
	public void setRegionTypes(String[] regionTypes) {
		this.regionTypes = regionTypes;
	}

	@Override
	public String toString() {
		return "BuildingResponse{" +
				"buildingID='" + buildingID + '\'' +
				", buildingName='" + buildingName + '\'' +
				", address='" + address + '\'' +
				", floors=" + Arrays.toString(floors) +
				", regionTypes=" + Arrays.toString(regionTypes) +
				'}';
	}
}
