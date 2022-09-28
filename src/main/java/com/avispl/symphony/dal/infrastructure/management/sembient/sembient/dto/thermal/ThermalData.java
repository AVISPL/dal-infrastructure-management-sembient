package com.avispl.symphony.dal.infrastructure.management.sembient.sembient.dto.thermal;

public class ThermalData {
	private String temperature;
	private String humidity;
	private Long timestamp;

	/**
	 * Retrieves {@link #temperature}
	 *
	 * @return value of {@link #temperature}
	 */
	public String getTemperature() {
		return temperature;
	}

	/**
	 * Sets {@link #temperature} value
	 *
	 * @param temperature new value of {@link #temperature}
	 */
	public void setTemperature(String temperature) {
		this.temperature = temperature;
	}

	/**
	 * Retrieves {@link #humidity}
	 *
	 * @return value of {@link #humidity}
	 */
	public String getHumidity() {
		return humidity;
	}

	/**
	 * Sets {@link #humidity} value
	 *
	 * @param humidity new value of {@link #humidity}
	 */
	public void setHumidity(String humidity) {
		this.humidity = humidity;
	}

	/**
	 * Retrieves {@link #timestamp}
	 *
	 * @return value of {@link #timestamp}
	 */
	public Long getTimestamp() {
		return timestamp;
	}

	/**
	 * Sets {@link #timestamp} value
	 *
	 * @param timestamp new value of {@link #timestamp}
	 */
	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	public String toString() {
		return "ThermalData{" +
				"temperature='" + temperature + '\'' +
				", humidity='" + humidity + '\'' +
				", timestamp=" + timestamp +
				'}';
	}
}
