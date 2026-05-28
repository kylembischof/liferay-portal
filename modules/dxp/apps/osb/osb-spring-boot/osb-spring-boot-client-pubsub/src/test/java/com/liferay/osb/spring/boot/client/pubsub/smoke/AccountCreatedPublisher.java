/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.osb.spring.boot.client.pubsub.smoke;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.liferay.osb.spring.boot.client.pubsub.Message;
import com.liferay.osb.spring.boot.client.pubsub.publisher.BasePubsubPublisher;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author Kyle Bischof
 */
@Component
public class AccountCreatedPublisher extends BasePubsubPublisher {

	public void publishAccountCreated(Account account) throws Exception {
		String payload = _objectMapper.writeValueAsString(account);

		publish(new Message(Collections.emptyMap(), payload, _topic));
	}

	@Override
	protected String getNamespace() {
		return _namespace;
	}

	@Override
	protected String getProjectId() {
		return _projectId;
	}

	@Override
	protected boolean isDeadLetterEnabled() {
		return false;
	}

	@Value("${smoke.pubsub.namespace:}")
	private String _namespace;

	private final ObjectMapper _objectMapper = new ObjectMapper();

	@Value("${smoke.pubsub.project.id:smoke-test}")
	private String _projectId;

	@Value("${smoke.pubsub.account.created.topic:account-created}")
	private String _topic;

}