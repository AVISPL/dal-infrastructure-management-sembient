/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.sembient.sembient;

import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import javax.security.auth.login.FailedLoginException;

import com.avispl.symphony.api.dal.control.Controller;
import com.avispl.symphony.api.dal.dto.control.AdvancedControllableProperty;
import com.avispl.symphony.api.dal.dto.control.AdvancedControllableProperty.DropDown;
import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.Statistics;
import com.avispl.symphony.api.dal.dto.monitor.aggregator.AggregatedDevice;
import com.avispl.symphony.api.dal.error.CommandFailureException;
import com.avispl.symphony.api.dal.monitor.Monitorable;
import com.avispl.symphony.api.dal.monitor.aggregator.Aggregator;
import com.avispl.symphony.dal.communicator.RestCommunicator;
import com.avispl.symphony.dal.infrastructure.management.sembient.sembient.dto.airquality.AirQualityData;
import com.avispl.symphony.dal.infrastructure.management.sembient.sembient.dto.airquality.AirQualitySensorResponse;
import com.avispl.symphony.dal.infrastructure.management.sembient.sembient.dto.airquality.AirQualityWrapper;
import com.avispl.symphony.dal.infrastructure.management.sembient.sembient.dto.building.BuildingResponse;
import com.avispl.symphony.dal.infrastructure.management.sembient.sembient.dto.building.BuildingWrapper;
import com.avispl.symphony.dal.infrastructure.management.sembient.sembient.dto.login.LoginResponse;
import com.avispl.symphony.dal.infrastructure.management.sembient.sembient.dto.login.LoginWrapper;
import com.avispl.symphony.dal.infrastructure.management.sembient.sembient.dto.occupancy.OccupancyData;
import com.avispl.symphony.dal.infrastructure.management.sembient.sembient.dto.occupancy.OccupancyRegionResponse;
import com.avispl.symphony.dal.infrastructure.management.sembient.sembient.dto.occupancy.OccupancyWrapper;
import com.avispl.symphony.dal.infrastructure.management.sembient.sembient.dto.region.RegionResponse;
import com.avispl.symphony.dal.infrastructure.management.sembient.sembient.dto.region.RegionTagWrapperControl;
import com.avispl.symphony.dal.infrastructure.management.sembient.sembient.dto.region.RegionTagWrapperMonitor;
import com.avispl.symphony.dal.infrastructure.management.sembient.sembient.dto.region.RegionWrapper;
import com.avispl.symphony.dal.infrastructure.management.sembient.sembient.dto.thermal.ThermalData;
import com.avispl.symphony.dal.infrastructure.management.sembient.sembient.dto.thermal.ThermalSensorResponse;
import com.avispl.symphony.dal.infrastructure.management.sembient.sembient.dto.thermal.ThermalWrapper;
import com.avispl.symphony.dal.infrastructure.management.sembient.sembient.utils.SembientAggregatorConstant;
import com.avispl.symphony.dal.util.StringUtils;

/**
 * SembientAggregatorCommunicator
 * An implementation of RestCommunicator to provide communication and interaction with Sembient cloud and its aggregated devices
 * Supported aggregated device categories are sensors
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 9/12/2022
 * @since 1.0.0
 */
public class SembientAggregatorCommunicator extends RestCommunicator implements Aggregator, Monitorable, Controller {

	/**
	 * Process is running constantly and triggers collecting data from Sembient API endpoints
	 * - Building & floor information will be fetched every {@link SembientAggregatorCommunicator#installationLayoutPollingCycle}
	 *
	 * @author Kevin
	 * @since 1.0.0
	 */
	class SembientDeviceDataLoader implements Runnable {

		private volatile boolean inProgress;

		/**
		 * Parameters constructors
		 */
		public SembientDeviceDataLoader() {
			inProgress = true;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void run() {
			mainloop:
			while (inProgress) {
				try {
					TimeUnit.MILLISECONDS.sleep(200);
				} catch (InterruptedException e) {
					// Ignore for now
				}
				if (!inProgress) {
					break mainloop;
				}
				// Wait for getMultipleStatistics() to collect building & floor information first
				if (cachedBuildings.size() == 0) {
					continue mainloop;
				}
				// next line will determine whether Sembient monitoring was paused
				updateAggregatorStatus();
				if (devicePaused) {
					continue mainloop;
				}
				try {
					if (logger.isDebugEnabled()) {
						logger.debug("Fetching region & sensor list");
					}
					fetchDevicesList();
					if (logger.isDebugEnabled()) {
						logger.debug("Fetched region & sensor list: " + aggregatedDevices);
					}
				} catch (Exception e) {
					logger.error("Error occurred during region & sensor list retrieval: " + e.getMessage() + " with cause: " + e.getCause().getMessage(), e);
				}
				if (!inProgress) {
					break mainloop;
				}
				int aggregatedDevicesCount = aggregatedDevices.size();
				if (aggregatedDevicesCount == 0) {
					continue mainloop;
				}
				if (nextDevicesCollectionIterationTimestamp > System.currentTimeMillis()) {
					continue;
				}
				for (AggregatedDevice aggregatedDevice : aggregatedDevices.values()) {
					if (!inProgress) {
						break;
					}
					devicesExecutionPool.add(executorService.submit(() -> {
						try {
							populateRegionDetails(aggregatedDevice);
						} catch (Exception e) {
							logger.error(String.format("Exception during Sembient '%s' data processing.", aggregatedDevice.getDeviceName()), e);
						}
					}));
				}
				do {
					try {
						TimeUnit.MILLISECONDS.sleep(200);
					} catch (InterruptedException e) {
						if (!inProgress) {
							break;
						}
					}
					devicesExecutionPool.removeIf(Future::isDone);
				} while (!devicesExecutionPool.isEmpty());
				// We don't want to fetch devices statuses too often, so by default it's currentTime + pollingCycle
				if (StringUtils.isNotNullOrEmpty(pollingCycle)) {
					long interval;
					try {
						int pollingInInt = Integer.parseInt(pollingCycle);
						if (pollingInInt < SembientAggregatorConstant.DEFAULT_POLLING_CYCLE) {
							pollingInInt = SembientAggregatorConstant.DEFAULT_POLLING_CYCLE;
						}
						interval = pollingInInt * SembientAggregatorConstant.MINUTE_TO_MS;
					} catch (Exception e) {
						logger.error("Invalid format, pollingCycle should be integer.", e);
						interval = SembientAggregatorConstant.DEFAULT_POLLING_CYCLE * SembientAggregatorConstant.MINUTE_TO_MS;
					}
					nextDevicesCollectionIterationTimestamp = System.currentTimeMillis() + interval;
				} else {
					nextDevicesCollectionIterationTimestamp = System.currentTimeMillis() + SembientAggregatorConstant.DEFAULT_POLLING_CYCLE * SembientAggregatorConstant.MINUTE_TO_MS;
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Finished collecting devices statistics cycle at " + new Date());
				}
			}
			// Finished collecting
		}

		/**
		 * Triggers main loop to stop
		 */
		public void stop() {
			inProgress = false;
		}

	}

	/**
	 * Update the status of the device.
	 * The device is considered as paused if did not receive any retrieveMultipleStatistics()
	 * calls during {@link SembientAggregatorCommunicator#validRetrieveStatisticsTimestamp}
	 */
	private synchronized void updateAggregatorStatus() {
		devicePaused = validRetrieveStatisticsTimestamp < System.currentTimeMillis();
	}

	/**
	 * Uptime time stamp to valid one
	 */
	private synchronized void updateValidRetrieveStatisticsTimestamp() {
		validRetrieveStatisticsTimestamp = System.currentTimeMillis() + retrieveStatisticsTimeOut;
		updateAggregatorStatus();
	}

	/**
	 * This parameter holds timestamp of when we need to stop performing API calls
	 * It used when device stop retrieving statistic. Updated each time of called #retrieveMultipleStatistics
	 */
	private volatile long validRetrieveStatisticsTimestamp;

	/**
	 * Indicates whether a device is considered as paused.
	 * True by default so if the system is rebooted and the actual value is lost -> the device won't start stats
	 * collection unless the {@link SembientAggregatorCommunicator#retrieveMultipleStatistics()} method is called which will change it
	 * to a correct value
	 */
	private volatile boolean devicePaused = true;

	/**
	 * Indicates whether a building and floor information(renew every {@link SembientAggregatorCommunicator#installationLayoutPollingCycle} minutes) is latest or not
	 */
	private volatile boolean latestBuildingAndFloorData = false;

	/**
	 * Aggregator inactivity timeout. If the {@link SembientAggregatorCommunicator#retrieveMultipleStatistics()}  method is not
	 * called during this period of time - device is considered to be paused, thus the Cloud API
	 * is not supposed to be called
	 */
	private static final long retrieveStatisticsTimeOut = 3 * 60 * 1000;

	/**
	 * We don't want the statistics to be collected constantly, because if there's not a big list of devices -
	 * new devices' statistics loop will be launched before the next monitoring iteration. To avoid that -
	 * this variable stores a timestamp which validates it, so when the devices' statistics is done collecting, variable
	 * is set to currentTime + 30s, at the same time, calling {@link #retrieveMultipleStatistics()} and updating the
	 * {@link #aggregatedDevices} resets it to the currentTime timestamp, which will re-activate data collection.
	 */
	private volatile long nextDevicesCollectionIterationTimestamp;

	/**
	 * Executor that runs all the async operations, that {@link #deviceDataLoader} is posting and
	 * {@link #devicesExecutionPool} is keeping track of
	 */
	private static ExecutorService executorService;

	/**
	 * Runner service responsible for collecting data and posting processes to {@link #devicesExecutionPool}
	 */
	private SembientDeviceDataLoader deviceDataLoader;

	/**
	 * Pool for keeping all the async operations in, to track any operations in progress and cancel them if needed
	 */
	private final List<Future> devicesExecutionPool = new ArrayList<>();


	/**
	 * Set of {@link BuildingResponse} - data will be fetched in worker thread, and later be used to populate
	 * in {@link SembientAggregatorCommunicator#getMultipleStatistics()}
	 */
	Set<BuildingResponse> cachedBuildings = ConcurrentHashMap.newKeySet();

	/**
	 * Devices this aggregator is responsible for
	 * Data is cached and retrieved every {@link #pollingCycle}
	 */
	private final ConcurrentHashMap<String, AggregatedDevice> aggregatedDevices = new ConcurrentHashMap<>();

	/**
	 * Time period within which the device metadata (basic devices' information) cannot be refreshed.
	 * Ignored if device list is not yet retrieved or the cached device list is empty {@link SembientAggregatorCommunicator#aggregatedDevices}
	 */
	private volatile long validBuildingAndFloorMetaDataRetrievalPeriodTimestamp;

	/**
	 * Whether service is running.
	 */
	private volatile boolean serviceRunning;

	/**
	 * ReentrantLock
	 */
	private final ReentrantLock reentrantLock = new ReentrantLock();

	private LoginResponse loginResponse;

	/**
	 * Map with key is device id and value is value of hour (8-17) in the dropdown list.
	 */
	private final ConcurrentHashMap<String, String> aggregatedDeviceHourMap = new ConcurrentHashMap<>();

	/**
	 * Map with key is device id and value is value of {@link OccupancyData} in the dropdown list.
	 */
	private final ConcurrentHashMap<String, OccupancyData[]> aggregatedDeviceOccupancyMap = new ConcurrentHashMap<>();

	/**
	 * Map with key is device id and value is value of tag in the dropdown list.
	 */
	private final ConcurrentHashMap<String, String> aggregatedDeviceTagMap = new ConcurrentHashMap<>();

	/**
	 * Map with key is device id and value is value of new tag.
	 */
	private final ConcurrentHashMap<String, String> lastNewTag = new ConcurrentHashMap<>();

	// Adapter properties

	/**
	 * String of building to be filtered
	 */
	private String buildingFilter;

	/**
	 * List of floors to be filtered
	 */
	private String floorFilter;

	/**
	 * List of region names to be filtered
	 */
	private String deviceNameFilter;

	/**
	 * List of device types to be filtered
	 */
	private String deviceTypeFilter;

	/**
	 * List of region types to be filtered
	 */
	private String regionTypeFilter;

	/**
	 * Property that define when will the adapter fetch new data of building, floor, devices
	 * then store to {@link SembientAggregatorCommunicator#cachedBuildings}
	 */
	private String installationLayoutPollingCycle;

	/**
	 * Property that define when will the adapter fetch new data of Thermal, Airquality, Occupancy
	 * then store to {@link SembientAggregatorCommunicator#cachedBuildings}
	 */
	private String pollingCycle;

	/**
	 * Property that define when will the adapter fetch new data of Thermal, Airquality, Occupancy  and get too many request error
	 * then store to {@link SembientAggregatorCommunicator#cachedBuildings}
	 */
	private String retryInterval;

	/**
	 * Property that define when will the adapter fetch new data of Thermal, Airquality, Occupancy  and get too many request error
	 * then store to {@link SembientAggregatorCommunicator#cachedBuildings}
	 */
	private String numberOfRetry;


	/**
	 * Stored too many request error endpoint
	 */
	private Set<String> cachedTooManyRequestError = ConcurrentHashMap.newKeySet();


	/**
	 * Retrieves {@link #regionTypeFilter}
	 *
	 * @return value of {@link #regionTypeFilter}
	 */
	public String getRegionTypeFilter() {
		return regionTypeFilter;
	}

	/**
	 * Sets {@link #regionTypeFilter} value
	 *
	 * @param regionTypeFilter new value of {@link #regionTypeFilter}
	 */
	public void setRegionTypeFilter(String regionTypeFilter) {
		this.regionTypeFilter = regionTypeFilter;
	}

	/**
	 * Retrieves {@link #pollingCycle}
	 *
	 * @return value of {@link #pollingCycle}
	 */
	public String getPollingCycle() {
		return pollingCycle;
	}

	/**
	 * Sets {@link #pollingCycle} value
	 *
	 * @param pollingCycle new value of {@link #pollingCycle}
	 */
	public void setPollingCycle(String pollingCycle) {
		this.pollingCycle = pollingCycle;
	}

	/**
	 * Retrieves {@link #buildingFilter}
	 *
	 * @return value of {@link #buildingFilter}
	 */
	public String getBuildingFilter() {
		return buildingFilter;
	}

	/**
	 * Sets {@link #buildingFilter} value
	 *
	 * @param buildingFilter new value of {@link #buildingFilter}
	 */
	public void setBuildingFilter(String buildingFilter) {
		this.buildingFilter = buildingFilter;
	}

	/**
	 * Retrieves {@link #floorFilter}
	 *
	 * @return value of {@link #floorFilter}
	 */
	public String getFloorFilter() {
		return floorFilter;
	}

	/**
	 * Sets {@link #floorFilter} value
	 *
	 * @param floorFilter new value of {@link #floorFilter}
	 */
	public void setFloorFilter(String floorFilter) {
		this.floorFilter = floorFilter;
	}

	/**
	 * Retrieves {@link #deviceNameFilter}
	 *
	 * @return value of {@link #deviceNameFilter}
	 */
	public String getDeviceNameFilter() {
		return deviceNameFilter;
	}

	/**
	 * Sets {@link #deviceNameFilter} value
	 *
	 * @param deviceNameFilter new value of {@link #deviceNameFilter}
	 */
	public void setDeviceNameFilter(String deviceNameFilter) {
		this.deviceNameFilter = deviceNameFilter;
	}

	/**
	 * Retrieves {@link #deviceTypeFilter}
	 *
	 * @return value of {@link #deviceTypeFilter}
	 */
	public String getDeviceTypeFilter() {
		return deviceTypeFilter;
	}

	/**
	 * Sets {@link #deviceTypeFilter} value
	 *
	 * @param deviceTypeFilter new value of {@link #deviceTypeFilter}
	 */
	public void setDeviceTypeFilter(String deviceTypeFilter) {
		this.deviceTypeFilter = deviceTypeFilter;
	}

	/**
	 * Retrieves {@link #installationLayoutPollingCycle}
	 *
	 * @return value of {@link #installationLayoutPollingCycle}
	 */
	public String getInstallationLayoutPollingCycle() {
		return installationLayoutPollingCycle;
	}

	/**
	 * Sets {@link #installationLayoutPollingCycle} value
	 *
	 * @param installationLayoutPollingCycle new value of {@link #installationLayoutPollingCycle}
	 */
	public void setInstallationLayoutPollingCycle(String installationLayoutPollingCycle) {
		this.installationLayoutPollingCycle = installationLayoutPollingCycle;
	}

	/**
	 * Retrieves {@link #loginResponse}
	 *
	 * @return value of {@link #loginResponse}
	 */
	public LoginResponse getLoginResponse() {
		return loginResponse;
	}

	/**
	 * Sets {@link #loginResponse} value
	 *
	 * @param loginResponse new value of {@link #loginResponse}
	 */
	public void setLoginResponse(LoginResponse loginResponse) {
		this.loginResponse = loginResponse;
	}

	private String tempResponse;

	/**
	 * Retrieves {@link #tempResponse}
	 *
	 * @return value of {@link #tempResponse}
	 */
	public String getTempResponse() {
		return tempResponse;
	}

	/**
	 * Sets {@link #tempResponse} value
	 *
	 * @param tempResponse new value of {@link #tempResponse}
	 */
	public void setTempResponse(String tempResponse) {
		this.tempResponse = tempResponse;
	}

	/**
	 * Retrieves {@link #retryInterval}
	 *
	 * @return value of {@link #retryInterval}
	 */
	public String getRetryInterval() {
		return retryInterval;
	}

	/**
	 * Sets {@link #retryInterval} value
	 *
	 * @param retryInterval new value of {@link #retryInterval}
	 */
	public void setRetryInterval(String retryInterval) {
		this.retryInterval = retryInterval;
	}

	/**
	 * Retrieves {@link #numberOfRetry}
	 *
	 * @return value of {@link #numberOfRetry}
	 */
	public String getNumberOfRetry() {
		return numberOfRetry;
	}

	/**
	 * Sets {@link #numberOfRetry} value
	 *
	 * @param numberOfRetry new value of {@link #numberOfRetry}
	 */
	public void setNumberOfRetry(String numberOfRetry) {
		this.numberOfRetry = numberOfRetry;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void internalInit() throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("Internal init is called");
		}
		this.setTrustAllCertificates(true);
		// Init thread
		executorService = Executors.newFixedThreadPool(SembientAggregatorConstant.MAX_NO_THREADS);
		executorService.submit(deviceDataLoader = new SembientDeviceDataLoader());

		validBuildingAndFloorMetaDataRetrievalPeriodTimestamp = System.currentTimeMillis();
		serviceRunning = true;
		super.internalInit();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void internalDestroy() {
		loginResponse = null;
		if (logger.isDebugEnabled()) {
			logger.debug("Internal destroy is called.");
		}
		serviceRunning = false;
		validRetrieveStatisticsTimestamp = 0;
		validBuildingAndFloorMetaDataRetrievalPeriodTimestamp = 0;
		if (deviceDataLoader != null) {
			deviceDataLoader.stop();
			deviceDataLoader = null;
		}

		if (executorService != null) {
			executorService.shutdownNow();
			executorService = null;
		}

		devicesExecutionPool.forEach(future -> future.cancel(true));
		devicesExecutionPool.clear();
		aggregatedDeviceHourMap.clear();
		aggregatedDeviceTagMap.clear();
		aggregatedDeviceOccupancyMap.clear();
		cachedBuildings.clear();
		lastNewTag.clear();
		aggregatedDevices.clear();
		super.internalDestroy();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void controlProperty(ControllableProperty controllableProperty) {
		reentrantLock.lock();
		try {
			String deviceId = controllableProperty.getDeviceId();
			String[] properties = controllableProperty.getProperty().split(SembientAggregatorConstant.HASH);
			String groupName = properties[0];
			String propertyName = properties[1];
			AggregatedDevice deviceToBeControlled = aggregatedDevices.get(deviceId);
			String[] deviceDetails = deviceId.split(SembientAggregatorConstant.DASH);
			// Validate if device id is built base on <device type>-<customer id>-<building id>-<floor name>-<device name>
			int lastIndex = deviceDetails.length - 1;
			int previousIndex = lastIndex - 1;
			int lastTwoIndex = lastIndex - 2;
			List<String> floorNames = new ArrayList<>();
			List<String> buildingIDs = new ArrayList<>();
			String deviceName = deviceDetails[lastIndex];
			if (previousIndex < 0 || lastTwoIndex < 0) {
				throw new IllegalArgumentException("Failed to perform control operation with wrong device ID format.");
			}
			String floorName = deviceDetails[previousIndex];
			String buildingID = deviceDetails[lastTwoIndex];
			if (deviceName == null) {
				throw new IllegalArgumentException("Failed to perform control operation with wrong device ID format.");
			}
			for (BuildingResponse response : cachedBuildings) {
				buildingIDs.add(response.getBuildingID());
				floorNames.addAll(Arrays.asList(response.getFloors()));
			}
			if (!deviceToBeControlled.getDeviceName().equals(deviceName) || !floorNames.contains(floorName) || !buildingIDs.contains(buildingID)) {
				throw new IllegalArgumentException("Failed to perform control operation with wrong device ID format.");
			}
			//
			Map<String, String> statFromCached = deviceToBeControlled.getProperties();
			List<AdvancedControllableProperty> controlFromCached = deviceToBeControlled.getControllableProperties();
			if (SembientAggregatorConstant.OCCUPANCY_LIST.equals(groupName) && SembientAggregatorConstant.HOUR.equals(propertyName)) {
				String hourValue = (String) controllableProperty.getValue();
				aggregatedDeviceHourMap.put(deviceId, (String) controllableProperty.getValue());
				OccupancyData[] occupancyData = aggregatedDeviceOccupancyMap.get(deviceId);
				if (occupancyData == null) {
					throw new IllegalArgumentException("Failed to control OccupancyList, Hour dropdown.");
				}
				for (OccupancyData data : occupancyData) {
					if (hourValue.equals(data.getHour())) {
						statFromCached.put(SembientAggregatorConstant.PROPERTY_NUMBER_OF_OCCUPANTS, data.getOccupancy());
						String rawCapacity = statFromCached.get(SembientAggregatorConstant.CAPACITY);
						if (rawCapacity != null) {
							int capacity = Integer.parseInt(rawCapacity);
							float utilization = Integer.parseInt(data.getOccupancy()) / (float) capacity;
							statFromCached.put(SembientAggregatorConstant.PROPERTY_OCCUPANCY, String.format(SembientAggregatorConstant.FLOAT_WITH_TWO_DECIMAL, utilization * 100));
						}
						statFromCached.put(SembientAggregatorConstant.PROPERTY_USAGE_TIME_IN_MINUTE, data.getUsageTime());
						float usageTimeInPercentage = Integer.parseInt(data.getUsageTime()) / (float) 60;
						statFromCached.put(SembientAggregatorConstant.PROPERTY_USAGE_TIME_IN_PERCENT, String.format(SembientAggregatorConstant.FLOAT_WITH_TWO_DECIMAL, usageTimeInPercentage * 100));
						break;
					}
				}
				for (AdvancedControllableProperty control : controlFromCached) {
					if (control.getName().equals(controllableProperty.getProperty())) {
						control.setTimestamp(new Date());
						control.setValue(hourValue);
						break;
					}
				}
			} else if (SembientAggregatorConstant.REGION_TAG.equals(groupName)) {
				switch (propertyName) {
					case SembientAggregatorConstant.NEW_TAG:
						String newTagValue = (String) controllableProperty.getValue();
						lastNewTag.put(deviceId, newTagValue);
						statFromCached.put(SembientAggregatorConstant.REGION_TAG_NEW_TAG, newTagValue);
						for (AdvancedControllableProperty control : controlFromCached) {
							if (control.getName().equals(controllableProperty.getProperty())) {
								control.setTimestamp(new Date());
								control.setValue(newTagValue);
								break;
							}
						}
						break;
					case SembientAggregatorConstant.TAG:
						aggregatedDeviceTagMap.put(deviceId, (String) controllableProperty.getValue());
						statFromCached.put(SembientAggregatorConstant.PROPERTY_TAG, (String) controllableProperty.getValue());
						for (AdvancedControllableProperty control : controlFromCached) {
							if (SembientAggregatorConstant.PROPERTY_TAG.equals(control.getName())) {
								control.setTimestamp(new Date());
								control.setValue(controllableProperty.getValue());
								break;
							}
						}
						break;
					case SembientAggregatorConstant.CREATE_NEW_TAG:
						String newTag = lastNewTag.get(deviceId);
						if (StringUtils.isNullOrEmpty(newTag) || StringUtils.isNullOrEmpty(newTag.trim())) {
							throw new IllegalArgumentException("Cannot create new region tag with NewTag's value is empty or null or only space characters.");
						}
						StringBuilder createRequestBuilder = new StringBuilder();
						createRequestBuilder.append(SembientAggregatorConstant.COMMAND_SPACE_TAGS).append(loginResponse.getCustomerId()).append(SembientAggregatorConstant.SLASH).append(buildingID)
								.append(SembientAggregatorConstant.SLASH).append(floorName).append(SembientAggregatorConstant.PARAM_REGION_NAME).append(deviceName).append(SembientAggregatorConstant.PARAM_REGION_TAGS)
								.append(newTag);
						RegionTagWrapperControl createRegionTagWrapperControl = null;
						try {
							createRegionTagWrapperControl = this.doPut(createRequestBuilder.toString(), null, RegionTagWrapperControl.class);
						} catch (CommandFailureException e) {
							logger.error("Failed to create with status code: " + e.getStatusCode() + ", value: " + newTag, e);
							if (e.getStatusCode() == 429) {
								throw new IllegalStateException("Too many requests, please try to create region tag with value: " + newTag + " later.");
							} else {
								throw new IllegalStateException("Failed to create region tag with value: " + newTag);
							}
						} catch (Exception e) {
							logger.error("Exception occurred when creating region tag with value: " + newTag, e);
							throw new IllegalStateException("Failed to create region tag with value: " + newTag);
						}
						if (createRegionTagWrapperControl != null) {
							if (!SembientAggregatorConstant.STATUS_CODE_200.equals(createRegionTagWrapperControl.getStatusCode())) {
								throw new IllegalStateException("Failed to create region with value is: " + controllableProperty.getValue());
							}
							lastNewTag.put(deviceId, SembientAggregatorConstant.EMPTY);
							// get old tags
							List<String> newOptions = new ArrayList<>();
							for (AdvancedControllableProperty control : controlFromCached) {
								if (SembientAggregatorConstant.PROPERTY_TAG.equals(control.getName())) {
									newOptions = new LinkedList<>(Arrays.asList(((DropDown) control.getType()).getOptions()));
									break;
								}
							}
							// add new created tag
							if (!newOptions.contains(newTag)) {
								newOptions.add(newTag);
							}
							controlFromCached.removeIf(advancedControllableProperty -> advancedControllableProperty.getName().equals(SembientAggregatorConstant.PROPERTY_TAG));
							controlFromCached.removeIf(advancedControllableProperty -> advancedControllableProperty.getName().equals(SembientAggregatorConstant.REGION_TAG_NEW_TAG));
							controlFromCached.removeIf(advancedControllableProperty -> advancedControllableProperty.getName().equals(SembientAggregatorConstant.PROPERTY_DELETE));
							statFromCached.remove(SembientAggregatorConstant.PROPERTY_TAG);
							statFromCached.remove(SembientAggregatorConstant.REGION_TAG_NEW_TAG);
							statFromCached.remove(SembientAggregatorConstant.PROPERTY_DELETE);
							aggregatedDeviceTagMap.put(deviceId, newOptions.get(0));
							controlFromCached.add(createDropdown(statFromCached, SembientAggregatorConstant.PROPERTY_TAG, newOptions, newOptions.get(0)));
							controlFromCached.add(
									createButton(statFromCached, SembientAggregatorConstant.PROPERTY_DELETE, SembientAggregatorConstant.LABEL_DELETE, SembientAggregatorConstant.LABEL_PRESSED_DELETING));
							controlFromCached.add(createText(statFromCached, SembientAggregatorConstant.REGION_TAG_NEW_TAG, SembientAggregatorConstant.EMPTY));
						} else {
							logger.error("Error while creating region with status code: 429 and value " + newTag);
							throw new IllegalStateException("Too many requests sent to the device, please try to create one more time with value: " + newTag);
						}
						break;
					case SembientAggregatorConstant.DELETE_SELECTED_TAG:
						String valueToBeDelete = aggregatedDeviceTagMap.get(deviceId);
						if (StringUtils.isNullOrEmpty(valueToBeDelete)) {
							throw new IllegalArgumentException("Tag dropdowns value cannot be empty.");
						}
						String deleteRequest =
								SembientAggregatorConstant.COMMAND_SPACE_TAGS + loginResponse.getCustomerId() + SembientAggregatorConstant.SLASH + buildingID + SembientAggregatorConstant.SLASH + floorName
										+ SembientAggregatorConstant.PARAM_REGION_NAME + deviceName + SembientAggregatorConstant.PARAM_REGION_TAGS + valueToBeDelete;
						try {
							this.doDelete(deleteRequest);
							// Get old tags
							List<String> options = new ArrayList<>();
							for (AdvancedControllableProperty control : controlFromCached) {
								if (SembientAggregatorConstant.PROPERTY_TAG.equals(control.getName())) {
									options = new LinkedList<>(Arrays.asList(((DropDown) control.getType()).getOptions()));
									break;
								}
							}
							// Remove deleted tags
							options.remove(valueToBeDelete);
							if (options.size() == 0) {
								controlFromCached.removeIf(advancedControllableProperty -> advancedControllableProperty.getName().equals(SembientAggregatorConstant.PROPERTY_DELETE));
								statFromCached.remove(SembientAggregatorConstant.PROPERTY_DELETE);
								controlFromCached.removeIf(advancedControllableProperty -> advancedControllableProperty.getName().equals(SembientAggregatorConstant.PROPERTY_TAG));
								statFromCached.remove(SembientAggregatorConstant.PROPERTY_TAG);
								break;
							}
							controlFromCached.removeIf(advancedControllableProperty -> advancedControllableProperty.getName().equals(SembientAggregatorConstant.PROPERTY_TAG));
							statFromCached.remove(SembientAggregatorConstant.PROPERTY_TAG);
							aggregatedDeviceTagMap.put(deviceId, options.get(0));
							controlFromCached.add(createDropdown(statFromCached, SembientAggregatorConstant.PROPERTY_TAG, options, options.get(0)));
						} catch (CommandFailureException e) {
							logger.error("Failed to delete with status code: " + e.getStatusCode() + ", value: " + valueToBeDelete, e);
							if (e.getStatusCode() == 429) {
								throw new IllegalStateException("Too many requests, please try to delete with value: " + valueToBeDelete + " later.");
							} else {
								throw new IllegalStateException("Failed to delete region tag with value: " + valueToBeDelete);
							}
						} catch (Exception e) {
							logger.error("Exception occurred when deleting region tag with value: " + valueToBeDelete, e);
							throw new IllegalStateException("Failed to delete region tag with value: " + valueToBeDelete);
						}
						break;
					default:
						throw new IllegalStateException(String.format("Controlling group %s is not supported.", controllableProperty.getProperty()));
				}
			} else {
				throw new IllegalStateException(String.format("Controlling group %s is not supported.", controllableProperty.getProperty()));
			}
			deviceToBeControlled.setProperties(statFromCached);
			deviceToBeControlled.setControllableProperties(controlFromCached);
			aggregatedDevices.put(deviceId, deviceToBeControlled);
		} finally {
			reentrantLock.unlock();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void controlProperties(List<ControllableProperty> controllablePropertyList) throws Exception {
		if (CollectionUtils.isEmpty(controllablePropertyList)) {
			throw new IllegalArgumentException("Controllable properties cannot be null or empty");
		}
		for (ControllableProperty controllableProperty : controllablePropertyList) {
			controlProperty(controllableProperty);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Statistics> getMultipleStatistics() throws Exception {
		reentrantLock.lock();
		Map<String, String> newStatistics = new HashMap<>();
		ExtendedStatistics extendedStatistics = new ExtendedStatistics();
		try {
			// Login to get the token if token has expired.
			sembientLogin();
			// Fetch building
			fetchBuildings();
			// Put NextRefreshInterval properties to stats map
			DateFormat obj = new SimpleDateFormat(SembientAggregatorConstant.DATE_ISO_FORMAT);
			obj.setTimeZone(TimeZone.getTimeZone(SembientAggregatorConstant.UTC_TIMEZONE));
			newStatistics.put(SembientAggregatorConstant.NEXT_INSTALLATION_LAYOUT_POLLING_CYCLE, obj.format(validBuildingAndFloorMetaDataRetrievalPeriodTimestamp));
			if (nextDevicesCollectionIterationTimestamp == 0) {
				newStatistics.put(SembientAggregatorConstant.NEXT_POLLING_CYCLE, obj.format(new Date()));
			} else {
				newStatistics.put(SembientAggregatorConstant.NEXT_POLLING_CYCLE, obj.format(nextDevicesCollectionIterationTimestamp));
			}
			if (cachedBuildings != null && cachedBuildings.size() != 0) {
				BuildingResponse buildingResponse = null;
				if (StringUtils.isNotNullOrEmpty(buildingFilter)) {
					for (BuildingResponse building : cachedBuildings) {
						if (building.getBuildingName().equals(buildingFilter)) {
							buildingResponse = building;
							break;
						}
					}
				} else {
					buildingResponse = cachedBuildings.stream().findFirst().get();
				}
				if (buildingResponse != null) {
					newStatistics.put(SembientAggregatorConstant.CURRENT_FILTER_BUILDING, buildingResponse.getBuildingName());
				} else {
					newStatistics.put(SembientAggregatorConstant.CURRENT_FILTER_BUILDING, SembientAggregatorConstant.NO_BUILDING_FOUND);
				}
				int index = 0;
				for (BuildingResponse building : cachedBuildings) {
					newStatistics.put(String.format(SembientAggregatorConstant.BUILDING_PROPERTY, index + 1), building.getBuildingName());
					index++;
				}
				if (buildingResponse != null) {
					String buildingID = buildingResponse.getBuildingID();
					String[] floorNames = buildingResponse.getFloors();
					// Filter by floors
					newStatistics.put(SembientAggregatorConstant.BUILDING + buildingResponse.getBuildingName() + SembientAggregatorConstant.HASH + SembientAggregatorConstant.BUILDING_ID, buildingID);
					newStatistics.put(SembientAggregatorConstant.BUILDING + buildingResponse.getBuildingName() + SembientAggregatorConstant.HASH + SembientAggregatorConstant.ADDRESS,
							buildingResponse.getAddress());
					if (StringUtils.isNotNullOrEmpty(floorFilter)) {
						String[] floorFilters = floorFilter.split(SembientAggregatorConstant.COMMA);
						int i = 0;
						for (String filter : floorFilters) {
							for (String floorName : floorNames) {
								if (filter.trim().equals(floorName)) {
									i++;
									String floorIndex = String.format(SembientAggregatorConstant.HASH + SembientAggregatorConstant.FLOOR_PROPERTY, i);
									newStatistics.put(SembientAggregatorConstant.BUILDING + buildingResponse.getBuildingName() + floorIndex, floorName);
									break;
								}
							}
						}
					} else {
						int i = 0;
						for (String floorName : floorNames) {
							i++;
							String floorIndex = String.format(SembientAggregatorConstant.HASH + SembientAggregatorConstant.FLOOR_PROPERTY, i);
							newStatistics.put(SembientAggregatorConstant.BUILDING + buildingResponse.getBuildingName() + floorIndex, floorName);
						}
					}
				}
			}

			extendedStatistics.setStatistics(newStatistics);
		} finally {
			reentrantLock.unlock();
		}
		return Collections.singletonList(extendedStatistics);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<AggregatedDevice> retrieveMultipleStatistics() throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Adapter initialized: %s, executorService exists: %s, serviceRunning: %s", isInitialized(), executorService != null, serviceRunning));
		}
		if (executorService == null) {
			// Due to the bug that after changing properties on fly - the adapter is destroyed but adapter is not initialized properly,
			// so executor service is not running. We need to make sure executorService exists
			executorService = Executors.newFixedThreadPool(8);
			executorService.submit(deviceDataLoader = new SembientDeviceDataLoader());
		}
		updateValidRetrieveStatisticsTimestamp();
		return aggregatedDevices.values().stream().collect(Collectors.toList());
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Sembient api endpoint does not have ICMP enable, so this workaround is needed to provide
	 * ping latency information to Symphony
	 */
	@Override
	public int ping() {
		if (isInitialized()) {
			long pingResultTotal = 0L;

			for (int i = 0; i < this.getPingAttempts(); i++) {
				long startTime = System.currentTimeMillis();

				String[] hostSplit = this.getHost().split(SembientAggregatorConstant.DASH);
				String host = hostSplit[0];
				try (Socket puSocketConnection = new Socket(host, this.getPort())) {
					puSocketConnection.setSoTimeout(this.getPingTimeout());
					if (puSocketConnection.isConnected()) {
						long pingResult = System.currentTimeMillis() - startTime;
						pingResultTotal += pingResult;
						if (this.logger.isTraceEnabled()) {
							this.logger.trace(String.format("PING OK: Attempt #%s to connect to %s on port %s succeeded in %s ms", i + 1, host, this.getPort(), pingResult));
						}
					} else {
						if (this.logger.isDebugEnabled()) {
							this.logger.debug(String.format("PING DISCONNECTED: Connection to %s did not succeed within the timeout period of %sms", host, this.getPingTimeout()));
						}
						return this.getPingTimeout();
					}
				} catch (SocketTimeoutException | ConnectException tex) {
					if (this.logger.isDebugEnabled()) {
						this.logger.error(String.format("PING TIMEOUT: Connection to %s did not succeed within the timeout period of %sms", host, this.getPingTimeout()));
					}
					return this.getPingTimeout();
				} catch (Exception e) {
					if (this.logger.isDebugEnabled()) {
						this.logger.error(String.format("PING TIMEOUT: Connection to %s did not succeed, UNKNOWN ERROR %s: ", host, e.getMessage()));
					}
					return this.getPingTimeout();
				}
			}
			return Math.max(1, Math.toIntExact(pingResultTotal / this.getPingAttempts()));
		} else {
			throw new IllegalStateException("Cannot use device class without calling init() first");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<AggregatedDevice> retrieveMultipleStatistics(List<String> list) throws Exception {
		return retrieveMultipleStatistics().stream().filter(aggregatedDevice -> list.contains(aggregatedDevice.getDeviceId())).collect(Collectors.toList());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected HttpHeaders putExtraRequestHeaders(HttpMethod httpMethod, String uri, HttpHeaders headers) throws Exception {
		if (loginResponse != null) {
			headers.add(SembientAggregatorConstant.X_API_KEY_HEADER, loginResponse.getApiKey());
			headers.add(SembientAggregatorConstant.AUTHORIZATION, SembientAggregatorConstant.AUTH_TYPE_BEARER + loginResponse.getBearerToken());
		}
		return super.putExtraRequestHeaders(httpMethod, uri, headers);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void authenticate() {
		// Sembient have it own login command
	}

	/**
	 * Send login command to Sembient cloud, if the token is expired (> 1 hour) then re-login is performed
	 *
	 * @throws FailedLoginException if invalid credentials or Sembient cloud doesn't respond
	 */
	private void sembientLogin() throws FailedLoginException {
		long currentTime = System.currentTimeMillis();
		if (loginResponse == null || currentTime > loginResponse.getExpirationTime()) {
			Map<String, String> headers = new HashMap<>();
			headers.put(SembientAggregatorConstant.ACCEPT_HEADER, SembientAggregatorConstant.APPLICATION_JSON);
			headers.put(SembientAggregatorConstant.CONTENT_TYPE_HEADER, SembientAggregatorConstant.APPLICATION_JSON);
			String valueToEncode = this.getLogin() + SembientAggregatorConstant.COLON + this.getPassword();
			String encodeBasicScheme = SembientAggregatorConstant.BASIC_AUTH_SCHEME + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
			headers.put(SembientAggregatorConstant.AUTHORIZATION, encodeBasicScheme);
			try {
				String loginRawResponse = this.doPost(SembientAggregatorConstant.COMMAND_USERS_LOGIN, headers, SembientAggregatorConstant.EMPTY);
				LoginWrapper loginWrapper = new ObjectMapper().readValue(loginRawResponse, LoginWrapper.class);
				if (SembientAggregatorConstant.STATUS_CODE_401.equals(loginWrapper.getStatusCode())) {
					throw new FailedLoginException("Wrong username/password.");
				}
				loginResponse = loginWrapper.getLoginResponse();
				loginResponse.setExpirationTime(currentTime + loginResponse.getExp() * 1000L);
			} catch (Exception e) {
				logger.error(String.format("An exception occur when trying to log in with username: %s, password: %s, error message: %s", this.getLogin(), this.getPassword(), e.getMessage()), e);
				throw new FailedLoginException(String.format("Failed to login with username: %s, password: %s", this.getLogin(), this.getPassword()));
			}
		}
	}

	/**
	 * Fetch all buildings
	 *
	 * @throws Exception if fail to get array of building
	 */
	private void fetchBuildings() throws Exception {
		long currentTimestamp = System.currentTimeMillis();
		if (cachedBuildings.size() > 0 && validBuildingAndFloorMetaDataRetrievalPeriodTimestamp > currentTimestamp) {
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Building and floor retrieval is in cooldown. %s seconds left", (validBuildingAndFloorMetaDataRetrievalPeriodTimestamp - currentTimestamp) / 1000));
			}
			return;
		}
		// Apply installation layout polling cycle
		if (StringUtils.isNotNullOrEmpty(installationLayoutPollingCycle)) {
			long interval;
			try {
				int pollingInInt = Integer.parseInt(installationLayoutPollingCycle);
				if (pollingInInt < 1) {
					pollingInInt = SembientAggregatorConstant.DEFAULT_INSTALLATION_LAYOUT_POLLING_CYCLE;
				}
				interval = pollingInInt * SembientAggregatorConstant.MINUTE_TO_MS;
			} catch (Exception e) {
				logger.error("Invalid format, installationLayoutPollingCycle should be integer.", e);
				interval = SembientAggregatorConstant.DEFAULT_INSTALLATION_LAYOUT_POLLING_CYCLE * SembientAggregatorConstant.MINUTE_TO_MS;
			}
			validBuildingAndFloorMetaDataRetrievalPeriodTimestamp = currentTimestamp + interval;
		} else {
			validBuildingAndFloorMetaDataRetrievalPeriodTimestamp = currentTimestamp + SembientAggregatorConstant.DEFAULT_INSTALLATION_LAYOUT_POLLING_CYCLE * SembientAggregatorConstant.MINUTE_TO_MS;
		}
		latestBuildingAndFloorData = true;
		BuildingWrapper buildingWrapper = this.doGetWithRetry(SembientAggregatorConstant.COMMAND_SPACE_BUILDINGS + loginResponse.getCustomerId(), BuildingWrapper.class);
		if (buildingWrapper != null) {
			cachedBuildings.clear();
			cachedBuildings.addAll(Arrays.asList(buildingWrapper.getBuildingResponse()));
		}
	}

	/**
	 * Fetch the latest list of region & sensor device
	 *
	 * @throws Exception if fail to fetch regions & sensors.
	 */
	private void fetchDevicesList() throws Exception {
		long currentTimestamp = System.currentTimeMillis();
		if (!latestBuildingAndFloorData) {
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Region meta data retrieval is in cooldown. %s seconds left", (validBuildingAndFloorMetaDataRetrievalPeriodTimestamp - currentTimestamp) / 1000));
			}
			return;
		}
		// Filter building:
		if (StringUtils.isNullOrEmpty(buildingFilter) && cachedBuildings.stream().findFirst().isPresent()) {
			BuildingResponse firstBuilding = cachedBuildings.stream().findFirst().get();
			String buildingID = firstBuilding.getBuildingID();
			String buildingName = firstBuilding.getBuildingName();
			String[] floorNames = firstBuilding.getFloors();
			// Filter by floors
			filterByFloors(buildingID, buildingName, floorNames);
		} else {
			for (BuildingResponse response : cachedBuildings) {
				if (response.getBuildingName().equals(buildingFilter.trim())) {
					String buildingID = response.getBuildingID();
					String buildingName = response.getBuildingName();
					String[] floorNames = response.getFloors();
					filterByFloors(buildingID, buildingName, floorNames);
					break;
				}
			}
		}
		// Notify worker thread that it's a valid time to start fetching details device information.
		latestBuildingAndFloorData = false;
	}

	/**
	 * Filter aggregated devices by floorNames
	 *
	 * @param buildingID ID of the building
	 * @param buildingName name of the building
	 * @param floorNames Floor names
	 * @throws Exception if fail to retrieve regions.
	 */
	private void filterByFloors(String buildingID, String buildingName, String[] floorNames) throws Exception {
		if (StringUtils.isNotNullOrEmpty(floorFilter)) {
			String[] listFloorToBeFilter = floorFilter.split(SembientAggregatorConstant.COMMA);
			for (String floor : listFloorToBeFilter) {
				for (String floorName : floorNames) {
					if (floor.trim().equals(floorName)) {
						// Filter by region type
						if (StringUtils.isNotNullOrEmpty(deviceTypeFilter)) {
							String[] listTypeToBeFilter = deviceTypeFilter.split(SembientAggregatorConstant.COMMA);
							for (String type : listTypeToBeFilter) {
								retrieveDevices(buildingID, buildingName, floor, type.trim());
							}
						} else {
							retrieveDevices(buildingID, buildingName, floor, null);
						}
					}

				}
			}
		} else {
			for (String floorName : floorNames) {
				if (StringUtils.isNotNullOrEmpty(deviceTypeFilter)) {
					String[] listTypeToBeFilter = deviceTypeFilter.split(SembientAggregatorConstant.COMMA);
					for (String type : listTypeToBeFilter) {
						retrieveDevices(buildingID, buildingName, floorName, type.trim());
					}
				} else {
					retrieveDevices(buildingID, buildingName, floorName, null);
				}
			}
		}
	}

	/**
	 * Retrieve all regions in building & floor
	 *
	 * @param buildingID building id
	 * @param buildingName building name
	 * @param floorName floor name
	 * @param deviceType type of region
	 * @throws Exception if fail to get region
	 */
	void retrieveDevices(String buildingID, String buildingName, String floorName, String deviceType) throws Exception {
		if (deviceType == null) {
			// Fetch all devices
			// 1. Fetch regions
			if (StringUtils.isNotNullOrEmpty(regionTypeFilter)) {
				String[] listTypeToBeFilter = regionTypeFilter.split(SembientAggregatorConstant.COMMA);
				for (String regionType : listTypeToBeFilter) {
					retrieveRegions(buildingID, buildingName, floorName, regionType.trim());
				}
			} else {
				retrieveRegions(buildingID, buildingName, floorName, null);
			}
			// 2. Fetch sensors
			retrieveSensors(buildingID, buildingName, floorName);
		} else {
			if (deviceType.equals(SembientAggregatorConstant.SENSOR)) {
				retrieveSensors(buildingID, buildingName, floorName);
			} else if (deviceType.equals(SembientAggregatorConstant.REGION)) {
				if (StringUtils.isNotNullOrEmpty(regionTypeFilter)) {
					String[] listTypeToBeFilter = regionTypeFilter.split(SembientAggregatorConstant.COMMA);
					for (String regionType : listTypeToBeFilter) {
						retrieveRegions(buildingID, buildingName, floorName, regionType.trim());
					}
				} else {
					retrieveRegions(buildingID, buildingName, floorName, null);
				}
			}
		}
	}

	/**
	 * Retrieve all regions in building & floor
	 *
	 * @param buildingID building ID
	 * @param buildingName building name
	 * @param floorName floor name
	 * @param regionType type of region
	 * @throws Exception if fail to get region
	 */
	void retrieveRegions(String buildingID, String buildingName, String floorName, String regionType) throws Exception {
		String request;
		if (regionType != null) {
			request = SembientAggregatorConstant.COMMAND_SPACE_REGIONS + this.loginResponse.getCustomerId() + SembientAggregatorConstant.SLASH + buildingID + SembientAggregatorConstant.SLASH + floorName
					+ SembientAggregatorConstant.PARAM_REGION_TYPE + regionType;
		} else {
			request = SembientAggregatorConstant.COMMAND_SPACE_REGIONS + this.loginResponse.getCustomerId() + SembientAggregatorConstant.SLASH + buildingID + SembientAggregatorConstant.SLASH + floorName;
		}
		RegionWrapper regionWrapper = this.doGetWithRetry(request, RegionWrapper.class);
		if (regionWrapper != null) {
			RegionResponse[] regionResponses = regionWrapper.getRegionResponse();
			if (regionResponses.length != 0) {
				fetchRegionMetadata(buildingID, buildingName, floorName, regionResponses);
			}
		}
	}

	/**
	 * Retrieve all regions in building & floor
	 *
	 * @param buildingID building ID
	 * @param buildingName building name
	 * @param floorName floor name
	 * @throws Exception if fail to get air quality wrapper
	 */
	void retrieveSensors(String buildingID, String buildingName, String floorName) throws Exception {
		String request;
		request = SembientAggregatorConstant.COMMAND_SPACE_REGIONS + this.loginResponse.getCustomerId() + SembientAggregatorConstant.SLASH + buildingID + SembientAggregatorConstant.SLASH + floorName;
		RegionWrapper regionWrapper = this.doGetWithRetry(request, RegionWrapper.class);
		Map<String, String> sensors = new HashMap<>();
		if (regionWrapper != null) {
			RegionResponse[] regionResponses = regionWrapper.getRegionResponse();
			if (regionResponses.length != 0) {
				for (RegionResponse response : regionResponses
				) {
					String regionName = response.getRegionName();
					String[] sensorNames = response.getSensors();
					for (String sensorName : sensorNames) {
						if (sensors.get(sensorName) == null) {
							sensors.put(sensorName, regionName);
						} else {
							String value = sensors.get(sensorName);
							if (!value.contains(regionName)) {
								value += SembientAggregatorConstant.COMMA + regionName;
								sensors.put(sensorName, value);
							}
						}
					}
				}
			}
			for (Entry<String, String> sensorResponse : sensors.entrySet()) {
				String sensorName = sensorResponse.getKey();
				boolean isContinue = false;
				// Filter by device name:
				if (StringUtils.isNotNullOrEmpty(deviceNameFilter)) {
					String[] sensorNames = deviceNameFilter.split(SembientAggregatorConstant.COMMA);
					for (String sensor : sensorNames) {
						if (sensor.trim().equals(sensorName)) {
							isContinue = true;
							break;
						}
					}
				}
				if (StringUtils.isNotNullOrEmpty(deviceNameFilter) && !isContinue) {
					continue;
				}

				AggregatedDevice sensorDevice = new AggregatedDevice();
				String deviceID =
						SembientAggregatorConstant.SENSOR + SembientAggregatorConstant.DASH + loginResponse.getCustomerId() + SembientAggregatorConstant.DASH + buildingID + SembientAggregatorConstant.DASH
								+ floorName + SembientAggregatorConstant.DASH + sensorName;
				sensorDevice.setDeviceId(deviceID);
				sensorDevice.setCategory(SembientAggregatorConstant.SENSOR);
				sensorDevice.setDeviceType(SembientAggregatorConstant.SENSOR);
				sensorDevice.setDeviceOnline(true);
				sensorDevice.setDeviceName(sensorName);
				Map<String, String> properties = new HashMap<>();
				if (aggregatedDevices.get(deviceID) != null && !aggregatedDevices.get(deviceID).getProperties().isEmpty()) {
					properties = aggregatedDevices.get(deviceID).getProperties();
				} else {
					if (!SembientAggregatorConstant.EMPTY.equals(sensorResponse.getValue())) {
						properties.put(SembientAggregatorConstant.REGIONS, sensorResponse.getValue());
					}
					properties.put(SembientAggregatorConstant.BUILDING_NAME, buildingName);
					properties.put(SembientAggregatorConstant.FLOOR_NAME, floorName);
				}
				sensorDevice.setProperties(properties);
				aggregatedDevices.put(deviceID, sensorDevice);
			}
		}
	}

	/**
	 * Fetch metadata for region (region name, type, sensor id, region tags)
	 *
	 * @param buildingID building ID
	 * @param buildingName building name
	 * @param floorName floor name
	 * @param regionResponses Array of region responses
	 */
	private void fetchRegionMetadata(String buildingID, String buildingName, String floorName, RegionResponse[] regionResponses) {
		for (RegionResponse region : regionResponses) {
			boolean isContinue = false;
			// Filter by device name:
			if (StringUtils.isNotNullOrEmpty(deviceNameFilter)) {
				String[] regionNames = deviceNameFilter.split(SembientAggregatorConstant.COMMA);
				for (String regionName : regionNames) {
					if (regionName.trim().equals(region.getRegionName())) {
						isContinue = true;
						break;
					}
				}
			}
			if (StringUtils.isNotNullOrEmpty(deviceNameFilter) && !isContinue) {
				continue;
			}
			AggregatedDevice aggregatedDevice = new AggregatedDevice();
			// Response doesn't contain any id, in order to make device id unique we create a combination of building, floor and region --
			// For instance: BuildingA-Floor1-Region1
			aggregatedDevice.setDeviceId(
					SembientAggregatorConstant.REGION + SembientAggregatorConstant.DASH + loginResponse.getCustomerId() + SembientAggregatorConstant.DASH + buildingID + SembientAggregatorConstant.DASH
							+ floorName + SembientAggregatorConstant.DASH + region.getRegionName());
			aggregatedDevice.setCategory(SembientAggregatorConstant.REGION);
			aggregatedDevice.setDeviceType(SembientAggregatorConstant.REGION);
			aggregatedDevice.setDeviceOnline(true);
			aggregatedDevice.setDeviceName(region.getRegionName());
			// occupancy, thermal, iaq data will be populated later on.
			if (aggregatedDevices.get(aggregatedDevice.getDeviceId()) != null) {
				Map<String, String> propertiesFromCached = aggregatedDevices.get(aggregatedDevice.getDeviceId()).getProperties();
				List<AdvancedControllableProperty> controlsFromCached = aggregatedDevices.get(aggregatedDevice.getDeviceId()).getControllableProperties();
				aggregatedDevice.setProperties(propertiesFromCached);
				aggregatedDevice.setControllableProperties(controlsFromCached);
			} else {
				Map<String, String> properties = new HashMap<>();
				properties.put(SembientAggregatorConstant.CAPACITY, region.getCapacity());
				properties.put(SembientAggregatorConstant.REGION_TYPE, region.getRegionType());
				properties.put(SembientAggregatorConstant.BUILDING_NAME, buildingName);
				properties.put(SembientAggregatorConstant.FLOOR_NAME, floorName);
				aggregatedDevice.setProperties(properties);
				aggregatedDevice.setControllableProperties(new ArrayList<>());
			}
			aggregatedDevices.put(aggregatedDevice.getDeviceId(), aggregatedDevice);
		}
	}

	/**
	 * Populate device details information
	 *
	 * @param aggregatedDevice Aggregated device that get from {@link SembientAggregatorCommunicator#fetchDevicesList}
	 */
	void populateRegionDetails(AggregatedDevice aggregatedDevice) throws Exception {
		// Get cached properties and controls
		Map<String, String> properties = aggregatedDevice.getProperties();
		List<AdvancedControllableProperty> controls = aggregatedDevice.getControllableProperties();
		// Get current date:
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(SembientAggregatorConstant.YYYY_MM_DD);
		LocalDate now = LocalDate.now(ZoneId.of(SembientAggregatorConstant.UTC_TIMEZONE));
		// Get yesterday:
		LocalDate yesterday = LocalDate.now(ZoneId.of(SembientAggregatorConstant.UTC_TIMEZONE)).minusDays(1);
		String currentDate = formatter.format(now);
		String yesterdayDate = formatter.format(yesterday);

		String deviceId = aggregatedDevice.getDeviceId();

		String deviceName = aggregatedDevice.getDeviceName();
		String[] rawBuildingInfo = deviceId.split(SembientAggregatorConstant.DASH);
		int lastIndex = rawBuildingInfo.length - 1;
		String buildingID = rawBuildingInfo[lastIndex - 2];
		String floorName = rawBuildingInfo[lastIndex - 1];
		int numberOfRetryInInt = getNumberOfRetryFromUserInput();
		long retryIntervalInLong = getRetryIntervalFromUserInput();
		if (SembientAggregatorConstant.SENSOR.equals(aggregatedDevice.getDeviceType())) {
			// Retrieve IAQ data
			CompletableFuture<Boolean> iaqFuture = CompletableFuture.supplyAsync(() -> populateIAQData(properties, currentDate, yesterdayDate, buildingID, floorName, deviceName));
			// Retrieve thermal data
			CompletableFuture<Boolean> thermalFuture = CompletableFuture.supplyAsync(() -> populateThermalData(properties, currentDate, yesterdayDate, buildingID, floorName, deviceName));

			// retry on 429 error
			iaqFuture.thenApply(result -> {
				if (!result) {
					int attemptRetry = 1;
					boolean isHavingData;
					do {
						// wait to next retry interval
						try {
							Thread.sleep(retryIntervalInLong);
						} catch (InterruptedException e) {
							logger.error(String.format("error while retrieve thermal data: %s", e.getMessage()));
						}

						isHavingData = populateIAQData(properties, currentDate, yesterdayDate, buildingID, floorName, deviceName);
						if (isHavingData) {
							break;
						}
					} while (attemptRetry++ < numberOfRetryInInt);
				}
				return false;
			});

			// retry on 429 error
			thermalFuture.thenApply(result -> {
				if (!result) {
					int attemptRetry = 1;
					boolean isHavingData;
					do {
						// wait to next retry interval
						try {
							Thread.sleep(retryIntervalInLong);
						} catch (InterruptedException e) {
							logger.error(String.format("error while retrieve thermal data: %s", e.getMessage()));
						}

						isHavingData = populateThermalData(properties, currentDate, yesterdayDate, buildingID, floorName, deviceName);
						if (isHavingData) {
							break;
						}
					} while (attemptRetry++ < numberOfRetryInInt);
				}
				return false;
			});
		} else {
			// Retrieve occupancy data
			CompletableFuture<Boolean> occupancyFuture = CompletableFuture.supplyAsync(
					() -> populateOccupancyData(properties, controls, currentDate, yesterdayDate, deviceId, buildingID, floorName, deviceName));
			// Retrieve region tags
			CompletableFuture<Boolean> regionTagFuture = CompletableFuture.supplyAsync(
					() -> populateRegionTag(properties, controls, deviceId));

			// retry on 429 error
			occupancyFuture.thenApply(result -> {
				if (!result) {
					int attemptRetry = 1;
					boolean isHavingData;
					do {
						// wait to next retry interval
						try {
							Thread.sleep(retryIntervalInLong);
						} catch (InterruptedException e) {
							logger.error(String.format("error while retrieve thermal data: %s", e.getMessage()));
						}
						isHavingData = populateOccupancyData(properties, controls, currentDate, yesterdayDate, deviceId, buildingID, floorName, deviceName);
						if (isHavingData) {
							break;
						}
					} while (attemptRetry++ < numberOfRetryInInt);
				}
				return false;
			});

			// retry on 429 error
			regionTagFuture.thenApply(result -> {
				if (!result) {
					int attemptRetry = 1;
					boolean isHavingData;
					do {
						// wait to next retry interval
						try {
							Thread.sleep(retryIntervalInLong);
						} catch (InterruptedException e) {
							logger.error(String.format("error while retrieve thermal data: %s", e.getMessage()));
						}

						isHavingData = populateRegionTag(properties, controls, deviceId);
						if (isHavingData) {
							break;
						}
					} while (attemptRetry++ < numberOfRetryInInt);
				}
				return false;
			});
		}
		aggregatedDevice.setProperties(properties);
		aggregatedDevice.setControllableProperties(controls);
		aggregatedDevices.put(aggregatedDevice.getDeviceId(), aggregatedDevice);
	}

	/**
	 * Populate region tag
	 *
	 * @param properties Map of cached properties of region (Aggregated device)
	 * @param controls List of cached AdvancedControllableProperty of region (Aggregated device)
	 * @param deviceId Device id of region.
	 * @return boolean is populateData successful
	 * @throws Exception when fail to get region tags
	 */
	private boolean populateRegionTag(Map<String, String> properties, List<AdvancedControllableProperty> controls, String deviceId) {
		// Remove old cached region tag properties
		properties.remove(SembientAggregatorConstant.REGION_TAG_NEW_TAG);
		controls.removeIf(advancedControllableProperty -> advancedControllableProperty.getName().equals(SembientAggregatorConstant.REGION_TAG_NEW_TAG));
		properties.remove(SembientAggregatorConstant.REGION_TAG_CREATE);
		controls.removeIf(advancedControllableProperty -> advancedControllableProperty.getName().equals(SembientAggregatorConstant.REGION_TAG_CREATE));
		properties.remove(SembientAggregatorConstant.PROPERTY_TAG);
		controls.removeIf(advancedControllableProperty -> advancedControllableProperty.getName().equals(SembientAggregatorConstant.PROPERTY_TAG));
		properties.remove(SembientAggregatorConstant.PROPERTY_DELETE);
		controls.removeIf(advancedControllableProperty -> advancedControllableProperty.getName().equals(SembientAggregatorConstant.PROPERTY_DELETE));
		//
		controls.add(createText(properties, SembientAggregatorConstant.REGION_TAG_NEW_TAG, lastNewTag.get(deviceId)));
		controls.add(createButton(properties, SembientAggregatorConstant.REGION_TAG_CREATE, SembientAggregatorConstant.LABEL_CREATE, SembientAggregatorConstant.LABEL_PRESSED_CREATING));
		String[] regionDetails = deviceId.split(SembientAggregatorConstant.DASH);
		int lastIndex = regionDetails.length - 1;
		String buildingID = regionDetails[lastIndex - 2];
		String floorName = regionDetails[lastIndex - 1];
		String regionName = regionDetails[lastIndex];
		String request = SembientAggregatorConstant.COMMAND_SPACE_TAGS + this.loginResponse.getCustomerId() + SembientAggregatorConstant.SLASH + buildingID + SembientAggregatorConstant.SLASH + floorName
				+ SembientAggregatorConstant.PARAM_REGION_NAME + regionName;
		RegionTagWrapperMonitor regionTagWrapperControl = this.doGetWithRetryForWorkerThread(request, RegionTagWrapperMonitor.class);
		// Get getRegionResponse by first index because it only has 1 element.
		// There are some cases that getRegionResponse array is empty
		if (regionTagWrapperControl == null && cachedTooManyRequestError.contains(request)) {
			cachedTooManyRequestError.remove(request);
			return false;
		}
		if (regionTagWrapperControl != null && regionTagWrapperControl.getRegionResponse().length != 0 && regionTagWrapperControl.getRegionResponse()[0].getRegionTags().length != 0) {
			String[] regionTags = regionTagWrapperControl.getRegionResponse()[0].getRegionTags();
			List<String> tags = new ArrayList<>(Arrays.asList(regionTags));
			String currentTag = tags.get(0);
			if (aggregatedDeviceTagMap.containsKey(deviceId)) {
				if (tags.contains(aggregatedDeviceTagMap.get(deviceId))) {
					// Check if latest list contain previous tag value
					currentTag = aggregatedDeviceTagMap.get(deviceId);
				} else {
					// Set back to default value if aggregatedDeviceTagMap isn't update to the latest one.
					aggregatedDeviceTagMap.put(deviceId, currentTag);
				}
			} else {
				aggregatedDeviceTagMap.put(deviceId, currentTag);
			}
			controls.add(createDropdown(properties, SembientAggregatorConstant.PROPERTY_TAG, tags, currentTag));
			controls.add(createButton(properties, SembientAggregatorConstant.PROPERTY_DELETE, SembientAggregatorConstant.LABEL_DELETE, SembientAggregatorConstant.LABEL_PRESSED_DELETING));
		}
		return true;
		// Not populate Delete button and Tag dropdown if there are no tags in region
	}

	/**
	 * Populate air quality data for region (aggregated device) in thread.
	 * 1. Get today data first
	 * 2. Get yesterday data if today data is empty
	 * 3. If fail to get both then we put "No data" in "Message" property
	 *
	 * @param properties Map of cached properties for aggregated device
	 * @param currentDate Current date in string
	 * @param yesterdayDate Yesterday in string
	 * @param buildingID building name
	 * @param floorName floor name
	 * @param deviceName device name
	 * @return boolean is populate data successful
	 * @throws Exception if fail to get {@link AirQualityWrapper}
	 */
	private boolean populateIAQData(Map<String, String> properties, String currentDate, String yesterdayDate, String buildingID, String floorName, String deviceName) {
		String firstRequest =
				SembientAggregatorConstant.COMMAND_IAQ_TIMESERIES + loginResponse.getCustomerId() + SembientAggregatorConstant.SLASH + buildingID + SembientAggregatorConstant.SLASH + floorName
						+ SembientAggregatorConstant.SLASH + currentDate;
		AirQualityWrapper airQualityWrapper = doGetWithRetryForWorkerThread(firstRequest, AirQualityWrapper.class);
		if (airQualityWrapper != null) {
			AirQualitySensorResponse[] airQualitySensorResponses = new AirQualitySensorResponse[0];
			if (SembientAggregatorConstant.STATUS_CODE_200.equals(airQualityWrapper.getStatusCode()) && airQualityWrapper.getAirQualitySensorWrapper() != null) {
				airQualitySensorResponses = airQualityWrapper.getAirQualitySensorWrapper().getAirQualitySensorResponses();
			}
			if (airQualitySensorResponses.length == 0) {
				String secondRequest =
						SembientAggregatorConstant.COMMAND_IAQ_TIMESERIES + loginResponse.getCustomerId() + SembientAggregatorConstant.SLASH + buildingID + SembientAggregatorConstant.SLASH + floorName
								+ SembientAggregatorConstant.SLASH + yesterdayDate;
				airQualityWrapper = doGetWithRetryForWorkerThread(secondRequest, AirQualityWrapper.class);
				if (airQualityWrapper != null) {
					if (SembientAggregatorConstant.STATUS_CODE_200.equals(airQualityWrapper.getStatusCode()) && airQualityWrapper.getAirQualitySensorWrapper() != null) {
						airQualitySensorResponses = airQualityWrapper.getAirQualitySensorWrapper().getAirQualitySensorResponses();
					}
					if (airQualitySensorResponses.length == 0) {
						populateNoData(properties, SembientAggregatorConstant.AIR_QUALITY);
					}
				} else {
					if (!properties.containsKey(SembientAggregatorConstant.AIR_QUALITY + SembientAggregatorConstant.HASH
							+ SembientAggregatorConstant.CO2_VALUE_LATEST)) {
						populateNoData(properties, SembientAggregatorConstant.AIR_QUALITY);
					}
					if (cachedTooManyRequestError.contains(secondRequest)) {
						cachedTooManyRequestError.remove(secondRequest);
						return false;
					}
				}
			}
			Map<String, AirQualityData[]> sensorAndIAQMap = new HashMap<>();
			for (AirQualitySensorResponse airQualitySensorResponse : airQualitySensorResponses) {
				if (deviceName.equals(airQualitySensorResponse.getSensorName())) {
					sensorAndIAQMap.put(airQualitySensorResponse.getSensorName(), airQualitySensorResponse.getAirQualityData());
				}
			}
			// Remove previous properties
			// CO2
			String co2Property = SembientAggregatorConstant.AIR_QUALITY + SembientAggregatorConstant.HASH
					+ SembientAggregatorConstant.CO2_VALUE_LATEST;
			// TVOC
			String tvocProperty = SembientAggregatorConstant.AIR_QUALITY + SembientAggregatorConstant.HASH
					+ SembientAggregatorConstant.TVOC_VALUE_LATEST_MICROGRAM;
			// PM25
			String pm25Property = SembientAggregatorConstant.AIR_QUALITY + SembientAggregatorConstant.HASH
					+ SembientAggregatorConstant.PM_25_VALUE_LATEST_MICROMET;
			//
			String fromTimeProperty = SembientAggregatorConstant.AIR_QUALITY + SembientAggregatorConstant.HASH
					+ SembientAggregatorConstant.FROM_TIME;
			String toTimeProperty = SembientAggregatorConstant.AIR_QUALITY + SembientAggregatorConstant.HASH
					+ SembientAggregatorConstant.TO_TIME;
			String recentDataProperty = SembientAggregatorConstant.AIR_QUALITY + SembientAggregatorConstant.HASH
					+ SembientAggregatorConstant.RECENT_DATA;
			String messageProperty = SembientAggregatorConstant.AIR_QUALITY + SembientAggregatorConstant.HASH
					+ SembientAggregatorConstant.MESSAGE;
			properties.remove(co2Property);
			properties.remove(tvocProperty);
			properties.remove(pm25Property);

			properties.remove(fromTimeProperty);
			properties.remove(toTimeProperty);
			properties.remove(recentDataProperty);
			properties.remove(messageProperty);

			for (Map.Entry<String, AirQualityData[]> entry : sensorAndIAQMap.entrySet()) {
				//
				int lastIndex = entry.getValue().length - 1;
				properties.put(co2Property, entry.getValue()[lastIndex].getCo2());
				properties.put(tvocProperty, entry.getValue()[lastIndex].getTvoc());
				properties.put(pm25Property, entry.getValue()[lastIndex].getPm25());
				DateFormat obj = new SimpleDateFormat(SembientAggregatorConstant.DATE_ISO_FORMAT);
				obj.setTimeZone(TimeZone.getTimeZone(SembientAggregatorConstant.UTC_TIMEZONE));
				// Convert s to ms
				Date fromTimeDate = new Date(entry.getValue()[0].getTimestamp() * 1000);
				Date toTimeDate = new Date(entry.getValue()[lastIndex].getTimestamp() * 1000);
				long resInMs = toTimeDate.getTime();
				long currentTimeMs = System.currentTimeMillis();

				long dif = currentTimeMs - resInMs;
				long hourInMs = 3600 * 1000;
				boolean isRecentData = (dif) < hourInMs;
				properties.put(recentDataProperty, String.valueOf(isRecentData));
				properties.put(toTimeProperty, obj.format(toTimeDate));
				properties.put(fromTimeProperty, obj.format(fromTimeDate));
			}
		} else {
			if (!properties.containsKey(SembientAggregatorConstant.AIR_QUALITY + SembientAggregatorConstant.HASH
					+ SembientAggregatorConstant.CO2_VALUE_LATEST)) {
				populateNoData(properties, SembientAggregatorConstant.AIR_QUALITY);
			}
			if (cachedTooManyRequestError.contains(firstRequest)) {
				cachedTooManyRequestError.remove(firstRequest);
				return false;
			}
		}
		return true;
	}

	/**
	 * Populate thermal data for region (aggregated device) in thread.
	 * 1. Get today data first
	 * 2. Get yesterday data if today data is empty
	 * 3. If fail to get both then we put "No data" in "Message" property
	 *
	 * @param properties Map of cached properties of aggregated device
	 * @param currentDate Current date in string
	 * @param yesterdayDate Yesterday in string
	 * @param buildingID building ID
	 * @param floorName floor name
	 * @param deviceName device name
	 * @return boolean is populate data successful
	 * @throws Exception if fail to get {@link ThermalWrapper}
	 */
	private boolean populateThermalData(Map<String, String> properties, String currentDate, String yesterdayDate, String buildingID, String floorName, String deviceName) {
		String firstRequest =
				SembientAggregatorConstant.COMMAND_THERMAL_TIMESERIES + loginResponse.getCustomerId() + SembientAggregatorConstant.SLASH + buildingID + SembientAggregatorConstant.SLASH + floorName
						+ SembientAggregatorConstant.SLASH + currentDate;
		ThermalWrapper thermalWrapper = doGetWithRetryForWorkerThread(firstRequest, ThermalWrapper.class);
		if (thermalWrapper != null) {
			ThermalSensorResponse[] thermalSensorResponse = new ThermalSensorResponse[0];
			if (SembientAggregatorConstant.STATUS_CODE_200.equals(thermalWrapper.getStatusCode()) && thermalWrapper.getThermalSensorWrappers() != null) {
				thermalSensorResponse = thermalWrapper.getThermalSensorWrappers().getThermalSensorResponses();
			}
			if (thermalSensorResponse.length == 0) {
				// Retry with yesterday data
				String secondRequest =
						SembientAggregatorConstant.COMMAND_THERMAL_TIMESERIES + loginResponse.getCustomerId() + SembientAggregatorConstant.SLASH + buildingID + SembientAggregatorConstant.SLASH + floorName
								+ SembientAggregatorConstant.SLASH + yesterdayDate;
				thermalWrapper = doGetWithRetryForWorkerThread(secondRequest, ThermalWrapper.class);
				if (thermalWrapper != null) {
					if (SembientAggregatorConstant.STATUS_CODE_200.equals(thermalWrapper.getStatusCode()) && thermalWrapper.getThermalSensorWrappers() != null) {
						thermalSensorResponse = thermalWrapper.getThermalSensorWrappers().getThermalSensorResponses();
					}
					if (thermalSensorResponse.length == 0) {
						populateNoData(properties, SembientAggregatorConstant.THERMAL);
					}
				} else {
					if (!properties.containsKey(SembientAggregatorConstant.THERMAL + SembientAggregatorConstant.HASH + SembientAggregatorConstant.TEMPERATURE_LATEST_F)) {
						populateNoData(properties, SembientAggregatorConstant.THERMAL);
					}
					if (cachedTooManyRequestError.contains(secondRequest)) {
						cachedTooManyRequestError.remove(secondRequest);
						return false;
					}
				}
			}

			Map<String, ThermalData[]> sensorAndThermalMap = new HashMap<>();
			for (ThermalSensorResponse sensorResponse : thermalSensorResponse) {
				if (sensorResponse.getSensorName().equals(deviceName)) {
					sensorAndThermalMap.put(deviceName, sensorResponse.getThermalData());
				}
			}
			// Temperature
			String sensorLatestTemperatureProperty = SembientAggregatorConstant.THERMAL + SembientAggregatorConstant.HASH + SembientAggregatorConstant.TEMPERATURE_LATEST_F;
			String sensorAvgTemperatureProperty = SembientAggregatorConstant.THERMAL + SembientAggregatorConstant.HASH + SembientAggregatorConstant.TEMPERATURE_AVG_F;
			String sensorMaxTemperatureProperty = SembientAggregatorConstant.THERMAL + SembientAggregatorConstant.HASH + SembientAggregatorConstant.TEMPERATURE_MAX_F;
			String sensorMinTemperatureProperty = SembientAggregatorConstant.THERMAL + SembientAggregatorConstant.HASH + SembientAggregatorConstant.TEMPERATURE_MIN_F;
			// Humidity
			String sensorLatestHumidityProperty = SembientAggregatorConstant.THERMAL + SembientAggregatorConstant.HASH + SembientAggregatorConstant.HUMIDITY_LATEST;
			String sensorAvgHumidityProperty = SembientAggregatorConstant.THERMAL + SembientAggregatorConstant.HASH + SembientAggregatorConstant.HUMIDITY_AVG;
			String sensorMaxHumidityProperty = SembientAggregatorConstant.THERMAL + SembientAggregatorConstant.HASH + SembientAggregatorConstant.HUMIDITY_MAX;
			String sensorMinHumidityProperty = SembientAggregatorConstant.THERMAL + SembientAggregatorConstant.HASH + SembientAggregatorConstant.HUMIDITY_MIN;
			// Other
			String sensorFromTimeProperty = SembientAggregatorConstant.THERMAL + SembientAggregatorConstant.HASH + SembientAggregatorConstant.FROM_TIME;
			String sensorToTimeProperty = SembientAggregatorConstant.THERMAL + SembientAggregatorConstant.HASH + SembientAggregatorConstant.TO_TIME;
			String sensorRecentDataProperty = SembientAggregatorConstant.THERMAL + SembientAggregatorConstant.HASH + SembientAggregatorConstant.RECENT_DATA;
			String sensorMessageProperty = SembientAggregatorConstant.THERMAL + SembientAggregatorConstant.HASH + SembientAggregatorConstant.MESSAGE;
			// Remove previous properties
			properties.remove(sensorLatestTemperatureProperty);
			properties.remove(sensorAvgTemperatureProperty);
			properties.remove(sensorMaxTemperatureProperty);
			properties.remove(sensorMinTemperatureProperty);

			properties.remove(sensorLatestHumidityProperty);
			properties.remove(sensorMaxHumidityProperty);
			properties.remove(sensorMinHumidityProperty);
			properties.remove(sensorAvgHumidityProperty);

			properties.remove(sensorFromTimeProperty);
			properties.remove(sensorToTimeProperty);
			properties.remove(sensorRecentDataProperty);
			properties.remove(sensorMessageProperty);
			for (Map.Entry<String, ThermalData[]> entry : sensorAndThermalMap.entrySet()) {
				ThermalData[] thermals = entry.getValue();
				double averageThermal = Arrays.stream(thermals)
						.mapToDouble(ThermalData::getTemperature)
						.average()
						.orElse(Double.NaN);
				double averageHumidity = Arrays.stream(thermals)
						.mapToDouble(ThermalData::getHumidity)
						.average()
						.orElse(Double.NaN);
				int latestThermal = thermals[thermals.length - 1].getTemperature();
				int latestHumidity = thermals[thermals.length - 1].getHumidity();
				long fromTime = thermals[0].getTimestamp();
				long toTime = thermals[thermals.length - 1].getTimestamp();
				Arrays.sort(thermals, Comparator.comparing(ThermalData::getTemperature));
				int minThermal = thermals[0].getTemperature();
				int maxThermal = thermals[thermals.length - 1].getTemperature();
				Arrays.sort(thermals, Comparator.comparing(ThermalData::getHumidity));
				int minHumidity = thermals[0].getHumidity();
				int maxHumidity = thermals[thermals.length - 1].getHumidity();
				// Temperature
				properties.put(sensorLatestTemperatureProperty, String.valueOf(latestThermal));
				properties.put(sensorMaxTemperatureProperty, String.valueOf(maxThermal));
				properties.put(sensorMinTemperatureProperty, String.valueOf(minThermal));
				properties.put(sensorAvgTemperatureProperty, String.format(SembientAggregatorConstant.FLOAT_WITH_TWO_DECIMAL, averageThermal));
				// Humidity
				properties.put(sensorLatestHumidityProperty, String.valueOf(latestHumidity));
				properties.put(sensorMaxHumidityProperty, String.valueOf(maxHumidity));
				properties.put(sensorMinHumidityProperty, String.valueOf(minHumidity));
				properties.put(sensorAvgHumidityProperty, String.format(SembientAggregatorConstant.FLOAT_WITH_TWO_DECIMAL, averageHumidity));
				DateFormat obj = new SimpleDateFormat(SembientAggregatorConstant.DATE_ISO_FORMAT);
				obj.setTimeZone(TimeZone.getTimeZone(SembientAggregatorConstant.UTC_TIMEZONE));
				// Convert s to ms
				Date fromTimeDate = new Date(fromTime * 1000);
				Date toTimeDate = new Date(toTime * 1000);
				long toTimeInMs = fromTimeDate.getTime();
				long currentTimeMs = System.currentTimeMillis();

				long dif = currentTimeMs - toTimeInMs;
				long hourInMs = 3600 * 1000;
				boolean isRecentData = (dif) < hourInMs;
				properties.put(sensorRecentDataProperty, String.valueOf(isRecentData));
				properties.put(sensorToTimeProperty, obj.format(toTimeDate));
				properties.put(sensorFromTimeProperty, obj.format(fromTimeDate));
			}
		} else {
			if (!properties.containsKey(SembientAggregatorConstant.THERMAL + SembientAggregatorConstant.HASH + SembientAggregatorConstant.TEMPERATURE_LATEST_F)) {
				populateNoData(properties, SembientAggregatorConstant.THERMAL);
			}
			if (cachedTooManyRequestError.contains(firstRequest)) {
				cachedTooManyRequestError.remove(firstRequest);
				return false;
			}
		}
		return true;
	}

	/**
	 * Populate no data message
	 *
	 * @param properties Map of statistics
	 * @param groupType type of the group
	 */
	private void populateNoData(Map<String, String> properties, String groupType) {
		properties.put(groupType + SembientAggregatorConstant.HASH + SembientAggregatorConstant.MESSAGE,
				SembientAggregatorConstant.NO_DATA);
	}

	/**
	 * Populate occupancy data for region (aggregated device) in thread.
	 * 1. Get today data first
	 * 2. Get yesterday data if today data is empty
	 * 3. If fail to get both then we put "No data" in "Message" property
	 *
	 * @param properties Map of cached properties of aggregated device
	 * @param controls List of cached AdvancedControllableProperty of aggregated device
	 * @param currentDate Current date in string
	 * @param yesterdayDate Yesterday in string
	 * @param deviceId device id
	 * @param buildingID building ID
	 * @param floorName floor name
	 * @param regionName region name
	 * @return boolean is populate data successful
	 * @throws Exception If fail to get {@link OccupancyWrapper} data.
	 */
	private boolean populateOccupancyData(Map<String, String> properties, List<AdvancedControllableProperty> controls, String currentDate, String yesterdayDate, String deviceId, String buildingID,
			String floorName, String regionName) {
		// Retrieve data from today
		String dateToBeDisplayed = currentDate;
		String firstRequest =
				SembientAggregatorConstant.COMMAND_OCCUPANCY_TIMESERIES + loginResponse.getCustomerId() + SembientAggregatorConstant.SLASH + buildingID + SembientAggregatorConstant.SLASH + floorName
						+ SembientAggregatorConstant.SLASH + currentDate;
		OccupancyWrapper occupancyWrapper = this.doGetWithRetryForWorkerThread(firstRequest, OccupancyWrapper.class);
		if (occupancyWrapper != null) {
			properties.remove(SembientAggregatorConstant.PROPERTY_MESSAGE);
			OccupancyRegionResponse[] occupancyRegionResponses = new OccupancyRegionResponse[0];
			if (SembientAggregatorConstant.STATUS_CODE_200.equals(occupancyWrapper.getStatusCode()) && occupancyWrapper.getOccupancyRegionWrappers() != null) {
				occupancyRegionResponses = occupancyWrapper.getOccupancyRegionWrappers().getOccupancyRegionResponses();
			}
			if (occupancyRegionResponses.length == 0) {
				// Retry one more time with yesterday data.
				String secondRequest =
						SembientAggregatorConstant.COMMAND_OCCUPANCY_TIMESERIES + loginResponse.getCustomerId() + SembientAggregatorConstant.SLASH + buildingID + SembientAggregatorConstant.SLASH + floorName
								+ SembientAggregatorConstant.SLASH + yesterdayDate;
				occupancyWrapper = this.doGetWithRetryForWorkerThread(secondRequest, OccupancyWrapper.class);
				if (occupancyWrapper != null) {
					if (SembientAggregatorConstant.STATUS_CODE_200.equals(occupancyWrapper.getStatusCode()) && occupancyWrapper.getOccupancyRegionWrappers() != null) {
						occupancyRegionResponses = occupancyWrapper.getOccupancyRegionWrappers().getOccupancyRegionResponses();
					}
					if (occupancyRegionResponses.length == 0) {
						properties.put(SembientAggregatorConstant.PROPERTY_MESSAGE, SembientAggregatorConstant.NO_DATA);
					}
					dateToBeDisplayed = yesterdayDate;
				} else {
					properties.put(SembientAggregatorConstant.PROPERTY_MESSAGE, SembientAggregatorConstant.NO_DATA);
					if (cachedTooManyRequestError.contains(secondRequest)) {
						cachedTooManyRequestError.remove(secondRequest);
						return false;
					}
				}
			}
			// Remove previous properties
			properties.remove(SembientAggregatorConstant.PROPERTY_HOUR);
			properties.remove(SembientAggregatorConstant.PROPERTY_CURRENT_DATE);
			properties.remove(SembientAggregatorConstant.PROPERTY_MESSAGE);
			properties.remove(SembientAggregatorConstant.PROPERTY_NUMBER_OF_OCCUPANTS);
			properties.remove(SembientAggregatorConstant.PROPERTY_OCCUPANCY);
			properties.remove(SembientAggregatorConstant.PROPERTY_USAGE_TIME_IN_MINUTE);
			properties.remove(SembientAggregatorConstant.PROPERTY_USAGE_TIME_IN_PERCENT);
			controls.removeIf(advancedControllableProperty -> advancedControllableProperty.getName().equals(SembientAggregatorConstant.PROPERTY_HOUR));
			OccupancyData[] occupancyData = new OccupancyData[0];
			for (OccupancyRegionResponse res : occupancyRegionResponses) {
				if (regionName.equals(res.getRegionName())) {
					occupancyData = res.getOccupancyData();
					break;
				}
			}
			if (occupancyData.length == 0) {
				properties.put(SembientAggregatorConstant.PROPERTY_MESSAGE, SembientAggregatorConstant.NO_DATA);
			}
			// Set to 8 by default if user haven't changed the hour value.
			String hourValue = SembientAggregatorConstant.DEFAULT_WORK_HOUR;
			if (aggregatedDeviceHourMap.containsKey(deviceId)) {
				hourValue = aggregatedDeviceHourMap.get(deviceId);
			}
			aggregatedDeviceOccupancyMap.put(deviceId, occupancyData);
			for (OccupancyData data : occupancyData) {
				if (hourValue.equals(data.getHour())) {
					properties.put(SembientAggregatorConstant.PROPERTY_NUMBER_OF_OCCUPANTS, data.getOccupancy());
					String rawCapacity = properties.get(SembientAggregatorConstant.CAPACITY);
					if (rawCapacity != null) {
						int capacity = Integer.parseInt(properties.get(SembientAggregatorConstant.CAPACITY));
						float utilization = Integer.parseInt(data.getOccupancy()) / (float) capacity;
						properties.put(SembientAggregatorConstant.PROPERTY_OCCUPANCY, String.format(SembientAggregatorConstant.FLOAT_WITH_TWO_DECIMAL, utilization * 100));
					}
					properties.put(SembientAggregatorConstant.PROPERTY_USAGE_TIME_IN_MINUTE, data.getUsageTime());
					float usageTimeInPercentage = Integer.parseInt(data.getUsageTime()) / (float) 60;
					properties.put(SembientAggregatorConstant.PROPERTY_USAGE_TIME_IN_PERCENT, String.format(SembientAggregatorConstant.FLOAT_WITH_TWO_DECIMAL, usageTimeInPercentage * 100));
					break;
				}
			}
			List<String> values = new ArrayList<>();
			values.add(SembientAggregatorConstant.DEFAULT_WORK_HOUR);
			values.add(SembientAggregatorConstant.WORK_HOUR_9);
			values.add(SembientAggregatorConstant.WORK_HOUR_10);
			values.add(SembientAggregatorConstant.WORK_HOUR_11);
			values.add(SembientAggregatorConstant.WORK_HOUR_12);
			values.add(SembientAggregatorConstant.WORK_HOUR_13);
			values.add(SembientAggregatorConstant.WORK_HOUR_14);
			values.add(SembientAggregatorConstant.WORK_HOUR_15);
			values.add(SembientAggregatorConstant.WORK_HOUR_16);
			values.add(SembientAggregatorConstant.WORK_HOUR_17);
			controls.add(createDropdown(properties, SembientAggregatorConstant.PROPERTY_HOUR, values, hourValue));
			properties.put(SembientAggregatorConstant.PROPERTY_CURRENT_DATE, dateToBeDisplayed);
		} else {
			if (!properties.containsKey(SembientAggregatorConstant.PROPERTY_HOUR)) {
				properties.put(SembientAggregatorConstant.PROPERTY_MESSAGE, SembientAggregatorConstant.NO_DATA);
			}
			if (cachedTooManyRequestError.contains(firstRequest)) {
				cachedTooManyRequestError.remove(firstRequest);
				return false;
			}
		}
		return true;
	}

	/**
	 * If addressed too frequently, Sembient API may respond with 429 code, meaning that the call rate per second was reached.
	 * Normally it would rarely happen due to the request rate limit, but when it does happen - adapter must retry the
	 * attempts of retrieving needed information. This method retries up to 10 times with 500ms timeout in between to avoid the timeout exception
	 *
	 * @param url to retrieve data from
	 * @return An instance of input class
	 */
	private <T> T doGetWithRetry(String url, Class<T> clazz) {
		int retryAttempts = 0;
		Exception lastError = null;
		while (retryAttempts++ < SembientAggregatorConstant.DEFAULT_NUMBER_OF_RETRY && serviceRunning) {
			try {
				return doGet(url, clazz);
			} catch (CommandFailureException e) {
				lastError = e;
				if (e.getStatusCode() != 429) {
					// Might be 401, 403 or any other error code here so the code will just get stuck
					// cycling this failed request until it's fixed. So we need to skip this scenario.
					logger.error(String.format("Sembient API error %s while retrieving %s data", e.getStatusCode(), url), e);
					break;
				} else {
					logger.error(String.format("Sembient API error %s while retrieving %s data", e.getStatusCode(), url), e);
				}
			} catch (Exception e) {
				lastError = e;
				// if service is running, log error
				if (serviceRunning) {
					logger.error(String.format("Sembient API error while retrieving %s data", url), e);
				}
				break;
			}
			try {
				TimeUnit.MILLISECONDS.sleep(SembientAggregatorConstant.DEFAULT_RETRY_INTERVAL_FOR_MAIN_THREAD);
			} catch (InterruptedException exception) {
				//
			}
		}

		if (retryAttempts == SembientAggregatorConstant.DEFAULT_NUMBER_OF_RETRY && serviceRunning) {
			// if we got here, all 10 attempts failed
			logger.error(String.format("Failed to retrieve %s data", url), lastError);
		}
		return null;
	}

	/**
	 * If addressed too frequently, Sembient API may respond with 429 code, meaning that the call rate per second was reached.
	 * Normally it would rarely happen due to the request rate limit, but when it does happen - adapter must retry the
	 * attempts of retrieving needed information. This method cached the error request url for retry function
	 *
	 * @param url to retrieve data from
	 * @return An instance of input class
	 */
	private <T> T doGetWithRetryForWorkerThread(String url, Class<T> clazz) {
		try {
			return doGet(url, clazz);
		} catch (CommandFailureException e) {
			if (e.getStatusCode() != 429) {
				// Might be 401, 403 or any other error code here so the code will just get stuck
				// cycling this failed request until it's fixed. So we need to skip this scenario.
				logger.error(String.format("Sembient API error %s while retrieving %s data", e.getStatusCode(), url), e);
				return null;
			} else {
				logger.error(String.format("Sembient API error %s while retrieving %s data", e.getStatusCode(), url), e);
			}
		} catch (Exception e) {
			// if service is running, log error
			if (serviceRunning) {
				logger.error(String.format("Sembient API error while retrieving %s data", url), e);
			}
			return null;
		}
		cachedTooManyRequestError.add(url);
		return null;
	}

	/**
	 * Create instance of AdvancedControllableProperty type text
	 *
	 * @param stats Map of properties
	 * @param name name of the control
	 * @param stringValue initial value
	 * @return instance of AdvancedControllableProperty type text
	 */
	private AdvancedControllableProperty createText(Map<String, String> stats, String name, String stringValue) {
		if (stringValue == null) {
			stringValue = SembientAggregatorConstant.EMPTY;
		}
		stats.put(name, stringValue);
		AdvancedControllableProperty.Text text = new AdvancedControllableProperty.Text();
		return new AdvancedControllableProperty(name, new Date(), text, stringValue);
	}

	/**
	 * Create instance of AdvancedControllableProperty type button
	 *
	 * @param stats Map of properties
	 * @param name name of the control
	 * @param label label of the button
	 * @param labelPressed label after pressing the button
	 * @return instance of AdvancedControllableProperty type button
	 */
	private AdvancedControllableProperty createButton(Map<String, String> stats, String name, String label, String labelPressed) {
		AdvancedControllableProperty.Button button = new AdvancedControllableProperty.Button();
		stats.put(name, label);
		button.setLabel(label);
		button.setLabelPressed(labelPressed);
		button.setGracePeriod(0L);
		return new AdvancedControllableProperty(name, new Date(), button, SembientAggregatorConstant.EMPTY);
	}

	/***
	 * Create instance of AdvancedControllableProperty type dropdown
	 *
	 * @param stats extended statistics
	 * @param name name of the control
	 * @param values list of values
	 * @param initialValue initial value of the control
	 * @return instance of AdvancedControllableProperty type dropdown
	 */
	private AdvancedControllableProperty createDropdown(Map<String, String> stats, String name, List<String> values, String initialValue) {
		stats.put(name, initialValue);
		AdvancedControllableProperty.DropDown dropDown = new AdvancedControllableProperty.DropDown();
		dropDown.setOptions(values.toArray(new String[0]));
		dropDown.setLabels(values.toArray(new String[0]));
		return new AdvancedControllableProperty(name, new Date(), dropDown, initialValue);
	}

	/**
	 * Handle retry interval from user input
	 *
	 * @return retryInterval retry interval in long value
	 */
	private long getRetryIntervalFromUserInput() {
		long retryIntervalInLong = SembientAggregatorConstant.DEFAULT_RETRY_INTERVAL;
		try {
			if (StringUtils.isNotNullOrEmpty(getRetryInterval())) {
				retryIntervalInLong = Long.parseLong(getRetryInterval()) * 1000;
				if (retryIntervalInLong <= 0) {
					retryIntervalInLong = SembientAggregatorConstant.DEFAULT_RETRY_INTERVAL;
				}
				if (retryIntervalInLong >= SembientAggregatorConstant.REST_COMMUNICATOR_TIMEOUT) {
					retryIntervalInLong = SembientAggregatorConstant.DEFAULT_RETRY_INTERVAL;
				}
			}
		} catch (Exception e) {
			logger.error(String.format("Invalid retry interval value: %s", getRetryInterval()));
		}
		return retryIntervalInLong;
	}

	/**
	 * Handle number of retry from user input
	 *
	 * @return numberOfRetry number of retry in integer value
	 */
	private int getNumberOfRetryFromUserInput() {
		int numberOfRetry = SembientAggregatorConstant.DEFAULT_NUMBER_OF_RETRY;

		try {
			if (StringUtils.isNotNullOrEmpty(getNumberOfRetry())) {
				numberOfRetry = Integer.parseInt(getNumberOfRetry());
				if (numberOfRetry <= 0) {
					numberOfRetry = SembientAggregatorConstant.DEFAULT_NUMBER_OF_RETRY;
				}
			}
		} catch (Exception e) {
			logger.error(String.format("Invalid number of retry value: %s", getNumberOfRetry()));
		}
		return numberOfRetry;
	}
}