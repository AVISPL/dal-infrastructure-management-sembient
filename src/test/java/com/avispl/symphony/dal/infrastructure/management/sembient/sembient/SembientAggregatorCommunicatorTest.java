/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.sembient.sembient;

import java.util.List;
import java.util.Map;

import com.avispl.symphony.api.dal.dto.control.ControllableProperty;

import javax.security.auth.login.FailedLoginException;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.aggregator.AggregatedDevice;
import com.avispl.symphony.api.dal.error.ResourceNotReachableException;

@Tag("RealDevice")
class SembientAggregatorCommunicatorTest {
	private SembientAggregatorCommunicator communicator;

	@BeforeEach
	void setUp() throws Exception {
		communicator = new SembientAggregatorCommunicator();
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
		Assert.assertNotNull( properties);
		Assert.assertNotNull( properties.get(key + "Temperature(F)"));
		Assert.assertNotNull( properties.get(key + "FromTime"));
		Assert.assertNotNull( properties.get(key + "ToTime"));
		Assert.assertNotNull( properties.get(key + "RecentData"));
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
		Assert.assertNotNull(properties.get(key + "CO2(ppm)"));
		Assert.assertNotNull(properties.get(key + "PM25Value(microgram/m3)"));
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
		Assert.assertEquals("No data", properties.get("Sensor3005-AirQuality#Message"));
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
		Map<String, String> properties = devices.get(4).getProperties();
		Assert.assertNotNull(properties.get("OccupancyList#CurrentDate"));
		Assert.assertEquals("8", properties.get("OccupancyList#Hour"));
		Assert.assertNotNull(properties.get("OccupancyList#NumberOfOccupants"));
		Assert.assertNotNull(properties.get("OccupancyList#UsageTime"));
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
		Map<String, String> properties = devices.get(4).getProperties();
		Assert.assertEquals("", properties.get("RegionTags#NewTag"));
		ControllableProperty controllableProperty = new ControllableProperty();
		controllableProperty.setProperty("RegionTags#NewTag");
		controllableProperty.setValue("res3");
		controllableProperty.setDeviceId("Region-SANDBOX-KRFOBAZP-TEST-1st floor-Region_2");
		communicator.controlProperty(controllableProperty);
		communicator.getMultipleStatistics();
		Thread.sleep(30000);
		devices = communicator.retrieveMultipleStatistics();
		properties = devices.get(4).getProperties();
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
		Map<String, String> properties = devices.get(4).getProperties();
		Assert.assertEquals("", properties.get("RegionTags#NewTag"));
		ControllableProperty controllableProperty = new ControllableProperty();
		controllableProperty.setProperty("RegionTags#NewTag");
		controllableProperty.setValue("res3");
		controllableProperty.setDeviceId("Region-SANDBOX-KRFOBAZP-TEST-1st floor-Region_2");
		communicator.controlProperty(controllableProperty);
		communicator.getMultipleStatistics();
		communicator.retrieveMultipleStatistics();
		Thread.sleep(30000);
		devices = communicator.retrieveMultipleStatistics();
		properties = devices.get(4).getProperties();
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
		Map<String, String> properties = devices.get(4).getProperties();
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
		properties = devices.get(4).getProperties();
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
		Map<String, String> properties = devices.get(4).getProperties();
		Assert.assertEquals("", properties.get("RegionTags#NewTag"));
		ControllableProperty controllableProperty = new ControllableProperty();
		controllableProperty.setProperty("RegionTags#Tag");
		controllableProperty.setValue("res2");
		controllableProperty.setDeviceId("Region-SANDBOX-KRFOBAZP-TEST-1st floor-Region_2");
		communicator.controlProperty(controllableProperty);
		communicator.getMultipleStatistics();
		devices = communicator.retrieveMultipleStatistics();
		properties = devices.get(4).getProperties();
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
		Map<String, String> properties = devices.get(4).getProperties();
		Assert.assertNotNull(properties.get("OccupancyList#CurrentDate"));
		Assert.assertEquals("8", properties.get("OccupancyList#Hour"));
		Assert.assertNotNull(properties.get("OccupancyList#NumberOfOccupants"));
		Assert.assertNotNull(properties.get("OccupancyList#UsageTime"));
		ControllableProperty controllableProperty = new ControllableProperty();
		controllableProperty.setProperty("OccupancyList#Hour");
		controllableProperty.setValue("10");
		controllableProperty.setDeviceId("Region-SANDBOX-KRFOBAZP-TEST-1st floor-Region_2");
		communicator.controlProperty(controllableProperty);
		communicator.getMultipleStatistics();
		communicator.retrieveMultipleStatistics();
		Thread.sleep(30000);
		devices = communicator.retrieveMultipleStatistics();
		properties = devices.get(4).getProperties();
		Assert.assertNotNull( properties.get("OccupancyList#CurrentDate"));
		Assert.assertEquals("10", properties.get("OccupancyList#Hour"));
		Assert.assertNotNull(properties.get("OccupancyList#NumberOfOccupants"));
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
		communicator.setDeviceTypeFilter("Workstations");
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
		communicator.setDeviceTypeFilter("Workstations");
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
		communicator.setInstallationLayoutPollingCycle("");
		communicator.getMultipleStatistics();
		communicator.retrieveMultipleStatistics();
		Thread.sleep(30000);
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) communicator.getMultipleStatistics().get(0);
		communicator.retrieveMultipleStatistics();
		Assert.assertNotNull(extendedStatistics.getStatistics().get("NextRefreshInterval"));
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
		controllableProperty.setProperty("RegionTags#NewTag");
		controllableProperty.setValue("");
		controllableProperty.setDeviceId("Region-SANDBOX-KRFOBAZP-TEST-1st floor-Region_2");

		Exception exception = Assert.assertThrows(ResourceNotReachableException.class, () -> {
			communicator.controlProperty(controllableProperty);
		});

		String expectedMessage = "Cannot create new region tag with value is empty or null";
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
			communicator.getMultipleStatistics();;
		});
		String expectedMessage = "Fail to login with username: not-exist-account, password: useforTMA";
		String actualMessage = exception.getMessage();

		Assert.assertTrue(actualMessage.contains(expectedMessage));
	}

//	@Test()
//	void test11() throws Exception {
//		communicator.getMultipleStatistics();
//		int[] a = communicator.test("2022-09-19");
//		int[] b = communicator.test("2022-09-20");
//		int[] c = communicator.test("2022-09-21");
//		int[] d = communicator.test("2022-09-22");
//		int[] e = communicator.test("2022-09-23");
//		int[] f = communicator.test("2022-09-24");
//		int[] g = communicator.test("2022-09-25");
////		int[] b = communicator.test("2022-10-04");
////		communicator.test("2022-10-05");
////		int[] c = communicator.test("2022-10-06");
////		int[] d = communicator.test("2022-10-07");
////		int[] e = communicator.test("2022-10-08");
////		int[] f = communicator.test("2022-10-09");
//		int suma = 0;
//		int sumb =0;
//		int sumc =0;
//		int sum =0;
//		for (int i = 0; i < 3; i++) {
//			if (i ==0 ){
//				suma += a[i];
//				suma += b[i];
//				suma += c[i];
//				suma += d[i];
//				suma += e[i];
//				suma += f[i];
//				suma += g[i];
//				sum+=suma;
//			}
//			if (i == 1) {
//				sumb += a[i];
//				sumb += b[i];
//				sumb += c[i];
//				sumb += d[i];
//				sumb += e[i];
//				sumb += f[i];
//				sumb += g[i];
//				sum+=sumb;
//			}
//
//			if (i == 2) {
//				sumc += a[i];
//				sumc += b[i];
//				sumc += c[i];
//				sumc += d[i];
//				sumc += e[i];
//				sumc += f[i];
//				sumc += g[i];
//				sum+=sumc;
//			}
//
//		}
//		System.out.println(sum);
//		System.out.println(suma);
//		System.out.println(sumb);
//		System.out.println(sumc);
//	}
}