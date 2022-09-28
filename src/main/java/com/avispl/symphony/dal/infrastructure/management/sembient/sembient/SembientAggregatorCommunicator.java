package com.avispl.symphony.dal.infrastructure.management.sembient.sembient;

import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
	 * Process is running constantly and triggers collecting data from Sembient API endpoints every 30 seconds
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
					TimeUnit.MILLISECONDS.sleep(500);
				} catch (InterruptedException e) {
					// Ignore for now
				}
				// Wait for getMultipleStatistics() call login first, then worker thread will start colling device (region) information.
				if (loginResponse == null) {
					continue mainloop;
				}
				if (!inProgress) {
					break mainloop;
				}

				// next line will determine whether Sembient monitoring was paused
				updateAggregatorStatus();
				if (devicePaused) {
					continue mainloop;
				}

				try {
					if (logger.isDebugEnabled()) {
						logger.debug("Fetching devices list");
					}
					fetchRegionsList();

					if (logger.isDebugEnabled()) {
						logger.debug("Fetched region metadata list: " + aggregatedDevices);
					}
				} catch (Exception e) {
					logger.error("Error occurred during region metadata list retrieval: " + e.getMessage() + " with cause: " + e.getCause().getMessage(), e);
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
						TimeUnit.MILLISECONDS.sleep(500);
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
	private List<Future> devicesExecutionPool = new ArrayList<>();

	/**
	 * Devices this aggregator is responsible for
	 * Data is cached and retrieved every {@link #defaultMetaDataTimeout}
	 */
	private ConcurrentHashMap<String, AggregatedDevice> aggregatedDevices = new ConcurrentHashMap<>();

	/**
	 * If the {@link SembientAggregatorCommunicator#deviceMetaDataRetrievalTimeout} is set to a value that is too small -
	 * devices list will be fetched too frequently. In order to avoid this - the minimal value is based on this value.
	 */
	private static final long defaultMetaDataTimeout = 60 * 1000 / 2;

	/**
	 * Device metadata retrieval timeout. The general devices list is retrieved once during this time period.
	 */
	private long deviceMetaDataRetrievalTimeout = 60 * 1000 / 2;

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


	void fetchRegionsList() throws Exception {
		long currentTimestamp = System.currentTimeMillis();
		if (aggregatedDevices.size() > 0 && validDeviceMetaDataRetrievalPeriodTimestamp > currentTimestamp) {
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("General devices metadata retrieval is in cooldown. %s seconds left",
						(validDeviceMetaDataRetrievalPeriodTimestamp - currentTimestamp) / 1000));
			}
			return;
		}
		validDeviceMetaDataRetrievalPeriodTimestamp = currentTimestamp + deviceMetaDataRetrievalTimeout;
		BuildingResponse[] fetchBuildings = fetchBuildings();
		int buildingFilterIndex = 0;

		if (fetchBuildings != null) {
			// Filter building:
			if (buildingFilter == null) {
				String buildingID = fetchBuildings[0].getBuildingID();
				String[] floorNames = fetchBuildings[0].getFloors();
				// Filter by floors
				filterByFloors(buildingID, floorNames);
			} else {
				for (BuildingResponse response : fetchBuildings) {
					if (response.getBuildingName().equals(buildingFilter)) {
						String buildingID = response.getBuildingID();
						String[] floorNames = response.getFloors();
						filterByFloors(buildingID, floorNames);
						break;
					}
				}
			}
		}
		nextDevicesCollectionIterationTimestamp = System.currentTimeMillis();
	}

	private void filterByFloors(String buildingID, String[] floorNames) throws Exception {
		if (StringUtils.isNotNullOrEmpty(floorFilter)) {
			String[] listFloorToBeFilter = floorFilter.split(",");
			for (String floor : listFloorToBeFilter) {
				for (String floorName : floorNames) {
					if (floor.equals(floorName)) {
						// Filter by region type
						if (StringUtils.isNotNullOrEmpty(regionTypeFilter)) {
							String[] listTypeToBeFilter = regionTypeFilter.split(",");
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
					String[] listTypeToBeFilter = regionTypeFilter.split(",");
					for (String type : listTypeToBeFilter) {
						retrieveRegions(buildingID, floorName, type);
					}
				} else {
					retrieveRegions(buildingID, floorName, null);
				}
			}
		}
	}

	void retrieveRegions(String buildingName, String floorName, String regionType) throws Exception {
		String request;
		if (regionType != null) {
			request = "/v3.1/space/regions/" + this.loginResponse.getCustomerId() + "/" + buildingName + "/" + floorName + "?regionType=" + regionType;
		} else {
			request = "/v3.1/space/regions/" + this.loginResponse.getCustomerId() + "/" + buildingName + "/" + floorName;
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
		try {
			// Get cached properties and controls
			Map<String, String> properties = aggregatedDevice.getProperties();
			List<AdvancedControllableProperty> controls = aggregatedDevice.getControllableProperties();
			// Get current date:
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
			LocalDate now = LocalDate.now(ZoneId.of("UTC"));
			// Get yesterday:
			LocalDate yesterday = LocalDate.now(ZoneId.of("UTC")).minusDays(1);
			String currentDate = formatter.format(now);
			String yesterdayDate = formatter.format(yesterday);

			String deviceId = aggregatedDevice.getDeviceId();
			String[] rawBuildingInfo = deviceId.split("-");
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
			aggregatedDevices.get(aggregatedDevice.getDeviceId()).setProperties(properties);
			aggregatedDevices.get(aggregatedDevice.getDeviceId()).setControllableProperties(controls);
		} catch (Exception e) {
			throw e;
		}
	}

	/**
	 * Populate region tag
	 *
	 * @param properties Map of cached properties of region (Aggregated device)
	 * @param controls List of cached AdvancedControllableProperty of region (Aggregated device)
	 * @param deviceId Device id of region.
	 */
	private void populateRegionTag(Map<String, String> properties, List<AdvancedControllableProperty> controls, String deviceId) throws Exception {
		controls.add(createText(properties, "RegionTag#NewTag", lastNewTag.get(deviceId)));
		controls.add(createButton(properties, "RegionTag#Create", "Create", "Creating"));
		String[] regionDetails = deviceId.split("-");
		String buildingName = regionDetails[0];
		String floorName = regionDetails[1];
		String regionName = regionDetails[2];
		String request = "/v3.1/space/tags/" + this.loginResponse.getCustomerId() + "/" + buildingName + "/" + floorName + "?regionName=" + regionName;
		RegionTagWrapperMonitor regionTagWrapperControl = this.doGetWithRetry(request, RegionTagWrapperMonitor.class);
		// Get getRegionResponse by first index because it only has 1 element.
		if (regionTagWrapperControl != null) {
			// There are some cases that getRegionResponse array is empty
			if (regionTagWrapperControl.getRegionResponse().length != 0 && regionTagWrapperControl.getRegionResponse()[0].getRegionTags().length != 0) {
				String[] regionTags = regionTagWrapperControl.getRegionResponse()[0].getRegionTags();
				List<String> values1 = new ArrayList<>(Arrays.asList(regionTags));
				String currentTag = values1.get(0);
				if (aggregatedDeviceTagMap.containsKey(deviceId)) {
					if (values1.contains(aggregatedDeviceTagMap.get(deviceId))) {
						// Check if latest list contain previous tag value
						currentTag = aggregatedDeviceTagMap.get(deviceId);
					} else {
						// Set back to default value if aggregatedDeviceTagMap isn't update to the latest one.
						aggregatedDeviceTagMap.put(deviceId, currentTag);
					}
				}
				controls.add(createDropdown(properties, "RegionTag#Tag", values1, currentTag));
				controls.add(createButton(properties, "RegionTag#Delete", "Delete", "Deleting"));
			}
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
		AirQualityWrapper airQualityWrapper = doGetWithRetry("/v3.1/iaq/timeseries/" + loginResponse.getCustomerId() + "/" + buildingName + "/"
				+ floorName + "/" + currentDate, AirQualityWrapper.class);

		if (airQualityWrapper != null) {
			AirQualitySensorResponse[] airQualitySensorResponses = new AirQualitySensorResponse[0];
			if ("200".equals(airQualityWrapper.getStatusCode()) && airQualityWrapper.getAirQualitySensorWrapper() != null) {
				airQualitySensorResponses = airQualityWrapper.getAirQualitySensorWrapper().getAirQualitySensorResponses();
			}
			if (airQualitySensorResponses.length == 0) {
				airQualityWrapper = doGetWithRetry("/v3.1/iaq/timeseries/" + loginResponse.getCustomerId() + "/" + buildingName + "/"
						+ floorName + "/" + yesterdayDate, AirQualityWrapper.class);
				if (airQualityWrapper != null) {
					if ("200".equals(airQualityWrapper.getStatusCode()) && airQualityWrapper.getAirQualitySensorWrapper() != null) {
						airQualitySensorResponses = airQualityWrapper.getAirQualitySensorWrapper().getAirQualitySensorResponses();
					}
					if (airQualitySensorResponses.length == 0) {
						populateNoData(properties, "AirQuality");
						return;
					}
				} else {
					populateNoData(properties, "AirQuality");
					return;
				}
			}
			Map<String, AirQualityData[]> sensorAndIAQMap = new HashMap<>();
			for (AirQualitySensorResponse airQualitySensorResponse : airQualitySensorResponses) {
				String[] regions = airQualitySensorResponse.getRegionName().split(",");
				if (Arrays.asList(regions).contains(regionName)) {
					sensorAndIAQMap.put(airQualitySensorResponse.getSensorName(), airQualitySensorResponse.getAirQualityData());
				}
			}
			for (Map.Entry<String, AirQualityData[]> entry : sensorAndIAQMap.entrySet()
			) {
				int lastIndex = entry.getValue().length - 1;
				properties.put("Sensor" + entry.getKey() + "-AirQuality" + "#CO2Value(C)", entry.getValue()[lastIndex].getCo2());
				properties.put("Sensor" + entry.getKey() + "-AirQuality" + "#TVOCValue(µg/m3)", entry.getValue()[lastIndex].getTvoc());
				properties.put("Sensor" + entry.getKey() + "-AirQuality" + "#PM25Value(µm)", entry.getValue()[lastIndex].getPm25());
				DateFormat obj = new SimpleDateFormat("dd MMM yyyy HH:mm:ss:SSS Z");
				obj.setTimeZone(TimeZone.getTimeZone("UTC"));
				// we create instance of the Date and pass milliseconds to the constructor
				Date res = new Date(entry.getValue()[lastIndex].getTimestamp() * 1000);
				int hour = LocalDateTime.now().getHour();
				boolean isRecentData = (hour - res.getHours()) > 1;
				properties.put("Sensor" + entry.getKey() + "-AirQuality" + "#RecentData", String.valueOf(isRecentData));
				// now we format the res by using SimpleDateFormat
				properties.put("Sensor" + entry.getKey() + "-AirQuality" + "#LastUpdate", obj.format(res));
			}
		} else {
			populateNoData(properties, "AirQuality");
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
		ThermalWrapper thermalWrapper = doGetWithRetry("/v3.1/thermal/timeseries/" + loginResponse.getCustomerId() + "/" + buildingName + "/"
				+ floorName + "/" + currentDate, ThermalWrapper.class);
		if (thermalWrapper != null) {
			ThermalSensorResponse[] thermalSensorResponse = new ThermalSensorResponse[0];
			if ("200".equals(thermalWrapper.getStatusCode()) && thermalWrapper.getThermalSensorWrappers() != null) {
				thermalSensorResponse = thermalWrapper.getThermalSensorWrappers().getThermalSensorResponses();
			}
			if (thermalSensorResponse.length == 0) {
				// Retry with yesterday data
				thermalWrapper = doGetWithRetry("/v3.1/thermal/timeseries/" + loginResponse.getCustomerId() + "/" + buildingName + "/"
						+ floorName + "/" + yesterdayDate, ThermalWrapper.class);
				if (thermalWrapper != null) {
					if ("200".equals(thermalWrapper.getStatusCode()) && thermalWrapper.getThermalSensorWrappers() != null) {
						thermalSensorResponse = thermalWrapper.getThermalSensorWrappers().getThermalSensorResponses();
					}
					if (thermalSensorResponse.length == 0) {
						populateNoData(properties, "Thermal");
						return;
					}
				} else {
					populateNoData(properties, "Thermal");
					return;
				}
			}
			Map<String, ThermalData[]> sensorAndThermalMap = new HashMap<>();
			for (ThermalSensorResponse sensorResponse : thermalSensorResponse) {
				String[] regions = sensorResponse.getRegionName().split(",");
				if (Arrays.asList(regions).contains(regionName)) {
					sensorAndThermalMap.put(sensorResponse.getSensorName(), sensorResponse.getThermalData());
				}
			}
			for (Map.Entry<String, ThermalData[]> entry : sensorAndThermalMap.entrySet()
			) {
				int lastIndex = entry.getValue().length - 1;
				properties.put("Sensor" + entry.getKey() + "-Thermal" + "#Temperature(C)", entry.getValue()[lastIndex].getTemperature());
				properties.put("Sensor" + entry.getKey() + "-Thermal" + "#Humidity(%)", entry.getValue()[lastIndex].getHumidity());
				DateFormat obj = new SimpleDateFormat("dd MMM yyyy HH:mm:ss:SSS Z");
				obj.setTimeZone(TimeZone.getTimeZone("UTC"));
				Date res = new Date(entry.getValue()[lastIndex].getTimestamp() * 1000);
				int hour = LocalDateTime.now().getHour();
				boolean isRecentData = (hour - res.getHours()) > 1;
				properties.put("Sensor" + entry.getKey() + "-Thermal" + "#RecentData", String.valueOf(isRecentData));
				properties.put("Sensor" + entry.getKey() + "-Thermal" + "#LastUpdate", obj.format(res));
			}
		} else {
			populateNoData(properties, "Thermal");
		}
	}

	private void populateNoData(Map<String, String> properties, String groupType) {
		for (Entry<String, String> entry : aggregatedDeviceSensor.entrySet()
		) {
			properties.put("Sensor" + entry.getValue() + "-" + groupType + "#Message", "No data");
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
		OccupancyWrapper occupancyWrapper = this.doGetWithRetry("/v3.1/occupancy/timeseries/" + loginResponse.getCustomerId() + "/" + buildingName + "/"
				+ floorName + "/" + currentDate, OccupancyWrapper.class);
		if (occupancyWrapper != null) {
			OccupancyRegionResponse[] occupancyRegionResponses = new OccupancyRegionResponse[0];
			if ("200".equals(occupancyWrapper.getStatusCode()) && occupancyWrapper.getOccupancyRegionWrappers() != null) {
				occupancyRegionResponses = occupancyWrapper.getOccupancyRegionWrappers().getOccupancyRegionResponses();
			}
			if (occupancyRegionResponses.length == 0) {
				// Retry one more time with yesterday data.
				occupancyWrapper = this.doGetWithRetry("/v3.1/occupancy/timeseries/" + loginResponse.getCustomerId() + "/" + buildingName + "/"
						+ floorName + "/" + yesterdayDate, OccupancyWrapper.class);
				if (occupancyWrapper != null) {
					if ("200".equals(occupancyWrapper.getStatusCode()) && occupancyWrapper.getOccupancyRegionWrappers() != null) {
						occupancyRegionResponses = occupancyWrapper.getOccupancyRegionWrappers().getOccupancyRegionResponses();
					}
					if (occupancyRegionResponses.length == 0) {
						properties.put("OccupancyList#Message", "No data");
						return;
					}
					dateToBeDisplayed = yesterdayDate;
				} else {
					properties.put("OccupancyList#Message", "No data");
					return;
				}
			}
			OccupancyData[] occupancyData = new OccupancyData[0];
			for (OccupancyRegionResponse res : occupancyRegionResponses) {
				if (regionName.equals(res.getRegionName())) {
					occupancyData = res.getOccupancyData();
					break;
				}
			}
			if (occupancyData.length == 0) {
				properties.put("OccupancyList", "Sensor haven't collected occupancy data yet.");
			}
			// Set to 8 by default if user haven't changed the hour value.
			String hourValue = "8";
			if (aggregatedDeviceHourMap.containsKey(deviceId)) {
				hourValue = aggregatedDeviceHourMap.get(deviceId);
			}
			for (OccupancyData data : occupancyData) {
				if (hourValue.equals(data.getHour())) {
					properties.put("OccupancyList#NumberOfOccupance", data.getOccupancy());
					properties.put("OccupancyList#UsageTime", data.getUsageTime());
					break;
				}
			}
			List<String> values = new ArrayList<>();
			values.add("8");
			values.add("9");
			values.add("10");
			values.add("11");
			values.add("12");
			values.add("13");
			values.add("14");
			values.add("15");
			values.add("16");
			values.add("17");
			controls.add(createDropdown(properties, "OccupancyList#Hour", values, hourValue));
			properties.put("OccupancyList#CurrentDate", dateToBeDisplayed);
		} else {
			properties.put("OccupancyList#Message", "No data");
		}
	}

	private AdvancedControllableProperty createText(Map<String, String> stats, String name, String stringValue) {
		if (stringValue == null) {
			stringValue = "";
		}
		stats.put(name, stringValue);
		AdvancedControllableProperty.Text text = new AdvancedControllableProperty.Text();
		return new AdvancedControllableProperty(name, new Date(), text, stringValue);
	}

	private AdvancedControllableProperty createButton(Map<String, String> stats, String name, String label, String labelPressed) {
		AdvancedControllableProperty.Button button = new AdvancedControllableProperty.Button();
		stats.put(name, label);
		button.setLabel(label);
		button.setLabelPressed(labelPressed);
		button.setGracePeriod(0L);
		return new AdvancedControllableProperty(name, new Date(), button, "");
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

		while (retryAttempts++ < 10 && serviceRunning) {
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

		if (retryAttempts == 10 && serviceRunning) {
			// if we got here, all 10 attempts failed
			logger.error(String.format("Failed to retrieve %s data", url), lastError);
		}
		return null;
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
	private <T> T doPutWithRetry(String url, Class<T> clazz) throws Exception {
		int retryAttempts = 0;
		Exception lastError = null;

		while (retryAttempts++ < 10 && serviceRunning) {
			try {
				return doPut(url, null, clazz);
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

		if (retryAttempts == 10 && serviceRunning) {
			// if we got here, all 10 attempts failed
			logger.error(String.format("Failed to retrieve %s data", url), lastError);
		}
		return null;
	}

	/**
	 * If addressed too frequently, Sembient API may respond with 429 code, meaning that the call rate per second was reached.
	 * Normally it would rarely happen due to the request rate limit, but when it does happen - adapter must retry the
	 * attempts of retrieving needed information. This method retries up to 10 times with 500ms timeout in between
	 *
	 * @param url to retrieve data from
	 * @throws Exception if a communication error occurs
	 */
	private void doDeleteWithRetry(String url) throws Exception {
		int retryAttempts = 0;
		Exception lastError = null;

		while (retryAttempts++ < 10 && serviceRunning) {
			try {
				this.doDelete(url);
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

		if (retryAttempts == 10 && serviceRunning) {
			// if we got here, all 10 attempts failed
			logger.error(String.format("Failed to retrieve %s data", url), lastError);
		}
	}

	private void fetchRegionMetadata(String buildingName, String floorName, RegionResponse[] regionResponses) {
		for (RegionResponse region : regionResponses) {
			boolean isContinue = false;
			// Filter by region name:
			if (StringUtils.isNotNullOrEmpty(regionNameFilter)) {
				String[] regionNames = regionNameFilter.split(",");
				for (String regionName : regionNames) {
					if (regionName.equals(region.getRegionName())) {
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
			aggregatedDevice.setDeviceId(buildingName + "-" + floorName + "-" + region.getRegionName());
			aggregatedDevice.setCategory("Region");
			aggregatedDevice.setDeviceModel(region.getRegionType());
			aggregatedDevice.setDeviceOnline(true);
			aggregatedDevice.setDeviceName(region.getRegionName());
			Map<String, String> properties = new HashMap<>();
			if (region.getSensors() != null) {
				for (int i = 0; i < region.getSensors().length; i++) {
					aggregatedDeviceSensor.put(aggregatedDevice.getDeviceId(), region.getSensors()[i]);
				}
			}
			// occupancy, thermal, iaq data will be populated later on.
			aggregatedDevice.setProperties(properties);
			aggregatedDevice.setControllableProperties(new ArrayList<>());
			aggregatedDevices.put(aggregatedDevice.getDeviceId(), aggregatedDevice);
		}
	}

	/**
	 * ReentrantLock
	 */
	private final ReentrantLock reentrantLock = new ReentrantLock();

	private LoginResponse loginResponse;

	/**
	 * Map with key is device id and value is value of hour (8-17) in the dropdown list.
	 */
	private ConcurrentHashMap<String, String> aggregatedDeviceHourMap = new ConcurrentHashMap<>();

	/**
	 * Map with key is device id and value is value of tag in the dropdown list.
	 */
	private ConcurrentHashMap<String, String> aggregatedDeviceTagMap = new ConcurrentHashMap<>();

	/**
	 * Map with key is device id and value is value of new tag.
	 */
	private ConcurrentHashMap<String, String> lastNewTag = new ConcurrentHashMap<>();

	/**
	 * Map with key is device id and value is value of sensor .
	 */
	private ConcurrentHashMap<String, String> aggregatedDeviceSensor = new ConcurrentHashMap<>();

	private String buildingFilter;

	private String floorFilter;

	private String regionNameFilter;

	private String regionTypeFilter;

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

	private void filterDevices() {

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void internalInit() throws Exception {
		this.setTrustAllCertificates(true);
		// Init thread
		executorService = Executors.newFixedThreadPool(8);
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
			String[] properties = controllableProperty.getProperty().split("#");
			String groupName = properties[0];
			String propertyName = properties[1];
			AggregatedDevice deviceToBeControlled = aggregatedDevices.get(deviceId);
			String[] deviceDetails = deviceToBeControlled.getDeviceId().split("-");
			String buildingName = deviceDetails[0];
			String floorName = deviceDetails[1];
			String regionName = deviceDetails[2];

			Map<String, String> statFromCached = deviceToBeControlled.getProperties();
			List<AdvancedControllableProperty> controlFromCached = deviceToBeControlled.getControllableProperties();
			if (groupName.equals("OccupancyList") && propertyName.equals("Hour")) {
				// Get current date:
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
				LocalDate now = LocalDate.now(ZoneId.of("UTC"));
				// Get yesterday:
				LocalDate yesterday = LocalDate.now(ZoneId.of("UTC")).minusDays(1);
				String currentDate = formatter.format(now);
				String yesterdayDate = formatter.format(yesterday);
				aggregatedDeviceHourMap.put(deviceId, (String) controllableProperty.getValue());
				populateOccupancyData(statFromCached, controlFromCached, currentDate, yesterdayDate, deviceId, buildingName, floorName, regionName);
				aggregatedDevices.get(deviceId).setProperties(statFromCached);
				aggregatedDevices.get(deviceId).setControllableProperties(controlFromCached);
			} else if (groupName.equals("RegionTag")) {
				switch (propertyName) {
					case "NewTag":
						lastNewTag.put(deviceId, (String) controllableProperty.getValue());
						statFromCached.put("RegionTag#NewTag", (String) controllableProperty.getValue());
						for (AdvancedControllableProperty control : controlFromCached) {
							if (control.getName().equals(controllableProperty.getProperty())) {
								control.setTimestamp(new Date());
								control.setValue(controllableProperty.getValue());
								break;
							}
						}
						aggregatedDevices.get(deviceId).setProperties(statFromCached);
						aggregatedDevices.get(deviceId).setControllableProperties(controlFromCached);
						break;
					case "Tag":
						aggregatedDeviceTagMap.put(deviceId, (String) controllableProperty.getValue());
						statFromCached.put("RegionTag#Tag", (String) controllableProperty.getValue());
						for (AdvancedControllableProperty control : controlFromCached) {
							if (control.getName().equals("RegionTag#Tag")) {
								control.setTimestamp(new Date());
								control.setValue(controllableProperty.getValue());
								break;
							}
						}
						aggregatedDevices.get(deviceId).setProperties(statFromCached);
						aggregatedDevices.get(deviceId).setControllableProperties(controlFromCached);
						break;
					case "Create":
						String newTag = lastNewTag.get(deviceId);
						if (StringUtils.isNullOrEmpty(newTag)) {
							throw new ResourceNotReachableException("NewTag value cannot be empty.");
						}
						String createRequest = "/v3.1/space/tags/" + loginResponse.getCustomerId() + "/" + buildingName + "/"
								+ floorName + "?regionName=" + regionName + "&regionTags=" + newTag;
						RegionTagWrapperControl createRegionTagWrapperControl = this.doPutWithRetry(createRequest, RegionTagWrapperControl.class);
						if (createRegionTagWrapperControl != null) {
							if (!"200".equals(createRegionTagWrapperControl.getStatusCode())) {
								throw new CommandFailureException("Fail to create region with value is: " + controllableProperty.getValue()
										, createRequest, createRegionTagWrapperControl.toString());
							}
							// Repopulate region tags group

							controlFromCached.removeIf(
									advancedControllableProperty -> advancedControllableProperty.getName().equals("RegionTag#NewTag"));
							controlFromCached.removeIf(
									advancedControllableProperty -> advancedControllableProperty.getName().equals("RegionTag#Create"));
							controlFromCached.removeIf(
									advancedControllableProperty -> advancedControllableProperty.getName().equals("RegionTag#Tag"));
							controlFromCached.removeIf(
									advancedControllableProperty -> advancedControllableProperty.getName().equals("RegionTag#Delete"));
							populateRegionTag(statFromCached, controlFromCached, deviceId);
							aggregatedDevices.get(deviceId).setProperties(statFromCached);
							aggregatedDevices.get(deviceId).setControllableProperties(controlFromCached);
						} else {
							throw new CommandFailureException("Fail to create region with value is: " + controllableProperty.getValue()
									, createRequest, null);
						}
						break;
					case "Delete":
						String valueToBeDelete = statFromCached.get("RegionTag#Tag");
						if (StringUtils.isNullOrEmpty(valueToBeDelete)) {
							throw new ResourceNotReachableException("Tag dropdowns value cannot be empty.");
						}
						String deleteRequest = "/v3.1/space/tags/" + loginResponse.getCustomerId() + "/" + buildingName + "/"
								+ floorName + "?regionName=" + regionName + "&regionTags=" + valueToBeDelete;
						try {
							this.doDeleteWithRetry(deleteRequest);

							controlFromCached.removeIf(
									advancedControllableProperty -> advancedControllableProperty.getName().equals("RegionTag#NewTag"));
							controlFromCached.removeIf(
									advancedControllableProperty -> advancedControllableProperty.getName().equals("RegionTag#Create"));
							controlFromCached.removeIf(
									advancedControllableProperty -> advancedControllableProperty.getName().equals("RegionTag#Tag"));
							controlFromCached.removeIf(
									advancedControllableProperty -> advancedControllableProperty.getName().equals("RegionTag#Delete"));
							populateRegionTag(statFromCached, controlFromCached, deviceId);
							aggregatedDevices.get(deviceId).setProperties(statFromCached);
							aggregatedDevices.get(deviceId).setControllableProperties(controlFromCached);
						} catch (Exception e) {
							throw new CommandFailureException("Fail to delete region with value is: " + valueToBeDelete
									, deleteRequest, null, e);
						}
						break;
					default:
						throw new IllegalStateException(String.format("Controlling group %s is not supported.", controllableProperty.getProperty()));
				}
			} else {
				// Error
			}
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
		Map<String, String> stats = new HashMap<>();
		ExtendedStatistics extendedStatistics = new ExtendedStatistics();
		try {
			// Login to get the token if token has expired.
			sembientLogin();
			BuildingResponse[] buildingResponses = fetchBuildings();
			// filter this list of building response
			if (buildingResponses != null) {
				BuildingResponse buildingResponse = null;
				if (buildingFilter != null) {
					for (BuildingResponse building : buildingResponses) {
						if (building.getBuildingName().equals(buildingFilter)) {
							buildingResponse = building;
							break;
						}
					}
				} else {
					buildingResponse = buildingResponses[0];
				}
				if (buildingResponse != null) {
					stats.put("CurrentFilterBuilding", buildingResponse.getBuildingName());
				} else {
					stats.put("CurrentFilterBuilding", "No building found");
				}
				for (int i = 0; i < buildingResponses.length; i++) {
					stats.put(String.format("Buildings#Building%02d", (i + 1)), buildingResponses[i].getBuildingName());
				}
				if (buildingResponse != null) {
					String buildingID = buildingResponse.getBuildingID();
					String[] floorNames = buildingResponse.getFloors();
					// Filter by floors
					stats.put("Building" + buildingResponse.getBuildingName() + "#BuildingId", buildingID);
					stats.put("Building" + buildingResponse.getBuildingName() + "#Address", buildingResponse.getAddress());
					if (StringUtils.isNotNullOrEmpty(floorFilter)) {
						String[] floorFilters = floorFilter.split(",");
						int i = 0;
						for (String filter : floorFilters) {
							for (String floorName : floorNames) {
								if (filter.equals(floorName)) {
									i++;
									String floorIndex = String.format("#Floor%02d", (i));
									stats.put("Building" + buildingResponse.getBuildingName() + floorIndex, floorName);
									break;
								}
							}
						}
					} else {
						int i = 0;
						for (String floorName : floorNames) {
							i++;
							String floorIndex = String.format("#Floor%02d", (i));
							stats.put("Building" + buildingResponse.getBuildingName() + floorIndex, floorName);
							break;
						}
					}
				}
			}
			extendedStatistics.setStatistics(stats);
		} finally {
			reentrantLock.unlock();
		}
		return Collections.singletonList(extendedStatistics);
	}

	private BuildingResponse[] fetchBuildings() throws Exception {
		BuildingWrapper buildingWrapper = this.doGetWithRetry("/v3.1/space/buildings/" + loginResponse.getCustomerId(), BuildingWrapper.class);
		if (buildingWrapper != null) {
			return buildingWrapper.getBuildingResponse();
		}
		return null;
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
				executorService = Executors.newFixedThreadPool(8);
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

	/***
	 * Create AdvancedControllableProperty preset instance
	 *
	 * @param stats extended statistics
	 * @param name name of the control
	 * @param values list of values
	 * @param initialValue initial value of the control
	 * @return AdvancedControllableProperty preset instance
	 */
	private AdvancedControllableProperty createDropdown(Map<String, String> stats, String name, List<String> values, String initialValue) {
		stats.put(name, initialValue);
		AdvancedControllableProperty.DropDown dropDown = new AdvancedControllableProperty.DropDown();
		dropDown.setOptions(values.toArray(new String[0]));
		dropDown.setLabels(values.toArray(new String[0]));

		return new AdvancedControllableProperty(name, new Date(), dropDown, initialValue);
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
			headers.add("x-api-key", loginResponse.getApiKey());
			headers.add("Authorization", "Bearer " + loginResponse.getBearerToken());
		}
		return super.putExtraRequestHeaders(httpMethod, uri, headers);
	}

	/**
	 * Sembient login method.
	 * This method is used for getting the API tokens from the Sembient API.
	 * Expect return:
	 * <li>exp: Expiration time(seconds)</li>
	 * <li>idToken: Bearer token</li>
	 * <li>cid: customer id</li>
	 * <li>apiKey: API key that we will attach to every header of the request.</li>
	 *
	 * {@inheritDoc}
	 */
	@Override
	protected void authenticate() {
		//
	}

	private void sembientLogin() throws FailedLoginException {
		long currentTime = System.currentTimeMillis();
		if (loginResponse == null || currentTime > loginResponse.getExpirationTime()) {
			Map<String, String> headers = new HashMap<>();
			headers.put("Accept", "application/json");
			headers.put("Content-Type", "application/json");
			String valueToEncode = this.getLogin() + ":" + this.getPassword();
			String encodeBasicScheme = "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
			headers.put("Authorization", encodeBasicScheme);
			try {
				String loginRawResponse = this.doPost("/v3.1/users/login", headers, "");
				LoginWrapper loginWrapper = new ObjectMapper().readValue(loginRawResponse, LoginWrapper.class);
				loginResponse = loginWrapper.getLoginResponse();
				loginResponse.setExpirationTime(currentTime + loginResponse.getExp() * 1000L);
			} catch (Exception e) {
				logger.error(String.format("An exception occur when trying to log in with username: %s, password: %s, error message: %s", this.getLogin(), this.getPassword(), e.getMessage()), e);
				throw new FailedLoginException(String.format("Fail to login with username: %s, password: %s", this.getLogin(), this.getPassword()));
			}
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

				String[] hostSplit = this.getHost().split("-");
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
}
