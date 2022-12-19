/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.sembient.sembient;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.security.auth.login.FailedLoginException;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.aggregator.AggregatedDevice;
import com.avispl.symphony.dal.infrastructure.management.sembient.sembient.utils.SembientAggregatorConstant;

@Tag("RealDevice")
class SembientAggregatorCommunicatorTest {
	private SembientAggregatorCommunicator communicator = new SembientAggregatorCommunicator();

	@BeforeEach
	void setUp() throws Exception {
		communicator.setHost("api.sembient.com");
		communicator.setProtocol("https");
		communicator.setContentType("application/json");
		communicator.setLogin("Anh.Quach@avispl.com");
		communicator.setPassword("useforTMA");
		communicator.init();
	}

	@AfterEach
	void tearDown() {
		communicator.destroy();
	}

	/**
	 * Test getMultipleStatistics for aggregator with the first call get Building information
	 * <p>
	 * Expect get Building information data successfully
	 */
	@Test
	void testGetMultipleStatisticsWithBuildingInformation() throws Exception {
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) communicator.getMultipleStatistics().get(0);
		Map<String, String> stats = extendedStatistics.getStatistics();
		Assert.assertEquals("TEST", stats.get("BuildingTest#BuildingId"));
		Assert.assertEquals("1st floor", stats.get("BuildingTest#Floor01"));
		Assert.assertEquals("Sandbox", stats.get("BuildingTest#Address"));
		Assert.assertEquals("Test", stats.get("CurrentFilterBuilding"));
		Assert.assertEquals("Test", stats.get("Buildings#Building01"));
	}

	/**
	 * Test retrieveMultipleStatistics with aggregated device information
	 * <p>
	 * Expect retrieveMultipleStatistics with aggregated device information successfully
	 */
	@Test
	void testRetrieveMultipleStatistics() throws Exception {
		communicator.getMultipleStatistics();
		communicator.retrieveMultipleStatistics();
		Thread.sleep(30000);
		communicator.getMultipleStatistics();
		List<AggregatedDevice> devices = communicator.retrieveMultipleStatistics();
		Assert.assertEquals(7, devices.size());
	}

	/**
	 * Test retrieveMultipleStatistics with aggregated device Sensor is Thermal information
	 * <p>
	 * Expect retrieveMultipleStatistics with aggregated device Sensor is Thermal information successfully
	 */
	@Test
	void testRetrieveMultipleStatisticsWithSensorWhereThermalInformation() throws Exception {
		communicator.getMultipleStatistics();
		communicator.retrieveMultipleStatistics();
		Thread.sleep(30000);
		List<AggregatedDevice> devices = communicator.retrieveMultipleStatistics();
		Map<String, String> properties = devices.get(0).getProperties();
		String key = "Thermal#";
		Assert.assertNotNull(properties);
		Assert.assertNotNull(properties.get(key + "TemperatureLatest(F)"));
		Assert.assertNotNull(properties.get(key + "TemperatureMax(F)"));
		Assert.assertNotNull(properties.get(key + "TemperatureMin(F)"));
		Assert.assertNotNull(properties.get(key + "TemperatureAvg(F)"));
		Assert.assertNotNull(properties.get(key + "HumidityLatest(%)"));
		Assert.assertNotNull(properties.get(key + "HumidityMax(%)"));
		Assert.assertNotNull(properties.get(key + "HumidityMin(%)"));
		Assert.assertNotNull(properties.get(key + "HumidityAvg(%)"));
		Assert.assertNotNull(properties.get(key + "FromTime"));
		Assert.assertNotNull(properties.get(key + "ToTime"));
		Assert.assertNotNull(properties.get(key + "RecentData"));
	}

	/**
	 * Test retrieveMultipleStatistics with aggregated device Sensor is Air quality information
	 * <p>
	 * Expect retrieveMultipleStatistics with aggregated device Sensor is Air quality information successfully
	 */
	@Test
	void testRetrieveMultipleStatisticsWithSensorWhereAirQualityInformation() throws Exception {
		communicator.getMultipleStatistics();
		communicator.retrieveMultipleStatistics();
		Thread.sleep(30000);
		List<AggregatedDevice> devices = communicator.retrieveMultipleStatistics();
		Map<String, String> properties = devices.get(0).getProperties();
		String key = "AirQuality#";
		Assert.assertNotNull(properties.get(key + "CO2Latest(ppm)"));
		Assert.assertNotNull(properties.get(key + "PM2.5Latest(microgram/m3)"));
		Assert.assertNotNull(properties.get(key + "TVOCLatest(microgram/m3)"));
		Assert.assertNotNull(properties.get(key + "FromTime"));
		Assert.assertNotNull(properties.get(key + "ToTime"));
		Assert.assertNotNull(properties.get(key + "RecentData"));
	}

	/**
	 * Test retrieveMultipleStatistics with aggregated device Sensor is Air quality no data
	 * <p>
	 * Expect retrieveMultipleStatistics with aggregated device Sensor is Air quality no data
	 */
	@Ignore
	@Test
	void testRetrieveMultipleStatisticsWithSensorWhereAirQualityNoData() throws Exception {
		communicator.getMultipleStatistics();
		communicator.retrieveMultipleStatistics();
		Thread.sleep(30000);
		List<AggregatedDevice> devices = communicator.retrieveMultipleStatistics();
		Map<String, String> properties = devices.get(0).getProperties();
		Assert.assertEquals("No data", properties.get("AirQuality#Message"));
	}

	/**
	 * Test retrieveMultipleStatistics with aggregated device Sensor is Thermal no data
	 * <p>
	 * Expect retrieveMultipleStatistics with aggregated device Sensor is Thermal no data
	 */
	@Test
	@Ignore
	void testRetrieveMultipleStatisticsWithSensorWhereThermalNoData() throws Exception {
		communicator.getMultipleStatistics();
		communicator.retrieveMultipleStatistics();
		Thread.sleep(30000);
		List<AggregatedDevice> devices = communicator.retrieveMultipleStatistics();
		Map<String, String> properties = devices.get(0).getProperties();
		Assert.assertEquals("No data", properties.get("Thermal#Message"));
	}

	/**
	 * Test retrieveMultipleStatistics with aggregated device Occupancy information group
	 * <p>
	 * Expect retrieveMultipleStatistics with aggregated device Occupancy information group successfully
	 */
	@Test
	void testRetrieveMultipleStatisticsWithOccupancyListInformation() throws Exception {
		communicator.getMultipleStatistics();
		communicator.retrieveMultipleStatistics();
		Thread.sleep(30000);
		List<AggregatedDevice> devices = communicator.retrieveMultipleStatistics();
		int index = 0;
		for (int i = 0; i < devices.size(); i++) {
			if (devices.get(i).getDeviceId().equals("Region-SANDBOX-KRFOBAZP-TEST-1st floor-Region_2")) {
				index = i;
			}
		}
		Map<String, String> properties = devices.get(index).getProperties();
		Assert.assertNotNull(properties.get("OccupancyList#CurrentDate"));
		Assert.assertEquals("8", properties.get("OccupancyList#Hour"));
		Assert.assertNotNull(properties.get("OccupancyList#NumberOfOccupants"));
		Assert.assertNotNull(properties.get("OccupancyList#UsageTime(minute)"));
	}

	/**
	 * Test control aggregated device with new Region tag
	 * <p>
	 * Expect control aggregated device with new Region tag successfully
	 */
	@Test
	void testControlAggregatedDeviceWithRegionTag() throws Exception {
		communicator.getMultipleStatistics();
		communicator.retrieveMultipleStatistics();
		Thread.sleep(30000);
		List<AggregatedDevice> devices = communicator.retrieveMultipleStatistics();
		int index = 0;
		for (int i = 0; i < devices.size(); i++) {
			if (devices.get(i).getDeviceId().equals("Region-SANDBOX-KRFOBAZP-TEST-1st floor-Region_2")) {
				index = i;
			}
		}
		Map<String, String> properties = devices.get(index).getProperties();
		Assert.assertEquals("", properties.get("RegionTags#NewTag"));
		ControllableProperty controllableProperty = new ControllableProperty();
		controllableProperty.setProperty("RegionTags#NewTag");
		controllableProperty.setValue("res3");
		controllableProperty.setDeviceId("Region-SANDBOX-KRFOBAZP-TEST-1st floor-Region_2");
		communicator.controlProperty(controllableProperty);
		communicator.getMultipleStatistics();
		devices = communicator.retrieveMultipleStatistics();
		properties = devices.get(index).getProperties();
		Assert.assertEquals("res3", properties.get("RegionTags#NewTag"));
	}

	/**
	 * Test control aggregated device with new Region tag
	 * <p>
	 * Expect control aggregated device with new Region tag successfully
	 */
	@Test
	void testControlAggregatedDeviceWithCreateRegionTag() throws Exception {
		communicator.getMultipleStatistics();
		communicator.retrieveMultipleStatistics();
		Thread.sleep(30000);
		List<AggregatedDevice> devices = communicator.retrieveMultipleStatistics();
		int index = 0;
		for (int i = 0; i < devices.size(); i++) {
			if (devices.get(i).getDeviceId().equals("Region-SANDBOX-KRFOBAZP-TEST-1st floor-Region_2")) {
				index = i;
			}
		}
		Map<String, String> properties = devices.get(index).getProperties();
		Assert.assertEquals("", properties.get("RegionTags#NewTag"));
		ControllableProperty controllableProperty = new ControllableProperty();
		controllableProperty.setProperty("RegionTags#NewTag");
		controllableProperty.setValue("res3");
		controllableProperty.setDeviceId("Region-SANDBOX-KRFOBAZP-TEST-1st floor-Region_2");
		communicator.controlProperty(controllableProperty);
		communicator.getMultipleStatistics();
		devices = communicator.retrieveMultipleStatistics();
		properties = devices.get(index).getProperties();
		Assert.assertEquals("res3", properties.get("RegionTags#NewTag"));
		controllableProperty.setProperty("RegionTags#CreateNewTag");
		controllableProperty.setValue("1");
		controllableProperty.setDeviceId("Region-SANDBOX-KRFOBAZP-TEST-1st floor-Region_2");
		communicator.controlProperty(controllableProperty);
		communicator.retrieveMultipleStatistics();
		// If reach here but not error occur then the control operation is success
	}


	/**
	 * Test control aggregated device delete Region tag
	 * <p>
	 * Expect control aggregated device delete Region tag successfully
	 */
	@Test
	void testControlAggregatedDeviceWithDeleteRegionTag() throws Exception {
		communicator.getMultipleStatistics();
		communicator.retrieveMultipleStatistics();
		Thread.sleep(30000);
		List<AggregatedDevice> devices = communicator.retrieveMultipleStatistics();
		int index = 0;
		for (int i = 0; i < devices.size(); i++) {
			if (devices.get(i).getDeviceId().equals("Region-SANDBOX-KRFOBAZP-TEST-1st floor-Region_2")) {
				index = i;
			}
		}
		Map<String, String> properties = devices.get(index).getProperties();
		Assert.assertEquals("", properties.get("RegionTags#NewTag"));
		ControllableProperty controllableProperty = new ControllableProperty();
		controllableProperty.setProperty("RegionTags#DeleteSelectedTag");
		controllableProperty.setValue("1");
		controllableProperty.setDeviceId("Region-SANDBOX-KRFOBAZP-TEST-1st floor-Region_2");
		communicator.controlProperty(controllableProperty);
		communicator.getMultipleStatistics();
		communicator.retrieveMultipleStatistics();
		Thread.sleep(30000);
		devices = communicator.retrieveMultipleStatistics();
		properties = devices.get(index).getProperties();
		Assert.assertEquals("", properties.get("RegionTags#NewTag"));
		Assert.assertEquals(null, properties.get("RegionTags#Tag"));
	}

	/**
	 * Test control aggregated device delete Region tag
	 * <p>
	 * Expect control aggregated device delete Region tag successfully
	 */
	@Test
	void testControlAggregatedDeviceWithChangeRegionTag() throws Exception {
		testControlAggregatedDeviceWithCreateRegionTag();
		communicator.getMultipleStatistics();
		communicator.retrieveMultipleStatistics();
		Thread.sleep(30000);
		List<AggregatedDevice> devices = communicator.retrieveMultipleStatistics();
		int index = 0;
		for (int i = 0; i < devices.size(); i++) {
			if (devices.get(i).getDeviceId().equals("Region-SANDBOX-KRFOBAZP-TEST-1st floor-Region_2")) {
				index = i;
			}
		}
		Map<String, String> properties = devices.get(index).getProperties();
		Assert.assertEquals("", properties.get("RegionTags#NewTag"));
		ControllableProperty controllableProperty = new ControllableProperty();
		controllableProperty.setProperty("RegionTags#Tag");
		controllableProperty.setValue("res2");
		controllableProperty.setDeviceId("Region-SANDBOX-KRFOBAZP-TEST-1st floor-Region_2");
		communicator.controlProperty(controllableProperty);
		communicator.getMultipleStatistics();
		devices = communicator.retrieveMultipleStatistics();
		properties = devices.get(index).getProperties();
		Assert.assertEquals("", properties.get("RegionTags#NewTag"));
		Assert.assertEquals("res2", properties.get("RegionTags#Tag"));
	}

	/**
	 * Test control aggregated device with OccupancyList change hour
	 * <p>
	 * Expect control aggregated device with OccupancyList change hour successfully
	 */
	@Test
	void testControlAggregatedDeviceWithOccupancyList() throws Exception {
		testControlAggregatedDeviceWithCreateRegionTag();
		communicator.getMultipleStatistics();
		communicator.retrieveMultipleStatistics();
		Thread.sleep(30000);
		List<AggregatedDevice> devices = communicator.retrieveMultipleStatistics();
		int index = 0;
		for (int i = 0; i < devices.size(); i++) {
			if (devices.get(i).getDeviceId().equals("Region-SANDBOX-KRFOBAZP-TEST-1st floor-Region_2")) {
				index = i;
			}
		}
		Map<String, String> properties = devices.get(index).getProperties();
		Assert.assertNotNull(properties.get("OccupancyList#CurrentDate"));
		Assert.assertEquals("8", properties.get("OccupancyList#Hour"));
		Assert.assertNotNull(properties.get("OccupancyList#NumberOfOccupants"));
		Assert.assertNotNull(properties.get("OccupancyList#UsageTime(minute)"));
		ControllableProperty controllableProperty = new ControllableProperty();
		controllableProperty.setProperty("OccupancyList#Hour");
		controllableProperty.setValue("10");
		controllableProperty.setDeviceId("Region-SANDBOX-KRFOBAZP-TEST-1st floor-Region_2");
		communicator.controlProperty(controllableProperty);
		communicator.getMultipleStatistics();
		communicator.retrieveMultipleStatistics();
		Thread.sleep(30000);
		devices = communicator.retrieveMultipleStatistics();
		properties = devices.get(index).getProperties();
		Assert.assertNotNull(properties.get("OccupancyList#CurrentDate"));
		Assert.assertEquals("10", properties.get("OccupancyList#Hour"));
		Assert.assertNotNull(properties.get("OccupancyList#NumberOfOccupants"));
		Assert.assertNotNull(properties.get("OccupancyList#UsageTime(minute)"));
	}

	/**
	 * Test filter by building name is Test
	 * <p>
	 * Expect filter by building name is Test successfully
	 */
	@Test
	void testBuildingFilterByName() throws Exception {
		communicator.setBuildingFilter("Test");
		communicator.getMultipleStatistics();
		communicator.retrieveMultipleStatistics();
		Thread.sleep(30000);
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) communicator.getMultipleStatistics().get(0);
		Map<String, String> stats = extendedStatistics.getStatistics();
		List<AggregatedDevice> devices = communicator.retrieveMultipleStatistics();
		Assert.assertEquals(7, devices.size());
		Assert.assertEquals("TEST", stats.get("BuildingTest#BuildingId"));
		Assert.assertEquals("1st floor", stats.get("BuildingTest#Floor01"));
		Assert.assertEquals("Sandbox", stats.get("BuildingTest#Address"));
		Assert.assertEquals("Test", stats.get("CurrentFilterBuilding"));
		Assert.assertEquals("Test", stats.get("Buildings#Building01"));
	}

	/**
	 * Test filter by building name not exits
	 * <p>
	 * Expect filter by building name with no data
	 */
	@Test
	void testBuildingFilterByNameNoTExits() throws Exception {
		communicator.setBuildingFilter("Test 02");
		communicator.getMultipleStatistics();
		communicator.retrieveMultipleStatistics();
		Thread.sleep(30000);
		List<AggregatedDevice> devices = communicator.retrieveMultipleStatistics();
		Assert.assertEquals(0, devices.size());
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) communicator.getMultipleStatistics().get(0);
		Map<String, String> stats = extendedStatistics.getStatistics();
		Assert.assertEquals("No building found", stats.get("CurrentFilterBuilding"));
	}

	/**
	 * Test filter by floor name is 1st floor
	 * <p>
	 * Expect filter by floor name is 1st floor successfully
	 */
	@Test
	void testFloorFilterByName() throws Exception {
		communicator.setFloorFilter("1st floor");
		communicator.getMultipleStatistics();
		communicator.retrieveMultipleStatistics();
		Thread.sleep(30000);
		List<AggregatedDevice> devices = communicator.retrieveMultipleStatistics();
		Assert.assertEquals(7, devices.size());
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) communicator.getMultipleStatistics().get(0);
		Map<String, String> stats = extendedStatistics.getStatistics();
		Assert.assertEquals("TEST", stats.get("BuildingTest#BuildingId"));
		Assert.assertEquals("1st floor", stats.get("BuildingTest#Floor01"));
		Assert.assertEquals("Sandbox", stats.get("BuildingTest#Address"));
		Assert.assertEquals("Test", stats.get("CurrentFilterBuilding"));
		Assert.assertEquals("Test", stats.get("Buildings#Building01"));
	}

	/**
	 * Test filter by floor name not exits
	 * <p>
	 * Expect filter by floor name with building floor is null
	 */
	@Test
	void testBuildingFilterAndFloorFilterByNameNoTExits() throws Exception {
		communicator.setFloorFilter("1st floor 01");
		communicator.getMultipleStatistics();
		communicator.retrieveMultipleStatistics();
		Thread.sleep(30000);
		List<AggregatedDevice> devices = communicator.retrieveMultipleStatistics();
		Assert.assertEquals(0, devices.size());
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) communicator.getMultipleStatistics().get(0);
		Map<String, String> stats = extendedStatistics.getStatistics();
		Assert.assertEquals(null, stats.get("BuildingTest#Floor01"));
		Assert.assertEquals("TEST", stats.get("BuildingTest#BuildingId"));
		Assert.assertEquals("Sandbox", stats.get("BuildingTest#Address"));
		Assert.assertEquals("Test", stats.get("CurrentFilterBuilding"));
		Assert.assertEquals("Test", stats.get("Buildings#Building01"));
	}

	/**
	 * Test filter by building name is Test and floor name is 1st floor
	 * <p>
	 * Expect filter by building name is Test and floor name is 1st floor successfully
	 */
	@Test
	void testFloorFilterAndFloorFilter() throws Exception {
		communicator.setBuildingFilter("Test");
		communicator.setFloorFilter("1st floor");
		communicator.getMultipleStatistics();
		communicator.retrieveMultipleStatistics();
		Thread.sleep(30000);
		List<AggregatedDevice> devices = communicator.retrieveMultipleStatistics();
		Assert.assertEquals(7, devices.size());
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) communicator.getMultipleStatistics().get(0);
		Map<String, String> stats = extendedStatistics.getStatistics();
		Assert.assertEquals("1st floor", stats.get("BuildingTest#Floor01"));
		Assert.assertEquals("TEST", stats.get("BuildingTest#BuildingId"));
		Assert.assertEquals("Sandbox", stats.get("BuildingTest#Address"));
		Assert.assertEquals("Test", stats.get("CurrentFilterBuilding"));
		Assert.assertEquals("Test", stats.get("Buildings#Building01"));
	}

	/**
	 * Test filter by Region type is Workstations
	 * <p>
	 * Expect filter by Region type is Workstations successfully
	 */
	@Test
	void testRegionFilterByType() throws Exception {
		communicator.setDeviceTypeFilter("Region");
		communicator.setRegionTypeFilter("Workstations");
		communicator.getMultipleStatistics();
		communicator.retrieveMultipleStatistics();
		Thread.sleep(30000);
		List<AggregatedDevice> devices = communicator.retrieveMultipleStatistics();
		Assert.assertEquals(2, devices.size());
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) communicator.getMultipleStatistics().get(0);
		Map<String, String> stats = extendedStatistics.getStatistics();
		Assert.assertEquals("1st floor", stats.get("BuildingTest#Floor01"));
		Assert.assertEquals("TEST", stats.get("BuildingTest#BuildingId"));
		Assert.assertEquals("Sandbox", stats.get("BuildingTest#Address"));
		Assert.assertEquals("Test", stats.get("CurrentFilterBuilding"));
		Assert.assertEquals("Test", stats.get("Buildings#Building01"));
	}

	/**
	 * Test filter by Region names are Region_2, Region_1
	 * <p>
	 * Expect filter by Region names are Region_2, Region_1 successfully
	 */
	@Test
	void testRegionFilterByName() throws Exception {
		communicator.setDeviceNameFilter("Region_2,Region_2,Region_1");
		communicator.getMultipleStatistics();
		communicator.retrieveMultipleStatistics();
		Thread.sleep(30000);
		List<AggregatedDevice> devices = communicator.retrieveMultipleStatistics();
		Assert.assertEquals(2, devices.size());
	}

	/**
	 * Test filter by Region names are Region_2, Region_1
	 * <p>
	 * Expect filter by Region names are Region_2, Region_1 successfully
	 */
	@Test
	void testRegionFilterByNameCase2() throws Exception {
		// Set name with space
		communicator.setDeviceNameFilter("Region_2 ,Region_2 , Region_1");
		communicator.getMultipleStatistics();
		communicator.retrieveMultipleStatistics();
		Thread.sleep(30000);
		List<AggregatedDevice> devices = communicator.retrieveMultipleStatistics();
		Assert.assertEquals(2, devices.size());
	}

	/**
	 * Test filter by Region names are region_2, region_1
	 * <p>
	 * Expect filter by Region names are Region_2, Region_1 successfully
	 */
	@Test
	void testRegionFilterByNameCase3() throws Exception {
		// Set name with spaces and lower case
		communicator.setDeviceNameFilter("region_2 , region_1");
		communicator.getMultipleStatistics();
		communicator.retrieveMultipleStatistics();
		Thread.sleep(30000);
		List<AggregatedDevice> devices = communicator.retrieveMultipleStatistics();
		// Expect 0 devices here because filter is case-sensitive
		Assert.assertEquals(0, devices.size());
	}

	/**
	 * Test filter by Region type is Workstations 01
	 * <p>
	 * Expect filter by Region type is Workstations 01 with no aggregated device
	 */
	@Test
	void testRegionFilterByTypeNotExits() throws Exception {
		communicator.setDeviceTypeFilter("Workstations 01");
		communicator.getMultipleStatistics();
		communicator.retrieveMultipleStatistics();
		Thread.sleep(30000);
		List<AggregatedDevice> devices = communicator.retrieveMultipleStatistics();
		Assert.assertEquals(0, devices.size());
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) communicator.getMultipleStatistics().get(0);
		Map<String, String> stats = extendedStatistics.getStatistics();
		Assert.assertEquals("1st floor", stats.get("BuildingTest#Floor01"));
		Assert.assertEquals("TEST", stats.get("BuildingTest#BuildingId"));
		Assert.assertEquals("Sandbox", stats.get("BuildingTest#Address"));
		Assert.assertEquals("Test", stats.get("CurrentFilterBuilding"));
		Assert.assertEquals("Test", stats.get("Buildings#Building01"));
	}

	/**
	 * Test filter floor is 1st floor and Region type is Workstations
	 * <p>
	 * Expect filter floor is 1st floor and Region type is Workstations successfully
	 */
	@Test
	void testFloorFilterAndRegionFilter() throws Exception {
		communicator.setFloorFilter("1st floor");
		communicator.setDeviceTypeFilter("Region");
		communicator.setRegionTypeFilter("Workstations");
		communicator.getMultipleStatistics();
		communicator.retrieveMultipleStatistics();
		Thread.sleep(30000);
		List<AggregatedDevice> devices = communicator.retrieveMultipleStatistics();
		Assert.assertEquals(2, devices.size());
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) communicator.getMultipleStatistics().get(0);
		Map<String, String> stats = extendedStatistics.getStatistics();
		Assert.assertEquals("1st floor", stats.get("BuildingTest#Floor01"));
		Assert.assertEquals("TEST", stats.get("BuildingTest#BuildingId"));
		Assert.assertEquals("Sandbox", stats.get("BuildingTest#Address"));
		Assert.assertEquals("Test", stats.get("CurrentFilterBuilding"));
		Assert.assertEquals("Test", stats.get("Buildings#Building01"));
	}

	/**
	 * Test filter floor is not exits and Region type is Workstations
	 * <p>
	 * Expect filter by Floor not exits with no data for aggregated device and Building Floor is null
	 */
	@Test
	void testFloorFilterNotExitsAndRegionFilterByNameWorkstations() throws Exception {
		communicator.setFloorFilter("1st floor 01");
		communicator.setDeviceTypeFilter("Workstations");
		communicator.getMultipleStatistics();
		communicator.retrieveMultipleStatistics();
		Thread.sleep(30000);
		List<AggregatedDevice> devices = communicator.retrieveMultipleStatistics();
		Assert.assertEquals(0, devices.size());
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) communicator.getMultipleStatistics().get(0);
		Map<String, String> stats = extendedStatistics.getStatistics();
		Assert.assertEquals(null, stats.get("BuildingTest#Floor01"));
		Assert.assertEquals("TEST", stats.get("BuildingTest#BuildingId"));
		Assert.assertEquals("Sandbox", stats.get("BuildingTest#Address"));
		Assert.assertEquals("Test", stats.get("CurrentFilterBuilding"));
		Assert.assertEquals("Test", stats.get("Buildings#Building01"));
	}

	/**
	 * Test refresh interval
	 *
	 * @throws Exception if fail to getMultipleStatistics, retrieveMultipleStatistics
	 */
	@Test
	void testPollingInterval() throws Exception {
		communicator.setInstallationLayoutPollingCycle("10");
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) communicator.getMultipleStatistics().get(0);
		Assert.assertNotNull(extendedStatistics.getStatistics().get("NextInstallationLayoutPollingCycle"));
	}

	// Negative cases

	/**
	 * Test negative case when creating tag
	 *
	 * @throws Exception When fail new tag is null/empty
	 */
	@Test()
	void testCaseTagIsNull() throws Exception {
		communicator.getMultipleStatistics();
		communicator.retrieveMultipleStatistics();
		communicator.retrieveMultipleStatistics();
		Thread.sleep(30000);
		ControllableProperty controllableProperty = new ControllableProperty();
		controllableProperty.setProperty("RegionTags#CreateNewTag");
		controllableProperty.setValue(1);
		controllableProperty.setDeviceId("Region-SANDBOX-KRFOBAZP-TEST-1st floor-Region_2");

		Exception exception = Assert.assertThrows(IllegalArgumentException.class, () -> {
			communicator.controlProperty(controllableProperty);
		});

		String expectedMessage = "Cannot create new region tag with NewTag's value is empty or null or only space characters.";
		String actualMessage = exception.getMessage();

		Assert.assertTrue(actualMessage.contains(expectedMessage));
	}

	/**
	 * Test negative case when fail to log in
	 *
	 * @throws Exception when fail to log in
	 */
	@Test()
	void testCaseLoginFail() throws Exception {
		communicator.destroy();
		communicator.setLogin("not-exist-account");
		communicator.init();
		Exception exception = Assert.assertThrows(FailedLoginException.class, () -> {
			communicator.getMultipleStatistics();
			;
		});
		String expectedMessage = "Failed to login with username: not-exist-account, password: useforTMA";
		String actualMessage = exception.getMessage();

		Assert.assertTrue(actualMessage.contains(expectedMessage));
	}

	/**
	 * Test refresh interval
	 *
	 * @throws Exception if fail to getMultipleStatistics, retrieveMultipleStatistics
	 */
	@Test
	void testAutoAssignPollingInterval() throws Exception {
		long currentDateTime = System.currentTimeMillis();
		communicator.setPollingCycle("0");
		communicator.setRetryInterval("1");
		communicator.getMultipleStatistics();
		Thread.sleep(5000);
		communicator.retrieveMultipleStatistics();
		Thread.sleep(60000);
		communicator.getMultipleStatistics();
		communicator.retrieveMultipleStatistics();
		Thread.sleep(600000);
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) communicator.getMultipleStatistics().get(0);
		communicator.retrieveMultipleStatistics();
		SimpleDateFormat f = new SimpleDateFormat(SembientAggregatorConstant.DATE_ISO_FORMAT);
		f.setTimeZone(TimeZone.getTimeZone(SembientAggregatorConstant.UTC_TIMEZONE));
		String date = extendedStatistics.getStatistics().get("NextPollingCycle");
		Date d = f.parse(date);
		long nextPollingCycle = d.getTime();
		Assert.assertTrue((currentDateTime + 30 * 60000) < nextPollingCycle);
	}
}