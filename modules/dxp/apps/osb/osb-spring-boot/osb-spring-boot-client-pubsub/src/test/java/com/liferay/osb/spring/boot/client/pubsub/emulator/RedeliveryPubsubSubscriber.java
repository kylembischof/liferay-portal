/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.osb.spring.boot.client.pubsub.emulator;

import com.liferay.osb.spring.boot.client.pubsub.Message;
import com.liferay.osb.spring.boot.client.pubsub.subscriber.BasePubsubSubscriber;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author Kyle Bischof
 */
@Component
public class RedeliveryPubsubSubscriber extends BasePubsubSubscriber {

	public int getAttemptCount() {
		return _attemptCount.get();
	}

	public BlockingQueue<String> getProcessedPayloads() {
		return _processedPayloads;
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
	protected String getTopic() {
		return "redelivery";
	}

	@Override
	protected boolean isDeadLetterEnabled() {
		return false;
	}

	@Override
	protected void receive(Message message) throws Exception {
		if (_attemptCount.incrementAndGet() == 1) {
			throw new IllegalStateException(
				"Simulated failure on first delivery");
		}

		_processedPayloads.offer(message.getPayload());
	}

	private final AtomicInteger _attemptCount = new AtomicInteger();

	@Value("${smoke.pubsub.namespace:}")
	private String _namespace;

	private final BlockingQueue<String> _processedPayloads =
		new LinkedBlockingQueue<>();

	@Value("${smoke.pubsub.project.id:smoke-test}")
	private String _projectId;

}