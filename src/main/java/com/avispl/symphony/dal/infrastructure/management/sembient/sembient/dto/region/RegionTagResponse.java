package com.avispl.symphony.dal.infrastructure.management.sembient.sembient.dto.region;

public class RegionTagResponse {
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
