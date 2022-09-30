/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.sembient.sembient.utils;

/**
 * SembientAggregatorConstant
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 9/29/2022
 * @since 1.0.0
 */
public class SembientAggregatorConstant {

	public static final String TVOCVALUE_MICROGRAM = "TVOCValue(microgram/m3)";
	public static final String PM_25_VALUE_MICROMET = "PM25Value(micromet)";
	public static final long MINUTE_TO_MS = 60000L;

	/**
	 * private constructor to prevent instance initialization
	 */
	private SembientAggregatorConstant() {
	}

	public static final String COMMA = ",";
	public static final String SLASH = "/";
	public static final String YYYY_MM_DD = "yyyy-MM-dd";
	public static final String UTC_TIMEZONE = "UTC";
	public static final String DASH = "-";
	public static final String LABEL_CREATE = "Create";
	public static final String AIR_QUALITY = "AirQuality";
	public static final String SENSOR = "Sensor";
	public static final String DATE_ISO_FORMAT = "dd MMM yyyy HH:mm:ss:SSS Z";
	public static final String HASH = "#";
	public static final String OCCUPANCY_LIST = "OccupancyList";
	public static final String HOUR = "Hour";
	public static final String REGION_TAG_NEW_TAG = "RegionTag#NewTag";
	public static final String REGION_TAG_CREATE = "RegionTag#Create";
	public static final String LABEL_PRESSED_CREATING = "Creating";
	public static final String LABEL_PRESSED_DELETING = "Deleting";
	public static final String LABEL_DELETE = "Delete";
	public static final String PROPERTY_TAG = "RegionTag#Tag";
	public static final String PROPERTY_DELETE = "RegionTag#Delete";
	public static final String THERMAL = "Thermal";
	public static final String NO_DATA = "No data";
	public static final String PROPERTY_MESSAGE = "OccupancyList#Message";
	public static final String PROPERTY_NUMBER_OF_OCCUPANCE = "OccupancyList#NumberOfOccupance";
	public static final String PROPERTY_USAGE_TIME = "OccupancyList#UsageTime";
	public static final String PROPERTY_HOUR = "OccupancyList#Hour";
	public static final String PROPERTY_CURRENT_DATE = "OccupancyList#CurrentDate";
	public static final String EMPTY = "";
	public static final String AUTHORIZATION = "Authorization";
	public static final String BASIC_AUTH_SCHEME = "Basic ";
	public static final String ACCEPT_HEADER = "Accept";
	public static final String CONTENT_TYPE_HEADER = "Content-Type";
	public static final String APPLICATION_JSON = "application/json";
	public static final String X_API_KEY_HEADER = "x-api-key";
	public static final String AUTH_TYPE_BEARER = "Bearer ";
	public static final String BUILDING = "Building";
	public static final String ADDRESS = "Address";
	public static final String BUILDING_ID = "BuildingId";
	public static final String CURRENT_FILTER_BUILDING = "CurrentFilterBuilding";
	public static final String NO_BUILDING_FOUND = "No building found";
	public static final String NEW_TAG = "NewTag";
	public static final String REGION_TAG = "RegionTag";
	public static final String TAG = "Tag";
	public static final String REGION = "Region";
	public static final String DEFAULT_WORK_HOUR = "8";
	public static final String TEMPERATURE_F = "Temperature(F)";
	public static final String HUMIDITY = "Humidity(%)";
	public static final String RECENT_DATA = "RecentData";
	public static final String LAST_UPDATE = "LastUpdate";
	public static final String MESSAGE = "Message";
	public static final String STATUS_CODE_200 = "200";
	public static final String CO2_VALUE = "CO2Value(C)";
}
