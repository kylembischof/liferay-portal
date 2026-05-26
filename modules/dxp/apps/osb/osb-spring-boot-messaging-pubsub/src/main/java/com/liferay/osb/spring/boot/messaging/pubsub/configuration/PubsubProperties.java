/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.osb.spring.boot.messaging.pubsub.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Kyle Bischof
 */
@ConfigurationProperties("pubsub")
public class PubsubProperties {

	public Broker getBroker() {
		return _broker;
	}

	public Subscriber getSubscriber() {
		return _subscriber;
	}

	public void setBroker(Broker broker) {
		_broker = broker;
	}

	public void setSubscriber(Subscriber subscriber) {
		_subscriber = subscriber;
	}

	public static class Broker {

		public boolean isAutoCreate() {
			return _autoCreate;
		}

		public void setAutoCreate(boolean autoCreate) {
			_autoCreate = autoCreate;
		}

		private boolean _autoCreate = true;

	}

	public static class Subscriber {

		public boolean isAutoCreate() {
			return _autoCreate;
		}

		public void setAutoCreate(boolean autoCreate) {
			_autoCreate = autoCreate;
		}

		private boolean _autoCreate = true;

	}

	private Broker _broker = new Broker();
	private Subscriber _subscriber = new Subscriber();

}