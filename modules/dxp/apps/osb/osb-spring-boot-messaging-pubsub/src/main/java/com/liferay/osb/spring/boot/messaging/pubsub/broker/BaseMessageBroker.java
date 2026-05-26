/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.osb.spring.boot.messaging.pubsub.broker;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.retrying.RetrySettings;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.NotFoundException;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;

import com.liferay.osb.spring.boot.messaging.pubsub.Message;
import com.liferay.osb.spring.boot.messaging.pubsub.configuration.PubsubProperties;
import com.liferay.osb.spring.boot.messaging.pubsub.credentials.ServiceAccountCredentialsProvider;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Amos Fong
 * @author Kyle Bischof
 */
public abstract class BaseMessageBroker implements MessageBroker {

	@Override
	public synchronized void publish(String topic, Message message)
		throws Exception {

		Publisher publisher = _publisherMap.get(topic);

		if (publisher == null) {
			Publisher.Builder builder = Publisher.newBuilder(
				TopicName.ofProjectTopicName(projectId, namespace + topic)
			).setCredentialsProvider(
				getCredentialsProvider()
			).setEnableMessageOrdering(
				true
			);

			TransportChannelProvider channelProvider = getChannelProvider();

			if (channelProvider != null) {
				builder.setChannelProvider(channelProvider);
			}

			RetrySettings retrySettings = buildRetrySettings();

			if (retrySettings != null) {
				builder.setRetrySettings(retrySettings);
			}

			publisher = builder.build();

			_publisherMap.put(topic, publisher);
		}

		PubsubMessage pubsubMessage = PubsubMessage.newBuilder(
		).putAllAttributes(
			message.getStringAttributes()
		).setData(
			ByteString.copyFromUtf8((String)message.getPayload())
		).setOrderingKey(
			getOrderingKey(message)
		).build();

		ApiFuture<String> apiFuture = publisher.publish(pubsubMessage);

		ApiFutures.addCallback(
			apiFuture,
			new ApiFutureCallback<String>() {

				public void onFailure(Throwable throwable) {
					_log.error("Failed to publish message", throwable);
				}

				public void onSuccess(String messageId) {
					if (_log.isDebugEnabled()) {
						_log.debug("Published message: " + messageId);
					}
				}

			},
			MoreExecutors.directExecutor());
	}

	@PreDestroy
	public void shutdown() {
		try {
			for (Publisher publisher : _publisherMap.values()) {
				publisher.shutdown();

				publisher.awaitTermination(1, TimeUnit.MINUTES);
			}
		}
		catch (Exception exception) {
			_log.error("Failed to shut down publishers", exception);
		}

		if (_emulatorChannel != null) {
			_emulatorChannel.shutdownNow();
		}
	}

	@PostConstruct
	public void start() throws Exception {
		if (!isAutoCreate()) {
			return;
		}

		TopicAdminSettings.Builder topicAdminSettingsBuilder =
			TopicAdminSettings.newBuilder(
			).setCredentialsProvider(
				getCredentialsProvider()
			);

		TransportChannelProvider channelProvider = getChannelProvider();

		if (channelProvider != null) {
			topicAdminSettingsBuilder.setTransportChannelProvider(
				channelProvider);
		}

		TopicAdminSettings topicAdminSettings =
			topicAdminSettingsBuilder.build();

		try (TopicAdminClient topicAdminClient = TopicAdminClient.create(
				topicAdminSettings)) {

			for (String topic : getDeclaredTopics()) {
				_createIfMissing(topicAdminClient, namespace + topic);

				if (deadLetterEnabled) {
					_createIfMissing(
						topicAdminClient, namespace + topic + "-dlq");
				}
			}
		}
	}

	protected RetrySettings buildRetrySettings() {
		return null;
	}

	protected TransportChannelProvider getChannelProvider() {
		String emulatorHost = System.getenv("PUBSUB_EMULATOR_HOST");

		if ((emulatorHost == null) || emulatorHost.isEmpty()) {
			return null;
		}

		if (_emulatorChannelProvider == null) {
			_emulatorChannel = ManagedChannelBuilder.forTarget(
				emulatorHost
			).usePlaintext(
			).build();

			_emulatorChannelProvider = FixedTransportChannelProvider.create(
				GrpcTransportChannel.create(_emulatorChannel));
		}

		return _emulatorChannelProvider;
	}

	protected CredentialsProvider getCredentialsProvider() throws Exception {
		if (_isEmulatorEnabled()) {
			return NoCredentialsProvider.create();
		}

		ServiceAccountCredentialsProvider serviceAccountCredentialsProvider =
			getServiceAccountCredentialsProvider();

		return serviceAccountCredentialsProvider.getCredentialsProvider();
	}

	protected abstract Set<String> getDeclaredTopics();

	protected String getOrderingKey(Message message) {
		Class<?> clazz = getClass();

		return clazz.getName();
	}

	protected abstract ServiceAccountCredentialsProvider
			getServiceAccountCredentialsProvider()
		throws Exception;

	protected boolean isAutoCreate() {
		PubsubProperties.Broker broker = pubsubProperties.getBroker();

		return broker.isAutoCreate();
	}

	protected boolean deadLetterEnabled = true;
	protected String namespace = "";
	protected String projectId;

	@Autowired
	protected PubsubProperties pubsubProperties;

	private void _createIfMissing(
		TopicAdminClient topicAdminClient, String topicName) {

		TopicName fullTopicName = TopicName.ofProjectTopicName(
			projectId, topicName);

		try {
			topicAdminClient.getTopic(fullTopicName);
		}
		catch (NotFoundException notFoundException) {
			if (_log.isDebugEnabled()) {
				_log.debug("Topic not found, creating", notFoundException);
			}

			topicAdminClient.createTopic(fullTopicName);

			if (_log.isInfoEnabled()) {
				_log.info("Created topic " + fullTopicName);
			}
		}
	}

	private boolean _isEmulatorEnabled() {
		String emulatorHost = System.getenv("PUBSUB_EMULATOR_HOST");

		if ((emulatorHost != null) && !emulatorHost.isEmpty()) {
			return true;
		}

		return false;
	}

	private static final Logger _log = LoggerFactory.getLogger(
		BaseMessageBroker.class);

	private ManagedChannel _emulatorChannel;
	private TransportChannelProvider _emulatorChannelProvider;
	private final Map<String, Publisher> _publisherMap = new HashMap<>();

}