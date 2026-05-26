/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.osb.spring.boot.messaging.pubsub.emulator;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.Subscription;
import com.google.pubsub.v1.TopicName;

import com.liferay.osb.spring.boot.messaging.pubsub.Message;
import com.liferay.osb.spring.boot.messaging.pubsub.broker.BaseMessageBroker;
import com.liferay.osb.spring.boot.messaging.pubsub.credentials.ServiceAccountCredentialsProvider;
import com.liferay.osb.spring.boot.messaging.pubsub.router.BaseMessageRouter;
import com.liferay.osb.spring.boot.messaging.pubsub.subscriber.BaseMessageSubscriber;
import com.liferay.osb.spring.boot.messaging.pubsub.subscriber.BasePubsubSubscriber;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.lang.reflect.Field;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.RandomStringUtils;

import org.json.JSONObject;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * The ticket's required smoke test (spec §9.3). Drives a real publisher and
 * subscriber against the Google Cloud Pub/Sub emulator binary
 * (<code>gcloud beta emulators pubsub start</code>), verifying that the
 * production BaseMessageBroker, BasePubsubSubscriber, BaseMessageRouter,
 * BaseMessageSubscriber, and DefaultMessageReceiver cooperate end-to-end
 * through the actual google-cloud-pubsub wire path.
 *
 * <p>Activates only when <code>PUBSUB_EMULATOR_HOST</code> env var is set
 * (e.g. <code>localhost:8911</code>), per Google's standard emulator
 * convention. The Gradle <code>:startPubsubEmulator</code> task starts the
 * emulator and exports the env var; for local runs do:
 * <pre>
 *   JAVA_HOME=&lt;jdk17+&gt; PATH=$JAVA_HOME/bin:$PATH \
 *       gcloud beta emulators pubsub start --host-port=localhost:8911 &amp;
 *   PUBSUB_EMULATOR_HOST=localhost:8911 \
 *       gradlew test --tests com.liferay.osb.spring.boot.messaging.pubsub.emulator.*
 * </pre>
 * Without the env var the test is skipped via <code>Assume</code>.
 *
 * @author Kyle Bischof
 */
public class EmulatorRoundtripTest {

	@BeforeClass
	public static void setUpClass() throws Exception {
		String emulatorHost = System.getenv("PUBSUB_EMULATOR_HOST");

		Assume.assumeNotNull(emulatorHost);
		Assume.assumeFalse(emulatorHost.isEmpty());

		_channel = ManagedChannelBuilder.forTarget(
			emulatorHost
		).usePlaintext(
		).build();

		_channelProvider = FixedTransportChannelProvider.create(
			GrpcTransportChannel.create(_channel));

		_provisionTopicAndSubscription();
	}

	@AfterClass
	public static void tearDownClass() {
		if (_channel != null) {
			_channel.shutdownNow();
		}
	}

	@Test(timeout = 30000)
	public void testRoundtripPublishesAndConsumes() throws Exception {
		String correlationId = RandomStringUtils.randomAlphanumeric(12);
		String messageId = RandomStringUtils.randomAlphanumeric(12);

		BaseMessageRouter router = new BaseMessageRouter();

		LinkedBlockingQueue<JSONObject> received = new LinkedBlockingQueue<>();

		BaseMessageSubscriber businessSubscriber = new BaseMessageSubscriber() {

			@Override
			protected void doParse(JSONObject jsonObject) {
				received.add(jsonObject);
			}

		};

		router.addRoute(businessSubscriber, Collections.singletonList(_TOPIC));

		TestPubsubSubscriber testSubscriber = new TestPubsubSubscriber(router);

		testSubscriber.start();

		try {
			TestMessageBroker broker = new TestMessageBroker();

			Map<String, Object> attributes = new HashMap<>();

			attributes.put("correlationId", correlationId);

			broker.publish(
				_TOPIC,
				new Message(attributes, "{\"id\":\"" + messageId + "\"}"));

			JSONObject parsedJSONObject = received.poll(10, TimeUnit.SECONDS);

			Assert.assertNotNull(
				"Subscriber did not receive the message within 10s",
				parsedJSONObject);
			Assert.assertEquals(messageId, parsedJSONObject.getString("id"));
		}
		finally {
			testSubscriber.stop();
		}
	}

	private static void _provisionTopicAndSubscription() throws Exception {
		TopicAdminSettings topicAdminSettings = TopicAdminSettings.newBuilder(
		).setCredentialsProvider(
			NoCredentialsProvider.create()
		).setTransportChannelProvider(
			_channelProvider
		).build();

		try (TopicAdminClient topicAdminClient = TopicAdminClient.create(
				topicAdminSettings)) {

			try {
				topicAdminClient.createTopic(
					TopicName.ofProjectTopicName(_PROJECT_ID, _TOPIC));
			}
			catch (Exception exception) {
				if (!exception.getMessage(
					).contains(
						"ALREADY_EXISTS"
					)) {

					throw exception;
				}
			}
		}

		SubscriptionAdminSettings subscriptionAdminSettings =
			SubscriptionAdminSettings.newBuilder(
			).setCredentialsProvider(
				NoCredentialsProvider.create()
			).setTransportChannelProvider(
				_channelProvider
			).build();

		try (SubscriptionAdminClient subscriptionAdminClient =
				SubscriptionAdminClient.create(subscriptionAdminSettings)) {

			try {
				subscriptionAdminClient.createSubscription(
					Subscription.newBuilder(
					).setAckDeadlineSeconds(
						10
					).setName(
						ProjectSubscriptionName.format(
							_PROJECT_ID, _SUBSCRIPTION)
					).setTopic(
						TopicName.format(_PROJECT_ID, _TOPIC)
					).build());
			}
			catch (Exception exception) {
				if (!exception.getMessage(
					).contains(
						"ALREADY_EXISTS"
					)) {

					throw exception;
				}
			}
		}
	}

	private static final String _PROJECT_ID = "test-project";

	private static final String _SUBSCRIPTION =
		"roundtrip-sub-" +
			RandomStringUtils.randomAlphanumeric(
				8
			).toLowerCase();

	private static final String _TOPIC =
		"roundtrip-topic-" +
			RandomStringUtils.randomAlphanumeric(
				8
			).toLowerCase();

	private static ManagedChannel _channel;
	private static TransportChannelProvider _channelProvider;

	private static class TestMessageBroker extends BaseMessageBroker {

		public TestMessageBroker() {
			projectId = _PROJECT_ID;
		}

		@Override
		protected TransportChannelProvider getChannelProvider() {
			return _channelProvider;
		}

		@Override
		protected CredentialsProvider getCredentialsProvider() {
			return NoCredentialsProvider.create();
		}

		@Override
		protected Set<String> getDeclaredTopics() {
			return Collections.singleton(_TOPIC);
		}

		@Override
		protected ServiceAccountCredentialsProvider
			getServiceAccountCredentialsProvider() {

			throw new UnsupportedOperationException();
		}

		@Override
		protected boolean isAutoCreate() {
			return false;
		}

	}

	private static class TestPubsubSubscriber extends BasePubsubSubscriber {

		public TestPubsubSubscriber(BaseMessageRouter router) {
			projectId = _PROJECT_ID;
			subscription = _SUBSCRIPTION;
			topic = _TOPIC;
			deadLetterEnabled = false;

			try {
				Field field = BasePubsubSubscriber.class.getDeclaredField(
					"messageRouter");

				field.setAccessible(true);
				field.set(this, router);
			}
			catch (Exception exception) {
				throw new RuntimeException(exception);
			}
		}

		@Override
		protected TransportChannelProvider getChannelProvider() {
			return _channelProvider;
		}

		@Override
		protected CredentialsProvider getCredentialsProvider() {
			return NoCredentialsProvider.create();
		}

		@Override
		protected ServiceAccountCredentialsProvider
			getServiceAccountCredentialsProvider() {

			throw new UnsupportedOperationException();
		}

		@Override
		protected boolean isAutoCreate() {
			return false;
		}

	}

}