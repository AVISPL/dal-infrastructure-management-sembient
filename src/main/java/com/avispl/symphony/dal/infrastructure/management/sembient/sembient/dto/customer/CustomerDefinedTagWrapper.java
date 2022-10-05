/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.sembient.sembient.dto.customer;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * CustomerDefinedTagWrapper class - A class that provide information about:
 * <ol>
 *   <li>Status code</li>
 *   <li>Array of {@link CustomerDefinedTagResponse}</li>
 * </ol>
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 9/30/2022
 * @since 1.0.0
 */
public class CustomerDefinedTagWrapper {
	private String statusCode;
	@JsonAlias("body")
	private CustomerDefinedTagResponse[] customerDefinedTagResponses;

	/**
	 * Retrieves {@link #statusCode}
	 *
	 * @return value of {@link #statusCode}
	 */
	public String getStatusCode() {
		return statusCode;
	}

	/**
	 * Sets {@link #statusCode} value
	 *
	 * @param statusCode new value of {@link #statusCode}
	 */
	public void setStatusCode(String statusCode) {
		this.statusCode = statusCode;
	}

	/**
	 * Retrieves {@link #customerDefinedTagResponses}
	 *
	 * @return value of {@link #customerDefinedTagResponses}
	 */
	public CustomerDefinedTagResponse[] getCustomerDefinedTagResponses() {
		return customerDefinedTagResponses;
	}

	/**
	 * Sets {@link #customerDefinedTagResponses} value
	 *
	 * @param customerDefinedTagResponses new value of {@link #customerDefinedTagResponses}
	 */
	public void setCustomerDefinedTagResponses(CustomerDefinedTagResponse[] customerDefinedTagResponses) {
		this.customerDefinedTagResponses = customerDefinedTagResponses;
	}
}
