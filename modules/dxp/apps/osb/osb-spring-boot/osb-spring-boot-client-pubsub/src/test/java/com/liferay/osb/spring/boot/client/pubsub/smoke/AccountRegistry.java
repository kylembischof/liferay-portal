/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.osb.spring.boot.client.pubsub.smoke;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * @author Kyle Bischof
 */
@Component
public class AccountRegistry {

	public Account getAccount(String accountKey) {
		return _accountsByKey.get(accountKey);
	}

	public Map<String, Account> getAccounts() {
		return _accountsByKey;
	}

	public void register(Account account) {
		_accountsByKey.put(account.getAccountKey(), account);
	}

	private final Map<String, Account> _accountsByKey =
		new ConcurrentHashMap<>();

}