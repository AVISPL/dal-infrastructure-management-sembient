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

	/**
	 * private constructor to prevent instance initialization
	 */
	private SembientAggregatorConstant() {
	}

	public static final String COMMA = ",";
	public static final String SLASH = "/";
	public static final String YYYY_MM_DD = "yyyy-MM-dd";
	public static final String UTC_TIMEZONE = "UTC";
	public static final String CREATE_NEW_TAG = "CreateNewTag";
	public static final String DELETE_SELECTED_TAG = "DeleteSelectedTag";
	public static final String DASH = "-";
	public static final String LABEL_CREATE = "Create";
	public static final String AIR_QUALITY = "AirQuality";
	public static final String SENSOR = "Sensor";
	public static final String DATE_ISO_FORMAT = "dd MMM yyyy HH:mm:ss:SSS Z";
	public static final String HASH = "#";
	public static final String OCCUPANCY_LIST = "OccupancyList";
	public static final String HOUR = "Hour";
	public static final int DEFAULT_INSTALLATION_LAYOUT_POLLING_CYCLE = 10;
	public static final int DEFAULT_POLLING_CYCLE = 30;
	public static final String REGION_TAG_NEW_TAG = "RegionTags#NewTag";
	public static final String REGION_TAG_CREATE = "RegionTags#CreateNewTag";
	public static final String LABEL_PRESSED_CREATING = "Creating";
	public static final String LABEL_PRESSED_DELETING = "Deleting";
	public static final String LABEL_DELETE = "Delete";
	public static final String PROPERTY_TAG = "RegionTags#Tag";
	public static final String PROPERTY_DELETE = "RegionTags#DeleteSelectedTag";
	public static final String THERMAL = "Thermal";
	public static final String NO_DATA = "No data";
	public static final String PROPERTY_MESSAGE = "OccupancyList#Message";
	public static final String PROPERTY_NUMBER_OF_OCCUPANTS = "OccupancyList#NumberOfOccupants";
	public static final String PROPERTY_OCCUPANCY = "OccupancyList#Occupancy(%)";
	public static final String PROPERTY_USAGE_TIME_IN_MINUTE = "OccupancyList#UsageTime(minute)";
	public static final String PROPERTY_USAGE_TIME_IN_PERCENT = "OccupancyList#UsageTime(%)";
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
	public static final String REGION_TAG = "RegionTags";
	public static final String TAG = "Tag";
	public static final String REGION = "Region";
	public static final String DEFAULT_WORK_HOUR = "8";
	public static final String TEMPERATURE_LATEST_F = "TemperatureLatest(F)";
	public static final String HUMIDITY_LATEST = "HumidityLatest(%)";
	public static final String RECENT_DATA = "RecentData";
	public static final String LAST_UPDATE = "LastUpdate";
	public static final String MESSAGE = "Message";
	public static final String STATUS_CODE_200 = "200";
	public static final String CO2_VALUE_LATEST = "CO2Latest(ppm)";
	public static final String FLOOR_PROPERTY = "Floor%02d";
	public static final String BUILDING_PROPERTY = "Buildings#Building%02d";
	public static final String COLON = ":";
	public static final String TVOC_VALUE_LATEST_MICROGRAM = "TVOCLatest(microgram/m3)";
	public static final String PM_25_VALUE_LATEST_MICROMET = "PM2.5Latest(microgram/m3)";
	public static final long MINUTE_TO_MS = 60000L;
	public static final String NEXT_INSTALLATION_LAYOUT_POLLING_CYCLE = "NextInstallationLayoutPollingCycle";
	public static final int MAXIMUM_RETRY = 10;
	public static final int MAX_NO_THREADS = 8;
	public static final String STATUS_CODE_401 = "401";
	// Parameter constants
	public static final String PARAM_REGION_NAME = "?regionName=";
	public static final String PARAM_REGION_TAGS = "&regionTags=";
	public static final String PARAM_REGION_TYPE = "?regionType=";
	// Work hours constants
	public static final String WORK_HOUR_9 = "9";
	public static final String WORK_HOUR_10 = "10";
	public static final String WORK_HOUR_11 = "11";
	public static final String WORK_HOUR_12 = "12";
	public static final String WORK_HOUR_13 = "13";
	public static final String WORK_HOUR_14 = "14";
	public static final String WORK_HOUR_15 = "15";
	public static final String WORK_HOUR_16 = "16";
	public static final String WORK_HOUR_17 = "17";
	// Command constants
	public static final String COMMAND_SPACE_TAGS = "/v3.1/space/tags/";
	public static final String COMMAND_USERS_LOGIN = "/v3.1/users/login";
	public static final String COMMAND_SPACE_BUILDINGS = "/v3.1/space/buildings/";
	public static final String COMMAND_SPACE_REGIONS = "/v3.1/space/regions/";
	public static final String COMMAND_IAQ_TIMESERIES = "/v3.1/iaq/timeseries/";
	public static final String COMMAND_THERMAL_TIMESERIES = "/v3.1/thermal/timeseries/";
	public static final String COMMAND_OCCUPANCY_TIMESERIES = "/v3.1/occupancy/timeseries/";
	public static final String BUILDING_NAME = "BuildingName";
	public static final String FLOOR_NAME = "FloorName";
	public static final String REGIONS = "Regions";
	public static final String CAPACITY = "Capacity";
	public static final String REGION_TYPE = "RegionType";
	public static final String TEMPERATURE_AVG_F = "TemperatureAvg(F)";
	public static final String TEMPERATURE_MAX_F = "TemperatureMax(F)";
	public static final String TEMPERATURE_MIN_F = "TemperatureMin(F)";
	public static final String HUMIDITY_AVG = "HumidityAvg(%)";
	public static final String HUMIDITY_MAX = "HumidityMax(%)";
	public static final String HUMIDITY_MIN = "HumidityMin(%)";
	public static final String FROM_TIME = "FromTime";
	public static final String TO_TIME = "ToTime";
	public static final String NEXT_POLLING_CYCLE = "NextPollingCycle";
	public static final String FLOAT_WITH_TWO_DECIMAL = "%.2f";
}
