/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.osb.spring.boot.messaging.pubsub.subscriber;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.rpc.NotFoundException;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.pubsub.v1.DeadLetterPolicy;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.Subscription;
import com.google.pubsub.v1.TopicName;

import com.liferay.osb.spring.boot.messaging.pubsub.configuration.PubsubProperties;
import com.liferay.osb.spring.boot.messaging.pubsub.credentials.ServiceAccountCredentialsProvider;
import com.liferay.osb.spring.boot.messaging.pubsub.router.MessageRouter;
import com.liferay.osb.spring.boot.messaging.pubsub.subscriber.internal.DefaultMessageReceiver;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Amos Fong
 * @author Kyle Bischof
 */
public abstract class BasePubsubSubscriber {

	@PostConstruct
	public void start() throws Exception {
		String userClassName = getClass().getName();

		String resolvedSubscriptionName = subscription;

		if ((resolvedSubscriptionName == null) ||
			resolvedSubscriptionName.isEmpty()) {

			resolvedSubscriptionName = userClassName;
		}

		String namespacedSubscription = namespace + resolvedSubscriptionName;
		String namespacedTopic = namespace + topic;

		ProjectSubscriptionName projectSubscriptionName =
			ProjectSubscriptionName.of(projectId, namespacedSubscription);

		if (isAutoCreate() && namespacedSubscription.contains(userClassName)) {
			_ensureSubscription(projectSubscriptionName, namespacedTopic);
		}

		_subscriber = Subscriber.newBuilder(
			projectSubscriptionName,
			new DefaultMessageReceiver(topic, messageRouter)
		).setCredentialsProvider(
			getCredentialsProvider()
		).build();

		_subscriber.startAsync(
		).awaitRunning();

		if (_log.isInfoEnabled()) {
			_log.info(
				"Listening for messages on " +
					_subscriber.getSubscriptionNameString());
		}
	}

	@PreDestroy
	public void stop() {
		if (_subscriber != null) {
			try {
				_subscriber.stopAsync(
				).awaitTerminated();

				if (_log.isInfoEnabled()) {
					_log.info(
						"Stopped listening for messages on " +
							_subscriber.getSubscriptionNameString());
				}
			}
			catch (Exception exception) {
				_log.error("Subscriber stop failed", exception);
			}
		}
	}

	protected CredentialsProvider getCredentialsProvider() throws Exception {
		ServiceAccountCredentialsProvider serviceAccountCredentialsProvider =
			getServiceAccountCredentialsProvider();

		return serviceAccountCredentialsProvider.getCredentialsProvider();
	}

	protected abstract ServiceAccountCredentialsProvider
			getServiceAccountCredentialsProvider()
		throws Exception;

	protected boolean isAutoCreate() {
		PubsubProperties.Subscriber subscriber =
			pubsubProperties.getSubscriber();

		return subscriber.isAutoCreate();
	}

	protected boolean deadLetterEnabled = true;
	protected int maxDeliveryAttempts = 5;
	protected String messageFilter = "";

	@Autowired
	protected MessageRouter messageRouter;

	protected String namespace = "";
	protected String projectId;

	@Autowired
	protected PubsubProperties pubsubProperties;

	protected String subscription = "";
	protected String topic;

	private void _ensureSubscription(
			ProjectSubscriptionName projectSubscriptionName,
			String namespacedTopic)
		throws Exception {

		SubscriptionAdminSettings subscriptionAdminSettings =
			SubscriptionAdminSettings.newBuilder(
			).setCredentialsProvider(
				getCredentialsProvider()
			).build();

		try (SubscriptionAdminClient subscriptionAdminClient =
				SubscriptionAdminClient.create(subscriptionAdminSettings)) {

			try {
				subscriptionAdminClient.getSubscription(
					projectSubscriptionName);

				if (_log.isDebugEnabled()) {
					_log.debug("Found subscription " + projectSubscriptionName);
				}

				return;
			}
			catch (NotFoundException notFoundException) {
				if (_log.isDebugEnabled()) {
					_log.debug(
						"Subscription not found, creating", notFoundException);
				}
			}

			TopicName topicName = TopicName.ofProjectTopicName(
				projectId, namespacedTopic);

			Subscription.Builder builder = Subscription.newBuilder(
			).setAckDeadlineSeconds(
				30
			).setFilter(
				messageFilter
			).setName(
				projectSubscriptionName.toString()
			).setTopic(
				topicName.toString()
			);

			if (deadLetterEnabled) {
				TopicName dlqTopicName = TopicName.ofProjectTopicName(
					projectId, namespacedTopic + "-dlq");

				builder.setDeadLetterPolicy(
					DeadLetterPolicy.newBuilder(
					).setDeadLetterTopic(
						dlqTopicName.toString()
					).setMaxDeliveryAttempts(
						maxDeliveryAttempts
					).build());
			}

			Subscription subscription = builder.build();

			if (_log.isDebugEnabled()) {
				_log.debug("Creating subscription " + subscription);
			}

			subscriptionAdminClient.createSubscription(subscription);
		}
	}

	private static final Logger _log = LoggerFactory.getLogger(
		BasePubsubSubscriber.class);

	private Subscriber _subscriber;

}