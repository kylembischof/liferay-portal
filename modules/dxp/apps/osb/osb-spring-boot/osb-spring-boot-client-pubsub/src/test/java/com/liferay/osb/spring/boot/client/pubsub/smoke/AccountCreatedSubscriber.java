/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.osb.spring.boot.client.pubsub.smoke;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.liferay.osb.spring.boot.client.pubsub.Message;
import com.liferay.osb.spring.boot.client.pubsub.subscriber.BasePubsubSubscriber;

import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author Kyle Bischof
 */
@Component
public class AccountCreatedSubscriber extends BasePubsubSubscriber {

	@Override
	protected String getNamespace() {
		return _namespace;
	}

	@Override
	protected String getProjectId() {
		return _projectId;
	}

	@Override
	protected String getTopic() {
		return _topic;
	}

	@Override
	protected boolean isDeadLetterEnabled() {
		return false;
	}

	@Override
	protected void receive(Message message) throws Exception {
		Account account = _objectMapper.readValue(
			message.getPayload(), Account.class);

		if ((account.getAccountKey() == null) ||
			account.getAccountKey(
			).isEmpty() || (account.getAppId() == null) ||
			account.getAppId(
			).isEmpty()) {

			_log.warning(
				"Missing \"accountKey\" or \"appId\" in account-created " +
					"message");

			return;
		}

		_accountRegistry.register(account);
	}

	private static final Logger _log = Logger.getLogger(
		AccountCreatedSubscriber.class.getName());

	@Autowired
	private AccountRegistry _accountRegistry;

	@Value("${smoke.pubsub.namespace:}")
	private String _namespace;

	private final ObjectMapper _objectMapper = new ObjectMapper();

	@Value("${smoke.pubsub.project.id:smoke-test}")
	private String _projectId;

	@Value("${smoke.pubsub.account.created.topic:account-created}")
	private String _topic;

}