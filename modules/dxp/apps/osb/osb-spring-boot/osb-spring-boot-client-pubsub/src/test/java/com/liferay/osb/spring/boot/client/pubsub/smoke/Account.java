/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.osb.spring.boot.client.pubsub.smoke;

/**
 * @author Kyle Bischof
 */
public class Account {

	public Account() {
	}

	public Account(String accountKey, String appId) {
		_accountKey = accountKey;
		_appId = appId;
	}

	public String getAccountKey() {
		return _accountKey;
	}

	public String getAppId() {
		return _appId;
	}

	public void setAccountKey(String accountKey) {
		_accountKey = accountKey;
	}

	public void setAppId(String appId) {
		_appId = appId;
	}

	private String _accountKey;
	private String _appId;

}