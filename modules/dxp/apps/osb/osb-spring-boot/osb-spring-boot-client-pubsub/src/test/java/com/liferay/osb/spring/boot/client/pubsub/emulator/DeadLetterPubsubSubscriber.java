/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.osb.spring.boot.client.pubsub.emulator;

import com.liferay.osb.spring.boot.client.pubsub.Message;
import com.liferay.osb.spring.boot.client.pubsub.subscriber.BasePubsubSubscriber;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author Kyle Bischof
 */
@Component
public class DeadLetterPubsubSubscriber extends BasePubsubSubscriber {

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
		return "dead-letter";
	}

	@Override
	protected void receive(Message message) throws Exception {
	}

	@Value("${smoke.pubsub.namespace:}")
	private String _namespace;

	@Value("${smoke.pubsub.project.id:smoke-test}")
	private String _projectId;

}