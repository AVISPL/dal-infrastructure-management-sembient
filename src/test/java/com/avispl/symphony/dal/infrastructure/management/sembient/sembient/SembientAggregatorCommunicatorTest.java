/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.sembient.sembient;

import java.util.List;
import java.util.Map;

import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.aggregator.AggregatedDevice;

@Tag("RealDevice")
class SembientAggregatorCommunicatorTest {

	private SembientAggregatorCommunicator communicator;

	@BeforeEach
	void setUp() throws Exception {
		communicator = new SembientAggregatorCommunicator();
		communicator.setHost("api.sembient.com");
		communicator.setProtocol("https");
		communicator.setContentType("application/json");
		communicator.setLogin("***REMOVED***");
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
		communicator.getMultipleStatistics();
		communicator.retrieveMultipleStatistics();
		Thread.sleep(30000);
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) communicator.getMultipleStatistics().get(0);
		Map<String, String> stats = extendedStatistics.getStatistics();
		System.out.println(stats);
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
		communicator.retrieveMultipleStatistics();
		Thread.sleep(30000);
		List<AggregatedDevice> devices = communicator.retrieveMultipleStatistics();
		Assert.assertEquals(2, devices.size());

		Assert.assertEquals("Region", devices.get(0).getCategory());
		Assert.assertEquals("TEST-1st floor-Region_2", devices.get(0).getDeviceId());
		Assert.assertEquals("Workstations", devices.get(0).getDeviceModel());
		Assert.assertEquals("Region_2", devices.get(0).getDeviceName());
		Assert.assertEquals(true, devices.get(0).getDeviceOnline());

		Assert.assertEquals("Region", devices.get(1).getCategory());
		Assert.assertEquals("TEST-1st floor-Region_1", devices.get(1).getDeviceId());
		Assert.assertEquals("Workstations", devices.get(1).getDeviceModel());
		Assert.assertEquals("Region_1", devices.get(1).getDeviceName());
		Assert.assertEquals(true, devices.get(1).getDeviceOnline());
	}

	/**
	 * Test retrieveMultipleStatistics with aggregated device Sensor is Thermal information
	 * <p>
	 * Expect retrieveMultipleStatistics with aggregated device Sensor is Thermal information successfully
	 */
	@Test
	void testRetrieveMultipleStatisticsWithSensorWhereThermalInformation() throws Exception {
		communicator.retrieveMultipleStatistics();
		Thread.sleep(30000);
		List<AggregatedDevice> devices = communicator.retrieveMultipleStatistics();
		Map<String, String> properties = devices.get(0).getProperties();
		System.out.println(properties);
		String key = "Sensor3005-Thermal#";
		Assert.assertNotNull( properties.get(key + "Humidity(%)"));
		Assert.assertNotNull( properties.get(key + "Temperature(F)"));
		Assert.assertNotNull( properties.get(key + properties.get(key + "LastUpdate")));
		Assert.assertNotNull( properties.get(key + properties.get(key + "RecentData")));
	}

	/**
	 * Test retrieveMultipleStatistics with aggregated device Sensor is Air quality information
	 * <p>
	 * Expect retrieveMultipleStatistics with aggregated device Sensor is Air quality information successfully
	 */
	@Test
	void testRetrieveMultipleStatisticsWithSensorWhereAirQualityInformation() throws Exception {
		communicator.retrieveMultipleStatistics();
		Thread.sleep(30000);
		List<AggregatedDevice> devices = communicator.retrieveMultipleStatistics();
		Map<String, String> properties = devices.get(0).getProperties();
		String key = "Sensor3005-AirQuality#";
		Assert.assertNotNull(properties.get(key + "CO2Value(C)"));
		Assert.assertNotNull(properties.get(key + "PM25Value(micromet)"));
		Assert.assertNotNull(properties.get(key + "LastUpdate"));
		Assert.assertNotNull(properties.get(key + "RecentData"));
	}


	/**
	 * Test retrieveMultipleStatistics with aggregated device Sensor is Air quality no data
	 * <p>
	 * Expect retrieveMultipleStatistics with aggregated device Sensor is Air quality no data
	 */
	@Test
	void testRetrieveMultipleStatisticsWithSensorWhereAirQualityNoData() throws Exception {
		communicator.getMultipleStatistics();
		communicator.retrieveMultipleStatistics();
		Thread.sleep(30000);
		List<AggregatedDevice> devices = communicator.retrieveMultipleStatistics();
		Map<String, String> properties = devices.get(0).getProperties();
		Assert.assertEquals("No data", properties.get("Sensor3005-AirQuality#Message"));
	}

	/**
	 * Test retrieveMultipleStatistics with aggregated device Sensor is Thermal no data
	 * <p>
	 * Expect retrieveMultipleStatistics with aggregated device Sensor is Thermal no data
	 */
	@Test
	void testRetrieveMultipleStatisticsWithSensorWhereThermalNoData() throws Exception {
		communicator.getMultipleStatistics();
		communicator.retrieveMultipleStatistics();
		Thread.sleep(30000);
		List<AggregatedDevice> devices = communicator.retrieveMultipleStatistics();
		Map<String, String> properties = devices.get(0).getProperties();
		Assert.assertEquals("No data", properties.get("Sensor3005-Thermal#Message"));
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
		Map<String, String> properties = devices.get(0).getProperties();
		Assert.assertNotNull(properties.get("OccupancyList#CurrentDate"));
		Assert.assertEquals("8", properties.get("OccupancyList#Hour"));
		Assert.assertNotNull(properties.get("OccupancyList#NumberOfOccupance"));
		Assert.assertNotNull(properties.get("OccupancyList#UsageTime"));
	}

	/**
	 * Test retrieveMultipleStatistics with aggregated device Region tag information group
	 * <p>
	 * Expect retrieveMultipleStatistics with aggregated device Region tag information group successfully
	 */
	@Test
	void testRetrieveMultipleStatisticsWithRegionTagInformation() throws Exception {
		communicator.getMultipleStatistics();
		communicator.retrieveMultipleStatistics();
		Thread.sleep(30000);
		List<AggregatedDevice> devices = communicator.retrieveMultipleStatistics();
		Map<String, String> properties = devices.get(0).getProperties();
		Assert.assertNotNull( properties.get("OccupancyList#CurrentDate"));
		Assert.assertEquals("8", properties.get("OccupancyList#Hour"));
		Assert.assertNotNull(properties.get("OccupancyList#NumberOfOccupance"));
		Assert.assertNotNull( properties.get("OccupancyList#UsageTime"));
	}

	/**
	 * Test control aggregated device with new Region tag
	 * <p>
	 * Expect control aggregated device with new Region tag successfully
	 */
	@Test
	void testControlAggregatedDeviceWithRegionTag() throws Exception {
		communicator.retrieveMultipleStatistics();
		Thread.sleep(30000);
		List<AggregatedDevice> devices = communicator.retrieveMultipleStatistics();
		Map<String, String> properties = devices.get(0).getProperties();
		Assert.assertEquals("", properties.get("RegionTag#NewTag"));
		ControllableProperty controllableProperty = new ControllableProperty();
		controllableProperty.setProperty("RegionTag#NewTag");
		controllableProperty.setValue("res3");
		controllableProperty.setDeviceId("TEST-1st floor-Region_2");
		communicator.controlProperty(controllableProperty);
		communicator.getMultipleStatistics();
		Thread.sleep(30000);
		devices = communicator.retrieveMultipleStatistics();
		properties = devices.get(0).getProperties();
		Assert.assertEquals("res3", properties.get("RegionTag#NewTag"));
	}

	/**
	 * Test control aggregated device with new Region tag
	 * <p>
	 * Expect control aggregated device with new Region tag successfully
	 */
	@Test
	void testControlAggregatedDeviceWithCreateRegionTag() throws Exception {
		communicator.retrieveMultipleStatistics();
		Thread.sleep(30000);
		List<AggregatedDevice> devices = communicator.retrieveMultipleStatistics();
		Map<String, String> properties = devices.get(0).getProperties();
		Assert.assertEquals("", properties.get("RegionTag#NewTag"));
		ControllableProperty controllableProperty = new ControllableProperty();
		controllableProperty.setProperty("RegionTag#NewTag");
		controllableProperty.setValue("res3");
		controllableProperty.setDeviceId("TEST-1st floor-Region_2");
		communicator.controlProperty(controllableProperty);
		communicator.getMultipleStatistics();
		communicator.retrieveMultipleStatistics();
		Thread.sleep(30000);
		devices = communicator.retrieveMultipleStatistics();
		properties = devices.get(0).getProperties();
		Assert.assertEquals("res3", properties.get("RegionTag#NewTag"));
		controllableProperty.setProperty("RegionTag#Create");
		controllableProperty.setValue("1");
		controllableProperty.setDeviceId("TEST-1st floor-Region_2");
		communicator.controlProperty(controllableProperty);
		communicator.retrieveMultipleStatistics();
		Thread.sleep(30000);
		devices = communicator.retrieveMultipleStatistics();
		properties = devices.get(0).getProperties();
		Assert.assertEquals("res3", properties.get("RegionTag#NewTag"));
	}


	/**
	 * Test control aggregated device delete Region tag
	 * <p>
	 * Expect control aggregated device delete Region tag successfully
	 */
	@Test
	void testControlAggregatedDeviceWithDeleteRegionTag() throws Exception {
		communicator.retrieveMultipleStatistics();
		Thread.sleep(30000);
		List<AggregatedDevice> devices = communicator.retrieveMultipleStatistics();
		Map<String, String> properties = devices.get(0).getProperties();
		Assert.assertEquals("", properties.get("RegionTag#NewTag"));
		Assert.assertEquals("res3", properties.get("RegionTag#Tag"));
		ControllableProperty controllableProperty = new ControllableProperty();
		controllableProperty.setProperty("RegionTag#Delete");
		controllableProperty.setValue("1");
		controllableProperty.setDeviceId("TEST-1st floor-Region_2");
		communicator.controlProperty(controllableProperty);
		communicator.getMultipleStatistics();
		communicator.retrieveMultipleStatistics();
		Thread.sleep(30000);
		devices = communicator.retrieveMultipleStatistics();
		properties = devices.get(0).getProperties();
		Assert.assertEquals("", properties.get("RegionTag#NewTag"));
		Assert.assertEquals(null, properties.get("RegionTag#Tag"));
	}

	/**
	 * Test control aggregated device delete Region tag
	 * <p>
	 * Expect control aggregated device delete Region tag successfully
	 */
	@Test
	void testControlAggregatedDeviceWithChangeRegionTag() throws Exception {
		testControlAggregatedDeviceWithCreateRegionTag();
		communicator.retrieveMultipleStatistics();
		Thread.sleep(30000);
		List<AggregatedDevice> devices = communicator.retrieveMultipleStatistics();
		Map<String, String> properties = devices.get(0).getProperties();
		Assert.assertEquals("res3", properties.get("RegionTag#NewTag"));
		Assert.assertEquals("res3", properties.get("RegionTag#Tag"));
		ControllableProperty controllableProperty = new ControllableProperty();
		controllableProperty.setProperty("RegionTag#Tag");
		controllableProperty.setValue("res2");
		controllableProperty.setDeviceId("TEST-1st floor-Region_2");
		communicator.controlProperty(controllableProperty);
		communicator.getMultipleStatistics();
		devices = communicator.retrieveMultipleStatistics();
		properties = devices.get(0).getProperties();
		Assert.assertEquals("res3", properties.get("RegionTag#NewTag"));
		Assert.assertEquals("res2", properties.get("RegionTag#Tag"));
	}

	/**
	 * Test control aggregated device with OccupancyList change hour
	 * <p>
	 * Expect control aggregated device with OccupancyList change hour successfully
	 */
	@Test
	void testControlAggregatedDeviceWithOccupancyList() throws Exception {
		testControlAggregatedDeviceWithCreateRegionTag();
		communicator.retrieveMultipleStatistics();
		Thread.sleep(30000);
		List<AggregatedDevice> devices = communicator.retrieveMultipleStatistics();
		Map<String, String> properties = devices.get(0).getProperties();
		Assert.assertNotNull(properties.get("OccupancyList#CurrentDate"));
		Assert.assertEquals("8", properties.get("OccupancyList#Hour"));
		Assert.assertNotNull(properties.get("OccupancyList#NumberOfOccupance"));
		Assert.assertNotNull(properties.get("OccupancyList#UsageTime"));
		ControllableProperty controllableProperty = new ControllableProperty();
		controllableProperty.setProperty("OccupancyList#Hour");
		controllableProperty.setValue("10");
		controllableProperty.setDeviceId("TEST-1st floor-Region_2");
		communicator.controlProperty(controllableProperty);
		communicator.getMultipleStatistics();
		communicator.retrieveMultipleStatistics();
		Thread.sleep(30000);
		devices = communicator.retrieveMultipleStatistics();
		properties = devices.get(0).getProperties();
		Assert.assertNotNull( properties.get("OccupancyList#CurrentDate"));
		Assert.assertEquals("10", properties.get("OccupancyList#Hour"));
		Assert.assertNotNull(properties.get("OccupancyList#NumberOfOccupance"));
		Assert.assertNotNull( properties.get("OccupancyList#UsageTime"));
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
		Assert.assertEquals(2, devices.size());
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
		Assert.assertEquals(2, devices.size());
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
	 * Test filter by Region type is Workstations
	 * <p>
	 * Expect filter by Region type is Workstations successfully
	 */
	@Test
	void testRegionFilterByName() throws Exception {
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
	 * Test filter by Region type is Workstations 01
	 * <p>
	 * Expect filter by Region type is Workstations 01 with no aggregated device
	 */
	@Test
	void testRegionFilterByNameNotExits() throws Exception {
		communicator.setRegionTypeFilter("Workstations 01");
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
		communicator.setRegionTypeFilter("Workstations");
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

	@Test
	void testPollingInterval() throws Exception {
		communicator.setPollingInterval("1");
		communicator.getMultipleStatistics();
		communicator.retrieveMultipleStatistics();
		Thread.sleep(30000);
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) communicator.getMultipleStatistics().get(0);
		System.out.println(extendedStatistics.getStatistics());
		extendedStatistics = (ExtendedStatistics) communicator.getMultipleStatistics().get(0);
		System.out.println(extendedStatistics.getStatistics());
	}
}