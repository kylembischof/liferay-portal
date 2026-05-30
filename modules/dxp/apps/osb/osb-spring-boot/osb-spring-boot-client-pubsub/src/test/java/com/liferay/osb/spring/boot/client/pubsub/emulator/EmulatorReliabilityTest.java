/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.osb.spring.boot.client.pubsub.emulator;

import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.pubsub.v1.DeadLetterPolicy;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.Subscription;

import com.liferay.osb.spring.boot.client.pubsub.Message;
import com.liferay.osb.spring.boot.client.pubsub.smoke.AccountCreatedPublisher;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @author Kyle Bischof
 */
public class EmulatorReliabilityTest {

	@BeforeClass
	public static void setUpClass() throws Exception {
		Assume.assumeTrue(
			"PUBSUB_EMULATOR_HOST is not set; skipping emulator reliability " +
				"test",
			_isEmulatorEnabled());

		_configurableApplicationContext = SpringApplication.run(
			EmulatorTestApplication.class,
			"--smoke.pubsub.namespace=" + _NAMESPACE,
			"--smoke.pubsub.project.id=" + _PROJECT_ID,
			"--spring.main.banner-mode=off",
			"--logging.level.com.liferay.osb.spring.boot.client.pubsub=info");
	}

	@AfterClass
	public static void tearDownClass() {
		if (_configurableApplicationContext != null) {
			_configurableApplicationContext.close();
		}
	}

	@Test
	public void testNackedMessageIsRedelivered() throws Exception {
		AccountCreatedPublisher accountCreatedPublisher =
			_configurableApplicationContext.getBean(
				AccountCreatedPublisher.class);

		RedeliveryPubsubSubscriber redeliveryPubsubSubscriber =
			_configurableApplicationContext.getBean(
				RedeliveryPubsubSubscriber.class);

		accountCreatedPublisher.publish(
			new Message(
				Collections.emptyMap(), "{\"event\":\"redelivery\"}",
				_REDELIVERY_TOPIC));

		String processedPayload =
			redeliveryPubsubSubscriber.getProcessedPayloads(
			).poll(
				30, TimeUnit.SECONDS
			);

		Assert.assertEquals("{\"event\":\"redelivery\"}", processedPayload);

		Assert.assertTrue(
			"Expected at least two delivery attempts",
			redeliveryPubsubSubscriber.getAttemptCount() >= 2);
	}

	@Test
	public void testSubscriptionHasDeadLetterPolicy() throws Exception {
		Subscription subscription = _getSubscription(
			_NAMESPACE + DeadLetterPubsubSubscriber.class.getName());

		Assert.assertEquals(30, subscription.getAckDeadlineSeconds());

		DeadLetterPolicy deadLetterPolicy = subscription.getDeadLetterPolicy();

		Assert.assertEquals(5, deadLetterPolicy.getMaxDeliveryAttempts());
		Assert.assertTrue(
			deadLetterPolicy.getDeadLetterTopic(
			).endsWith(
				_NAMESPACE + "dead-letter"
			));
	}

	private static boolean _isEmulatorEnabled() {
		String emulatorHost = System.getenv("PUBSUB_EMULATOR_HOST");

		if ((emulatorHost != null) && !emulatorHost.isEmpty()) {
			return true;
		}

		return false;
	}

	private Subscription _getSubscription(String subscriptionName)
		throws Exception {

		ManagedChannel managedChannel = ManagedChannelBuilder.forTarget(
			System.getenv("PUBSUB_EMULATOR_HOST")
		).usePlaintext(
		).build();

		try {
			SubscriptionAdminSettings subscriptionAdminSettings =
				SubscriptionAdminSettings.newBuilder(
				).setCredentialsProvider(
					NoCredentialsProvider.create()
				).setTransportChannelProvider(
					FixedTransportChannelProvider.create(
						GrpcTransportChannel.create(managedChannel))
				).build();

			try (SubscriptionAdminClient subscriptionAdminClient =
					SubscriptionAdminClient.create(subscriptionAdminSettings)) {

				return subscriptionAdminClient.getSubscription(
					ProjectSubscriptionName.of(_PROJECT_ID, subscriptionName));
			}
		}
		finally {
			managedChannel.shutdownNow();
		}
	}

	private static final String _NAMESPACE = "smoke-ns-";

	private static final String _PROJECT_ID = "emulator-reliability";

	private static final String _REDELIVERY_TOPIC = "redelivery";

	private static ConfigurableApplicationContext
		_configurableApplicationContext;

}