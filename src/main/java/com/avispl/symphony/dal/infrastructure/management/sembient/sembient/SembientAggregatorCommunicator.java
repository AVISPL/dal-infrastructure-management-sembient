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
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
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
import com.avispl.symphony.api.dal.error.ResourceNotReachableException;
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
	 * - Building & floor information will be fetched every {@link SembientAggregatorCommunicator#pollingInterval}
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
						logger.debug("Fetching region list");
					}
					fetchRegionsList();
					if (logger.isDebugEnabled()) {
						logger.debug("Fetched region list: " + aggregatedDevices);
					}
				} catch (Exception e) {
					logger.error("Error occurred during region list retrieval: " + e.getMessage() + " with cause: " + e.getCause().getMessage(), e);
				}
				if (!inProgress) {
					break mainloop;
				}
				int aggregatedDevicesCount = aggregatedDevices.size();
				if (aggregatedDevicesCount == 0) {
					continue mainloop;
				}
				while (nextDevicesCollectionIterationTimestamp > System.currentTimeMillis()) {
					try {
						TimeUnit.MILLISECONDS.sleep(1000);
					} catch (InterruptedException e) {
						//
					}
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

				// We don't want to fetch devices statuses too often, so by default it's currentTime + 30s
				// otherwise - the variable is reset by the retrieveMultipleStatistics() call, which
				// launches devices detailed statistics collection
				nextDevicesCollectionIterationTimestamp = System.currentTimeMillis() + 30000;

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
	 * Indicates whether a building and floor information(renew every {@link SembientAggregatorCommunicator#pollingInterval} minutes) is latest or not
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
	private long nextDevicesCollectionIterationTimestamp;

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
	 * Data is cached and retrieved every {@link #defaultMetaDataTimeout}
	 */
	private final ConcurrentHashMap<String, AggregatedDevice> aggregatedDevices = new ConcurrentHashMap<>();

	/**
	 * If the {@link SembientAggregatorCommunicator#deviceMetaDataRetrievalTimeout} is set to a value that is too small -
	 * devices list will be fetched too frequently. In order to avoid this - the minimal value is based on this value.
	 */
	private static final long defaultMetaDataTimeout = 60 * 1000 / 2;

	/**
	 * Device metadata retrieval timeout. The general devices list is retrieved once during this time period.
	 */
	private long deviceMetaDataRetrievalTimeout = 60 * 1000;

	/**
	 * Time period within which the device metadata (basic devices' information) cannot be refreshed.
	 * Ignored if device list is not yet retrieved or the cached device list is empty {@link SembientAggregatorCommunicator#aggregatedDevices}
	 */
	private volatile long validDeviceMetaDataRetrievalPeriodTimestamp;

	/**
	 * Whether service is running.
	 */
	private volatile boolean serviceRunning;

	/**
	 * Sets {@code deviceMetaDataInformationRetrievalTimeout}
	 *
	 * @param deviceMetaDataRetrievalTimeout the {@code long} field
	 */
	public void setDeviceMetaDataRetrievalTimeout(long deviceMetaDataRetrievalTimeout) {
		this.deviceMetaDataRetrievalTimeout = Math.max(defaultMetaDataTimeout, deviceMetaDataRetrievalTimeout);
	}

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
	 * Map with key is device id and value is value of tag in the dropdown list.
	 */
	private final ConcurrentHashMap<String, String> aggregatedDeviceTagMap = new ConcurrentHashMap<>();

	/**
	 * Map with key is device id and value is value of new tag.
	 */
	private final ConcurrentHashMap<String, String> lastNewTag = new ConcurrentHashMap<>();

	/**
	 * Map with key is device id and value is value of sensor .
	 */
	private final ConcurrentHashMap<String, String> aggregatedDeviceSensor = new ConcurrentHashMap<>();

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
	private String regionNameFilter;

	/**
	 * List of region types to be filtered
	 */
	private String regionTypeFilter;

	/**
	 * Property that define when will the adapter fetch new data of building and floor -
	 * then store to {@link SembientAggregatorCommunicator#cachedBuildings}
	 */
	private String pollingInterval;

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
	 * Retrieves {@link #regionNameFilter}
	 *
	 * @return value of {@link #regionNameFilter}
	 */
	public String getRegionNameFilter() {
		return regionNameFilter;
	}

	/**
	 * Sets {@link #regionNameFilter} value
	 *
	 * @param regionNameFilter new value of {@link #regionNameFilter}
	 */
	public void setRegionNameFilter(String regionNameFilter) {
		this.regionNameFilter = regionNameFilter;
	}

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
	 * Retrieves {@link #pollingInterval}
	 *
	 * @return value of {@link #pollingInterval}
	 */
	public String getPollingInterval() {
		return pollingInterval;
	}

	/**
	 * Sets {@link #pollingInterval} value
	 *
	 * @param pollingInterval new value of {@link #pollingInterval}
	 */
	public void setPollingInterval(String pollingInterval) {
		this.pollingInterval = pollingInterval;
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
	 * {@inheritDoc}
	 */
	@Override
	protected void internalInit() throws Exception {
		this.setTrustAllCertificates(true);
		// Init thread
		executorService = Executors.newFixedThreadPool(SembientAggregatorConstant.MAX_NO_THREADS);
		executorService.submit(deviceDataLoader = new SembientDeviceDataLoader());

		validDeviceMetaDataRetrievalPeriodTimestamp = System.currentTimeMillis();
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
		lastNewTag.clear();
		aggregatedDeviceSensor.clear();
		aggregatedDevices.clear();
		super.internalDestroy();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void controlProperty(ControllableProperty controllableProperty) throws Exception {
		reentrantLock.lock();
		try {
			String deviceId = controllableProperty.getDeviceId();
			String[] properties = controllableProperty.getProperty().split(SembientAggregatorConstant.HASH);
			String groupName = properties[0];
			String propertyName = properties[1];
			AggregatedDevice deviceToBeControlled = aggregatedDevices.get(deviceId);
			String[] deviceDetails = deviceToBeControlled.getDeviceId().split(SembientAggregatorConstant.DASH);
			String buildingName = deviceDetails[0];
			String floorName = deviceDetails[1];
			String regionName = deviceDetails[2];

			Map<String, String> statFromCached = deviceToBeControlled.getProperties();
			List<AdvancedControllableProperty> controlFromCached = deviceToBeControlled.getControllableProperties();
			if (SembientAggregatorConstant.OCCUPANCY_LIST.equals(groupName) && SembientAggregatorConstant.HOUR.equals(propertyName)) {
				// Get current date:
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern(SembientAggregatorConstant.YYYY_MM_DD);
				LocalDate now = LocalDate.now(ZoneId.of(SembientAggregatorConstant.UTC_TIMEZONE));
				// Get yesterday:
				LocalDate yesterday = LocalDate.now(ZoneId.of(SembientAggregatorConstant.UTC_TIMEZONE)).minusDays(1);
				String currentDate = formatter.format(now);
				String yesterdayDate = formatter.format(yesterday);
				aggregatedDeviceHourMap.put(deviceId, (String) controllableProperty.getValue());
				populateOccupancyData(statFromCached, controlFromCached, currentDate, yesterdayDate, deviceId, buildingName, floorName, regionName);
			} else if (SembientAggregatorConstant.REGION_TAG.equals(groupName)) {
				switch (propertyName) {
					case SembientAggregatorConstant.NEW_TAG:
						String newTagValue = (String) controllableProperty.getValue();
						if (StringUtils.isNullOrEmpty(newTagValue)) {
							throw new ResourceNotReachableException("Cannot create new region tag with value is empty or null");
						}
						lastNewTag.put(deviceId, (String) controllableProperty.getValue());
						statFromCached.put(SembientAggregatorConstant.REGION_TAG_NEW_TAG, (String) controllableProperty.getValue());
						for (AdvancedControllableProperty control : controlFromCached) {
							if (control.getName().equals(controllableProperty.getProperty())) {
								control.setTimestamp(new Date());
								control.setValue(controllableProperty.getValue());
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
					case SembientAggregatorConstant.LABEL_CREATE:
						String newTag = lastNewTag.get(deviceId);
						if (StringUtils.isNullOrEmpty(newTag)) {
							throw new ResourceNotReachableException("NewTag value cannot be empty.");
						}
						StringBuilder createRequestBuilder = new StringBuilder();
						createRequestBuilder.append(SembientAggregatorConstant.COMMAND_SPACE_TAGS).append(loginResponse.getCustomerId()).append(SembientAggregatorConstant.SLASH).append(buildingName)
								.append(SembientAggregatorConstant.SLASH).append(floorName).append(SembientAggregatorConstant.PARAM_REGION_NAME).append(regionName).append(SembientAggregatorConstant.PARAM_REGION_TAGS)
								.append(newTag);
						RegionTagWrapperControl createRegionTagWrapperControl = null;
						try {
							createRegionTagWrapperControl = this.doPut(createRequestBuilder.toString(), null, RegionTagWrapperControl.class);
						} catch (CommandFailureException e) {
							logger.error("Fail to create with status code: " + e.getStatusCode() + ", value: " + newTag, e);
							if (e.getStatusCode() == 429) {
								throw new ResourceNotReachableException("Too many request, please try to create region tag with value: " + newTag + " later.");
							} else {
								throw new ResourceNotReachableException("Fail to create region tag with value: " + newTag);
							}
						} catch (Exception e) {
							logger.error("Exception occur when creating region tag with value: " + newTag, e);
							throw new ResourceNotReachableException("Fail to create region tag with value: " + newTag);
						}
						if (createRegionTagWrapperControl != null) {
							if (!SembientAggregatorConstant.STATUS_CODE_200.equals(createRegionTagWrapperControl.getStatusCode())) {
								throw new CommandFailureException("Fail to create region with value is: " + controllableProperty.getValue()
										, createRequestBuilder.toString(), createRegionTagWrapperControl.toString());
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
							newOptions.add(newTag);
							controlFromCached.removeIf(
									advancedControllableProperty -> advancedControllableProperty.getName().equals(SembientAggregatorConstant.PROPERTY_TAG));
							controlFromCached.removeIf(
									advancedControllableProperty -> advancedControllableProperty.getName().equals(SembientAggregatorConstant.REGION_TAG_NEW_TAG));
							controlFromCached.add(createDropdown(statFromCached, SembientAggregatorConstant.PROPERTY_TAG, newOptions, newOptions.get(0)));
							controlFromCached.add(createText(statFromCached, SembientAggregatorConstant.REGION_TAG_NEW_TAG, lastNewTag.get(deviceId)));
						} else {
							logger.error("Error while creating region with status code: 429 and value " + newTag);
							throw new ResourceNotReachableException("Too many requests sent to the device, please try to create one more time with value: " + newTag);
						}
						break;
					case SembientAggregatorConstant.LABEL_DELETE:
						String valueToBeDelete = aggregatedDeviceTagMap.get(deviceId);
						if (StringUtils.isNullOrEmpty(valueToBeDelete)) {
							throw new ResourceNotReachableException("Tag dropdowns value cannot be empty.");
						}
						String deleteRequest = SembientAggregatorConstant.COMMAND_SPACE_TAGS + loginResponse.getCustomerId() + SembientAggregatorConstant.SLASH + buildingName + SembientAggregatorConstant.SLASH
								+ floorName + SembientAggregatorConstant.PARAM_REGION_NAME + regionName + SembientAggregatorConstant.PARAM_REGION_TAGS + valueToBeDelete;
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
								controlFromCached.removeIf(
										advancedControllableProperty -> advancedControllableProperty.getName().equals(SembientAggregatorConstant.PROPERTY_DELETE));
								controlFromCached.removeIf(
										advancedControllableProperty -> advancedControllableProperty.getName().equals(SembientAggregatorConstant.PROPERTY_TAG));
								break;
							}
							controlFromCached.removeIf(
									advancedControllableProperty -> advancedControllableProperty.getName().equals(SembientAggregatorConstant.PROPERTY_TAG));
							controlFromCached.add(createDropdown(statFromCached, SembientAggregatorConstant.PROPERTY_TAG, options, options.get(0)));
						} catch (CommandFailureException e) {
							logger.error("Fail to delete with status code: " + e.getStatusCode() + ", value: " + valueToBeDelete, e);
							if (e.getStatusCode() == 429) {
								throw new ResourceNotReachableException("Too many request, please try to delete with value: " + valueToBeDelete + " later.");
							} else {
								throw new ResourceNotReachableException("Fail to delete region tag with value: " + valueToBeDelete);
							}
						} catch (Exception e) {
							logger.error("Exception occur when deleting region tag with value: " + valueToBeDelete, e);
							throw new ResourceNotReachableException("Fail to delete region tag with value: " + valueToBeDelete);
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
			// Put NextPollingInterval properties to stats map
			DateFormat obj = new SimpleDateFormat(SembientAggregatorConstant.DATE_ISO_FORMAT);
			obj.setTimeZone(TimeZone.getTimeZone(SembientAggregatorConstant.UTC_TIMEZONE));
			newStatistics.put(SembientAggregatorConstant.NEXT_POLLING_INTERVAL, obj.format(validDeviceMetaDataRetrievalPeriodTimestamp));
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
								if (filter.equals(floorName)) {
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
		reentrantLock.lock();
		try {
			sembientLogin();
			if (executorService == null) {
				// Due to the bug that after changing properties on fly - the adapter is destroyed but adapter is not initialized properly,
				// so executor service is not running. We need to make sure executorService exists
				executorService = Executors.newFixedThreadPool(SembientAggregatorConstant.MAX_NO_THREADS);
				executorService.submit(deviceDataLoader = new SembientDeviceDataLoader());
			}

			long currentTimestamp = System.currentTimeMillis();
			nextDevicesCollectionIterationTimestamp = currentTimestamp;
			updateValidRetrieveStatisticsTimestamp();
			aggregatedDevices.values().forEach(aggregatedDevice -> aggregatedDevice.setTimestamp(currentTimestamp));
			return new ArrayList<>(aggregatedDevices.values());
		} finally {
			reentrantLock.unlock();
		}
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
		return retrieveMultipleStatistics()
				.stream()
				.filter(aggregatedDevice -> list.contains(aggregatedDevice.getDeviceId()))
				.collect(Collectors.toList());
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
				throw new FailedLoginException(String.format("Fail to login with username: %s, password: %s", this.getLogin(), this.getPassword()));
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
		if (cachedBuildings.size() > 0 && validDeviceMetaDataRetrievalPeriodTimestamp > currentTimestamp) {
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Building and floor retrieval is in cooldown. %s seconds left",
						(validDeviceMetaDataRetrievalPeriodTimestamp - currentTimestamp) / 1000));
			}
			return;
		}
		// Apply polling interval
		if (StringUtils.isNotNullOrEmpty(pollingInterval)) {
			long interval;
			try {
				interval = Integer.parseInt(pollingInterval) * SembientAggregatorConstant.MINUTE_TO_MS;
			} catch (Exception e) {
				logger.error("Invalid format, pollingInterval should be integer.", e);
				interval = deviceMetaDataRetrievalTimeout;
			}
			validDeviceMetaDataRetrievalPeriodTimestamp = currentTimestamp + interval;
		} else {
			validDeviceMetaDataRetrievalPeriodTimestamp = currentTimestamp + deviceMetaDataRetrievalTimeout;
		}
		latestBuildingAndFloorData = true;
		BuildingWrapper buildingWrapper = this.doGetWithRetry(SembientAggregatorConstant.COMMAND_SPACE_BUILDINGS + loginResponse.getCustomerId(), BuildingWrapper.class);
		if (buildingWrapper != null) {
			cachedBuildings.clear();
			cachedBuildings.addAll(Arrays.asList(buildingWrapper.getBuildingResponse()));
		}
	}

	/**
	 * Fetch the latest list of buildings, floors and regions
	 *
	 * @throws Exception if fail to fetch buildings, floors and regions.
	 */
	private void fetchRegionsList() throws Exception {
		long currentTimestamp = System.currentTimeMillis();
		if (!latestBuildingAndFloorData) {
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Region meta data retrieval is in cooldown. %s seconds left",
						(validDeviceMetaDataRetrievalPeriodTimestamp - currentTimestamp) / 1000));
			}
			return;
		}
		// Filter building:
		if (StringUtils.isNullOrEmpty(buildingFilter) && cachedBuildings.stream().findFirst().isPresent()) {
			String buildingID = cachedBuildings.stream().findFirst().get().getBuildingID();
			String[] floorNames = cachedBuildings.stream().findFirst().get().getFloors();
			// Filter by floors
			filterByFloors(buildingID, floorNames);
		} else {
			for (BuildingResponse response : cachedBuildings) {
				if (response.getBuildingName().equals(buildingFilter.trim())) {
					String buildingID = response.getBuildingID();
					String[] floorNames = response.getFloors();
					filterByFloors(buildingID, floorNames);
					break;
				}
			}
		}
		// Notify worker thread that it's a valid time to start fetching details device information.
		latestBuildingAndFloorData = false;
		nextDevicesCollectionIterationTimestamp = System.currentTimeMillis();
	}

	/**
	 * Filter aggregated devices by floorNames
	 *
	 * @param buildingID ID of the building
	 * @param floorNames Floor names
	 * @throws Exception if fail to retrieve regions.
	 */
	private void filterByFloors(String buildingID, String[] floorNames) throws Exception {
		if (StringUtils.isNotNullOrEmpty(floorFilter)) {
			String[] listFloorToBeFilter = floorFilter.split(SembientAggregatorConstant.COMMA);
			for (String floor : listFloorToBeFilter) {
				for (String floorName : floorNames) {
					if (floor.trim().equals(floorName)) {
						// Filter by region type
						if (StringUtils.isNotNullOrEmpty(regionTypeFilter)) {
							String[] listTypeToBeFilter = regionTypeFilter.split(SembientAggregatorConstant.COMMA);
							for (String type : listTypeToBeFilter) {
								retrieveRegions(buildingID, floor, type);
							}
						} else {
							retrieveRegions(buildingID, floor, null);
						}
					}
				}
			}
		} else {
			for (String floorName : floorNames) {
				if (StringUtils.isNotNullOrEmpty(regionTypeFilter)) {
					String[] listTypeToBeFilter = regionTypeFilter.split(SembientAggregatorConstant.COMMA);
					for (String type : listTypeToBeFilter) {
						retrieveRegions(buildingID, floorName, type);
					}
				} else {
					retrieveRegions(buildingID, floorName, null);
				}
			}
		}
	}

	/**
	 * Retrieve all regions in building & floor
	 *
	 * @param buildingName building name
	 * @param floorName floor name
	 * @param regionType type of region
	 * @throws Exception if fail to get region
	 */
	void retrieveRegions(String buildingName, String floorName, String regionType) throws Exception {
		String request;
		if (regionType != null) {
			request =
					SembientAggregatorConstant.COMMAND_SPACE_REGIONS + this.loginResponse.getCustomerId() + SembientAggregatorConstant.SLASH + buildingName + SembientAggregatorConstant.SLASH + floorName
							+ SembientAggregatorConstant.PARAM_REGION_TYPE + regionType;
		} else {
			request = SembientAggregatorConstant.COMMAND_SPACE_REGIONS + this.loginResponse.getCustomerId() + SembientAggregatorConstant.SLASH + buildingName + SembientAggregatorConstant.SLASH + floorName;
		}
		RegionWrapper regionWrapper = this.doGetWithRetry(request, RegionWrapper.class);
		if (regionWrapper != null) {
			RegionResponse[] regionResponses = regionWrapper.getRegionResponse();
			if (regionResponses.length != 0) {
				fetchRegionMetadata(buildingName, floorName, regionResponses);
			}
		}
	}

	/**
	 * Populate region details information
	 *
	 * @param aggregatedDevice Aggregated device that get from {@link SembientAggregatorCommunicator#fetchRegionsList}
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
		String[] rawBuildingInfo = deviceId.split(SembientAggregatorConstant.DASH);
		String buildingName = rawBuildingInfo[0];
		String floorName = rawBuildingInfo[1];
		String regionName = rawBuildingInfo[2];
		// Retrieve occupancy data
		populateOccupancyData(properties, controls, currentDate, yesterdayDate, deviceId, buildingName, floorName, regionName);
		// Retrieve thermal data
		populateThermalData(properties, currentDate, yesterdayDate, buildingName, floorName, regionName);
		// Retrieve IAQ data
		populateIAQData(properties, currentDate, yesterdayDate, buildingName, floorName, regionName);
		// Retrieve region tags
		populateRegionTag(properties, controls, deviceId);
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
	 * @throws Exception when fail to get region tags
	 */
	private void populateRegionTag(Map<String, String> properties, List<AdvancedControllableProperty> controls, String deviceId) throws Exception {
		controls.add(createText(properties, SembientAggregatorConstant.REGION_TAG_NEW_TAG, lastNewTag.get(deviceId)));
		controls.add(createButton(properties, SembientAggregatorConstant.REGION_TAG_CREATE, SembientAggregatorConstant.LABEL_CREATE, SembientAggregatorConstant.LABEL_PRESSED_CREATING));
		String[] regionDetails = deviceId.split(SembientAggregatorConstant.DASH);
		String buildingName = regionDetails[0];
		String floorName = regionDetails[1];
		String regionName = regionDetails[2];
		String request =
				SembientAggregatorConstant.COMMAND_SPACE_TAGS + this.loginResponse.getCustomerId() + SembientAggregatorConstant.SLASH + buildingName + SembientAggregatorConstant.SLASH + floorName
						+ SembientAggregatorConstant.PARAM_REGION_NAME
						+ regionName;
		RegionTagWrapperMonitor regionTagWrapperControl = this.doGetWithRetry(request, RegionTagWrapperMonitor.class);
		// Get getRegionResponse by first index because it only has 1 element.
		// There are some cases that getRegionResponse array is empty
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
	 * @param buildingName building name
	 * @param floorName floor name
	 * @param regionName region name
	 * @throws Exception if fail to get {@link AirQualityWrapper}
	 */
	private void populateIAQData(Map<String, String> properties, String currentDate, String yesterdayDate, String buildingName, String floorName, String regionName) throws Exception {
		AirQualityWrapper airQualityWrapper = doGetWithRetry(
				SembientAggregatorConstant.COMMAND_IAQ_TIMESERIES + loginResponse.getCustomerId() + SembientAggregatorConstant.SLASH + buildingName + SembientAggregatorConstant.SLASH
						+ floorName + SembientAggregatorConstant.SLASH + currentDate, AirQualityWrapper.class);
		if (airQualityWrapper != null) {
			AirQualitySensorResponse[] airQualitySensorResponses = new AirQualitySensorResponse[0];
			if (SembientAggregatorConstant.STATUS_CODE_200.equals(airQualityWrapper.getStatusCode()) && airQualityWrapper.getAirQualitySensorWrapper() != null) {
				airQualitySensorResponses = airQualityWrapper.getAirQualitySensorWrapper().getAirQualitySensorResponses();
			}
			if (airQualitySensorResponses.length == 0) {
				airQualityWrapper = doGetWithRetry(
						SembientAggregatorConstant.COMMAND_IAQ_TIMESERIES + loginResponse.getCustomerId() + SembientAggregatorConstant.SLASH + buildingName + SembientAggregatorConstant.SLASH
								+ floorName + SembientAggregatorConstant.SLASH + yesterdayDate, AirQualityWrapper.class);
				if (airQualityWrapper != null) {
					if (SembientAggregatorConstant.STATUS_CODE_200.equals(airQualityWrapper.getStatusCode()) && airQualityWrapper.getAirQualitySensorWrapper() != null) {
						airQualitySensorResponses = airQualityWrapper.getAirQualitySensorWrapper().getAirQualitySensorResponses();
					}
					if (airQualitySensorResponses.length == 0) {
						populateNoData(properties, SembientAggregatorConstant.AIR_QUALITY);
						return;
					}
				}
			}
			Map<String, AirQualityData[]> sensorAndIAQMap = new HashMap<>();
			for (AirQualitySensorResponse airQualitySensorResponse : airQualitySensorResponses) {
				String[] regions = airQualitySensorResponse.getRegionName().split(SembientAggregatorConstant.COMMA);
				if (Arrays.asList(regions).contains(regionName)) {
					sensorAndIAQMap.put(airQualitySensorResponse.getSensorName(), airQualitySensorResponse.getAirQualityData());
				}
			}
			boolean isPopulated = false;
			for (Map.Entry<String, AirQualityData[]> entry : sensorAndIAQMap.entrySet()
			) {
				// Remove previous properties
				String co2Property = SembientAggregatorConstant.SENSOR + entry.getKey() + SembientAggregatorConstant.DASH + SembientAggregatorConstant.AIR_QUALITY + SembientAggregatorConstant.HASH
						+ SembientAggregatorConstant.CO2_VALUE;
				String tvocProperty = SembientAggregatorConstant.SENSOR + entry.getKey() + SembientAggregatorConstant.DASH + SembientAggregatorConstant.AIR_QUALITY + SembientAggregatorConstant.HASH
						+ SembientAggregatorConstant.TVOCVALUE_MICROGRAM;
				String pm25Property = SembientAggregatorConstant.SENSOR + entry.getKey() + SembientAggregatorConstant.DASH + SembientAggregatorConstant.AIR_QUALITY + SembientAggregatorConstant.HASH
						+ SembientAggregatorConstant.PM_25_VALUE_MICROMET;
				String lastUpdateProperty = SembientAggregatorConstant.SENSOR + entry.getKey() + SembientAggregatorConstant.DASH + SembientAggregatorConstant.AIR_QUALITY + SembientAggregatorConstant.HASH
						+ SembientAggregatorConstant.LAST_UPDATE;
				String recentDataProperty = SembientAggregatorConstant.SENSOR + entry.getKey() + SembientAggregatorConstant.DASH + SembientAggregatorConstant.AIR_QUALITY + SembientAggregatorConstant.HASH
						+ SembientAggregatorConstant.RECENT_DATA;
				String messageProperty = SembientAggregatorConstant.SENSOR + entry.getKey() + SembientAggregatorConstant.DASH + SembientAggregatorConstant.AIR_QUALITY + SembientAggregatorConstant.HASH
						+ SembientAggregatorConstant.MESSAGE;
				properties.remove(co2Property);
				properties.remove(tvocProperty);
				properties.remove(pm25Property);
				properties.remove(lastUpdateProperty);
				properties.remove(recentDataProperty);
				properties.remove(messageProperty);
				//
				int lastIndex = entry.getValue().length - 1;
				properties.put(co2Property, entry.getValue()[lastIndex].getCo2());
				properties.put(tvocProperty, entry.getValue()[lastIndex].getTvoc());
				properties.put(pm25Property, entry.getValue()[lastIndex].getPm25());
				if (!isPopulated) {
					DateFormat obj = new SimpleDateFormat(SembientAggregatorConstant.DATE_ISO_FORMAT);
					obj.setTimeZone(TimeZone.getTimeZone(SembientAggregatorConstant.UTC_TIMEZONE));
					// Convert s to ms
					Date res = new Date(entry.getValue()[lastIndex].getTimestamp() * 1000);
					long resInMs = res.getTime();
					long currentTimeMs = System.currentTimeMillis();

					long dif = currentTimeMs - resInMs;
					long hourInMs = 3600 * 1000;
					boolean isRecentData = (dif) < hourInMs;
					properties.put(recentDataProperty, String.valueOf(isRecentData));
					// now we format the res by using SimpleDateFormat
					properties.put(lastUpdateProperty, obj.format(res));
					isPopulated = true;
				}
			}
		}
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
	 * @param buildingName building name
	 * @param floorName floor name
	 * @param regionName region name
	 * @throws Exception if fail to get {@link ThermalWrapper}
	 */
	private void populateThermalData(Map<String, String> properties, String currentDate, String yesterdayDate, String buildingName, String floorName, String regionName) throws Exception {
		ThermalWrapper thermalWrapper = doGetWithRetry(
				SembientAggregatorConstant.COMMAND_THERMAL_TIMESERIES + loginResponse.getCustomerId() + SembientAggregatorConstant.SLASH + buildingName + SembientAggregatorConstant.SLASH
						+ floorName + SembientAggregatorConstant.SLASH + currentDate, ThermalWrapper.class);
		if (thermalWrapper != null) {
			ThermalSensorResponse[] thermalSensorResponse = new ThermalSensorResponse[0];
			if (SembientAggregatorConstant.STATUS_CODE_200.equals(thermalWrapper.getStatusCode()) && thermalWrapper.getThermalSensorWrappers() != null) {
				thermalSensorResponse = thermalWrapper.getThermalSensorWrappers().getThermalSensorResponses();
			}
			if (thermalSensorResponse.length == 0) {
				// Retry with yesterday data
				thermalWrapper = doGetWithRetry(
						SembientAggregatorConstant.COMMAND_THERMAL_TIMESERIES + loginResponse.getCustomerId() + SembientAggregatorConstant.SLASH + buildingName + SembientAggregatorConstant.SLASH
								+ floorName + SembientAggregatorConstant.SLASH + yesterdayDate, ThermalWrapper.class);
				if (thermalWrapper != null) {
					if (SembientAggregatorConstant.STATUS_CODE_200.equals(thermalWrapper.getStatusCode()) && thermalWrapper.getThermalSensorWrappers() != null) {
						thermalSensorResponse = thermalWrapper.getThermalSensorWrappers().getThermalSensorResponses();
					}
					if (thermalSensorResponse.length == 0) {
						populateNoData(properties, SembientAggregatorConstant.THERMAL);
						return;
					}
				}
			}

			Map<String, ThermalData[]> sensorAndThermalMap = new HashMap<>();
			for (ThermalSensorResponse sensorResponse : thermalSensorResponse) {
				String[] regions = sensorResponse.getRegionName().split(SembientAggregatorConstant.COMMA);
				if (Arrays.asList(regions).contains(regionName)) {
					sensorAndThermalMap.put(sensorResponse.getSensorName(), sensorResponse.getThermalData());
				}
			}
			boolean isPopulated = false;
			for (Map.Entry<String, ThermalData[]> entry : sensorAndThermalMap.entrySet()
			) {
				String sensorTemperatureProperty = SembientAggregatorConstant.SENSOR + entry.getKey() + SembientAggregatorConstant.DASH + SembientAggregatorConstant.THERMAL + SembientAggregatorConstant.HASH
						+ SembientAggregatorConstant.TEMPERATURE_F;
				String sensorLastUpdateProperty = SembientAggregatorConstant.SENSOR + entry.getKey() + SembientAggregatorConstant.DASH + SembientAggregatorConstant.THERMAL + SembientAggregatorConstant.HASH
						+ SembientAggregatorConstant.LAST_UPDATE;
				String sensorRecentDataProperty = SembientAggregatorConstant.SENSOR + entry.getKey() + SembientAggregatorConstant.DASH + SembientAggregatorConstant.THERMAL + SembientAggregatorConstant.HASH
						+ SembientAggregatorConstant.RECENT_DATA;
				String sensorHumidityProperty = SembientAggregatorConstant.SENSOR + entry.getKey() + SembientAggregatorConstant.DASH + SembientAggregatorConstant.THERMAL + SembientAggregatorConstant.HASH
						+ SembientAggregatorConstant.HUMIDITY;
				String sensorMessageProperty = SembientAggregatorConstant.SENSOR + entry.getKey() + SembientAggregatorConstant.DASH + SembientAggregatorConstant.THERMAL + SembientAggregatorConstant.HASH
						+ SembientAggregatorConstant.MESSAGE;
				// Remove previous properties
				properties.remove(sensorTemperatureProperty);
				properties.remove(sensorLastUpdateProperty);
				properties.remove(sensorRecentDataProperty);
				properties.remove(sensorHumidityProperty);
				properties.remove(sensorMessageProperty);
				//
				int lastIndex = entry.getValue().length - 1;
				properties.put(sensorTemperatureProperty, entry.getValue()[lastIndex].getTemperature());
				properties.put(sensorHumidityProperty, entry.getValue()[lastIndex].getHumidity());
				if (!isPopulated) {
					DateFormat obj = new SimpleDateFormat(SembientAggregatorConstant.DATE_ISO_FORMAT);
					obj.setTimeZone(TimeZone.getTimeZone(SembientAggregatorConstant.UTC_TIMEZONE));
					// Convert s to ms
					Date res = new Date(entry.getValue()[lastIndex].getTimestamp() * 1000);
					long resInMs = res.getTime();
					long currentTimeMs = System.currentTimeMillis();

					long dif = currentTimeMs - resInMs;
					long hourInMs = 3600 * 1000;
					boolean isRecentData = (dif) < hourInMs;
					properties.put(sensorRecentDataProperty, String.valueOf(isRecentData));
					properties.put(sensorLastUpdateProperty, obj.format(res));
					isPopulated = true;
				}
			}
		}
	}

	/**
	 * Populate no data message
	 *
	 * @param properties Map of statistics
	 * @param groupType type of the group
	 */
	private void populateNoData(Map<String, String> properties, String groupType) {
		for (Entry<String, String> entry : aggregatedDeviceSensor.entrySet()
		) {
			properties.put(SembientAggregatorConstant.SENSOR + entry.getValue() + SembientAggregatorConstant.DASH + groupType + SembientAggregatorConstant.HASH
					+ SembientAggregatorConstant.MESSAGE, SembientAggregatorConstant.NO_DATA);
		}
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
	 * @param buildingName building name
	 * @param floorName floor name
	 * @param regionName region name
	 * @throws Exception If fail to get {@link OccupancyWrapper} data.
	 */
	private void populateOccupancyData(Map<String, String> properties, List<AdvancedControllableProperty> controls, String currentDate, String yesterdayDate,
			String deviceId, String buildingName, String floorName, String regionName)
			throws Exception {
		// Retrieve data from today
		String dateToBeDisplayed = currentDate;
		OccupancyWrapper occupancyWrapper = this.doGetWithRetry(SembientAggregatorConstant.COMMAND_OCCUPANCY_TIMESERIES + loginResponse.getCustomerId() + SembientAggregatorConstant.SLASH
				+ buildingName + SembientAggregatorConstant.SLASH
				+ floorName + SembientAggregatorConstant.SLASH + currentDate, OccupancyWrapper.class);
		if (occupancyWrapper != null) {
			properties.remove(SembientAggregatorConstant.PROPERTY_MESSAGE);
			OccupancyRegionResponse[] occupancyRegionResponses = new OccupancyRegionResponse[0];
			if (SembientAggregatorConstant.STATUS_CODE_200.equals(occupancyWrapper.getStatusCode()) && occupancyWrapper.getOccupancyRegionWrappers() != null) {
				occupancyRegionResponses = occupancyWrapper.getOccupancyRegionWrappers().getOccupancyRegionResponses();
			}
			if (occupancyRegionResponses.length == 0) {
				// Retry one more time with yesterday data.
				occupancyWrapper = this.doGetWithRetry(
						SembientAggregatorConstant.COMMAND_OCCUPANCY_TIMESERIES + loginResponse.getCustomerId() + SembientAggregatorConstant.SLASH + buildingName + SembientAggregatorConstant.SLASH
								+ floorName + SembientAggregatorConstant.SLASH + yesterdayDate, OccupancyWrapper.class);
				if (occupancyWrapper != null) {
					if (SembientAggregatorConstant.STATUS_CODE_200.equals(occupancyWrapper.getStatusCode()) && occupancyWrapper.getOccupancyRegionWrappers() != null) {
						occupancyRegionResponses = occupancyWrapper.getOccupancyRegionWrappers().getOccupancyRegionResponses();
					}
					if (occupancyRegionResponses.length == 0) {
						properties.put(SembientAggregatorConstant.PROPERTY_MESSAGE, SembientAggregatorConstant.NO_DATA);
						return;
					}
					dateToBeDisplayed = yesterdayDate;
				} else {
					properties.put(SembientAggregatorConstant.PROPERTY_MESSAGE, SembientAggregatorConstant.NO_DATA);
					return;
				}
			}
			// Remove previous properties
			properties.remove(SembientAggregatorConstant.PROPERTY_HOUR);
			properties.remove(SembientAggregatorConstant.PROPERTY_CURRENT_DATE);
			properties.remove(SembientAggregatorConstant.PROPERTY_MESSAGE);
			properties.remove(SembientAggregatorConstant.PROPERTY_NUMBER_OF_OCCUPANCE);
			properties.remove(SembientAggregatorConstant.PROPERTY_USAGE_TIME);
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
			for (OccupancyData data : occupancyData) {
				if (hourValue.equals(data.getHour())) {
					properties.put(SembientAggregatorConstant.PROPERTY_NUMBER_OF_OCCUPANCE, data.getOccupancy());
					properties.put(SembientAggregatorConstant.PROPERTY_USAGE_TIME, data.getUsageTime());
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
		}
	}

	/**
	 * If addressed too frequently, Sembient API may respond with 429 code, meaning that the call rate per second was reached.
	 * Normally it would rarely happen due to the request rate limit, but when it does happen - adapter must retry the
	 * attempts of retrieving needed information. This method retries up to 10 times with 500ms timeout in between
	 *
	 * @param url to retrieve data from
	 * @return JsonNode response body
	 * @throws Exception if a communication error occurs
	 */
	private <T> T doGetWithRetry(String url, Class<T> clazz) throws Exception {
		int retryAttempts = 0;
		Exception lastError = null;

		while (retryAttempts++ < SembientAggregatorConstant.MAXIMUM_RETRY && serviceRunning) {
			try {
				return doGet(url, clazz);
			} catch (CommandFailureException e) {
				lastError = e;
				if (e.getStatusCode() != 429) {
					// Might be 401, 403 or any other error code here so the code will just get stuck
					// cycling this failed request until it's fixed. So we need to skip this scenario.
					logger.error(String.format("Sembient API error %s while retrieving %s data", e.getStatusCode(), url), e);
					break;
				}
			} catch (Exception e) {
				lastError = e;
				// if service is running, log error
				if (serviceRunning) {
					logger.error(String.format("Sembient API error while retrieving %s data", url), e);
				}
				break;
			}
			TimeUnit.MILLISECONDS.sleep(200);
		}

		if (retryAttempts == SembientAggregatorConstant.MAXIMUM_RETRY && serviceRunning) {
			// if we got here, all 10 attempts failed
			logger.error(String.format("Failed to retrieve %s data", url), lastError);
		}
		return null;
	}

	/**
	 * Fetch metadata for region (region name, type, sensor id, region tags)
	 *
	 * @param buildingName building name
	 * @param floorName floor name
	 * @param regionResponses Array of region responses
	 */
	private void fetchRegionMetadata(String buildingName, String floorName, RegionResponse[] regionResponses) {
		for (RegionResponse region : regionResponses) {
			boolean isContinue = false;
			// Filter by region name:
			if (StringUtils.isNotNullOrEmpty(regionNameFilter)) {
				String[] regionNames = regionNameFilter.split(SembientAggregatorConstant.COMMA);
				for (String regionName : regionNames) {
					if (regionName.trim().equals(region.getRegionName())) {
						isContinue = true;
						break;
					}
				}
			}
			if (StringUtils.isNotNullOrEmpty(regionNameFilter) && !isContinue) {
				continue;
			}
			AggregatedDevice aggregatedDevice = new AggregatedDevice();
			// Response doesn't contain any id, in order to make device id unique we create a combination of building, floor and region --
			// For instance: BuildingA-Floor1-Region1
			aggregatedDevice.setDeviceId(buildingName + SembientAggregatorConstant.DASH + floorName + SembientAggregatorConstant.DASH + region.getRegionName());
			aggregatedDevice.setCategory(SembientAggregatorConstant.REGION);
			aggregatedDevice.setDeviceModel(region.getRegionType());
			aggregatedDevice.setDeviceOnline(true);
			aggregatedDevice.setDeviceName(region.getRegionName());
			if (region.getSensors() != null) {
				for (int i = 0; i < region.getSensors().length; i++) {
					aggregatedDeviceSensor.put(aggregatedDevice.getDeviceId(), region.getSensors()[i]);
				}
			}
			// occupancy, thermal, iaq data will be populated later on.
			if (aggregatedDevices.get(aggregatedDevice.getDeviceId()) != null) {
				Map<String, String> propertiesFromCached = aggregatedDevices.get(aggregatedDevice.getDeviceId()).getProperties();
				List<AdvancedControllableProperty> controlsFromCached = aggregatedDevices.get(aggregatedDevice.getDeviceId()).getControllableProperties();
				aggregatedDevice.setProperties(propertiesFromCached);
				aggregatedDevice.setControllableProperties(controlsFromCached);
			} else {
				Map<String, String> properties = new HashMap<>();
				aggregatedDevice.setProperties(properties);
				aggregatedDevice.setControllableProperties(new ArrayList<>());
			}
			aggregatedDevices.put(aggregatedDevice.getDeviceId(), aggregatedDevice);
		}
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
}