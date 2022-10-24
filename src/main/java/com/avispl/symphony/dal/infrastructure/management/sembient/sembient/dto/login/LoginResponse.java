/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.sembient.sembient.dto.login;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * LoginResponse class - A class contain information about:
 * <ol>
 *   <li>Expiration time(s) - 1 hour</li>
 *   <li>Bearer token</li>
 *   <li>Customer Id</li>
 *   <li>API key that we will attach in the header of every request</li>
 * </ol>
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 9/30/2022
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginResponse {
	private int exp;
	private long expirationTime;
	@JsonAlias("idToken")
	private String bearerToken;
	@JsonAlias("cid")
	private String customerId;
	private String apiKey;

	/**
	 * Retrieves {@link #exp}
	 *
	 * @return value of {@link #exp}
	 */
	public int getExp() {
		return exp;
	}

	/**
	 * Sets {@link #exp} value
	 *
	 * @param exp new value of {@link #exp}
	 */
	public void setExp(int exp) {
		this.exp = exp;
	}

	/**
	 * Retrieves {@link #expirationTime}
	 *
	 * @return value of {@link #expirationTime}
	 */
	public long getExpirationTime() {
		return expirationTime;
	}

	/**
	 * Sets {@link #expirationTime} value
	 *
	 * @param expirationTime new value of {@link #expirationTime}
	 */
	public void setExpirationTime(long expirationTime) {
		this.expirationTime = expirationTime;
	}

	/**
	 * Retrieves {@link #bearerToken}
	 *
	 * @return value of {@link #bearerToken}
	 */
	public String getBearerToken() {
		return bearerToken;
	}

	/**
	 * Sets {@link #bearerToken} value
	 *
	 * @param bearerToken new value of {@link #bearerToken}
	 */
	public void setBearerToken(String bearerToken) {
		this.bearerToken = bearerToken;
	}

	/**
	 * Retrieves {@link #customerId}
	 *
	 * @return value of {@link #customerId}
	 */
	public String getCustomerId() {
		return customerId;
	}

	/**
	 * Sets {@link #customerId} value
	 *
	 * @param customerId new value of {@link #customerId}
	 */
	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}

	/**
	 * Retrieves {@link #apiKey}
	 *
	 * @return value of {@link #apiKey}
	 */
	public String getApiKey() {
		return apiKey;
	}

	/**
	 * Sets {@link #apiKey} value
	 *
	 * @param apiKey new value of {@link #apiKey}
	 */
	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	@Override
	public String toString() {
		return "LoginResponse{" +
				"exp=" + exp +
				", expirationTime=" + expirationTime +
				", bearerToken='" + bearerToken + '\'' +
				", customerId='" + customerId + '\'' +
				", apiKey='" + apiKey + '\'' +
				'}';
	}
}
