/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.sembient.sembient.dto.login;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * LoginWrapper class - A class that provide information about:
 * <ol>
 *   <li>Status code</li>
 *   <li>{@link LoginResponse}</li>
 * </ol>
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 9/30/2022
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginWrapper {
	private String statusCode;
	@JsonAlias("body")
	private LoginResponse loginResponse;

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

	@Override
	public String toString() {
		return "LoginWrapper{" +
				"statusCode='" + statusCode + '\'' +
				", loginResponse=" + loginResponse +
				'}';
	}
}
