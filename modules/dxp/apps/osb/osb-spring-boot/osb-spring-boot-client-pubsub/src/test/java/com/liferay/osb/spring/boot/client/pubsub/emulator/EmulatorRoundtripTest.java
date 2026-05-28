/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.osb.spring.boot.client.pubsub.emulator;

import com.liferay.osb.spring.boot.client.pubsub.Message;
import com.liferay.osb.spring.boot.client.pubsub.smoke.Account;
import com.liferay.osb.spring.boot.client.pubsub.smoke.AccountCreatedPublisher;
import com.liferay.osb.spring.boot.client.pubsub.smoke.AccountRegistry;

import java.util.Collections;

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
public class EmulatorRoundtripTest {

	@BeforeClass
	public static void setUpClass() throws Exception {
		Assume.assumeTrue(
			"PUBSUB_EMULATOR_HOST is not set; skipping emulator roundtrip test",
			_isEmulatorEnabled());

		_configurableApplicationContext = SpringApplication.run(
			EmulatorTestApplication.class,
			"--smoke.pubsub.namespace=" + _NAMESPACE,
			"--smoke.pubsub.project.id=" + _PROJECT_ID,
			"--smoke.pubsub.account.created.topic=" + _TOPIC,
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
	public void testInvalidPayloadIsDropped() throws Exception {
		AccountCreatedPublisher accountCreatedPublisher =
			_configurableApplicationContext.getBean(
				AccountCreatedPublisher.class);

		AccountRegistry accountRegistry =
			_configurableApplicationContext.getBean(AccountRegistry.class);

		int registrySizeBeforeInvalid = accountRegistry.getAccounts(
		).size();

		Message invalidMessage = new Message(
			Collections.emptyMap(), "{\"accountKey\":\"\",\"appId\":\"\"}",
			_TOPIC);

		accountCreatedPublisher.publish(invalidMessage);

		Thread.sleep(3000);

		Assert.assertEquals(
			"Registry must not grow from an invalid payload",
			registrySizeBeforeInvalid,
			accountRegistry.getAccounts(
			).size());
	}

	@Test
	public void testRoundtripParsesAndRegistersAccount() throws Exception {
		AccountCreatedPublisher accountCreatedPublisher =
			_configurableApplicationContext.getBean(
				AccountCreatedPublisher.class);

		AccountRegistry accountRegistry =
			_configurableApplicationContext.getBean(AccountRegistry.class);

		Account publishedAccount = new Account("ACC-42", "APP-7");

		accountCreatedPublisher.publishAccountCreated(publishedAccount);

		Account receivedAccount = null;

		long deadline = System.currentTimeMillis() + 20000;

		while (System.currentTimeMillis() < deadline) {
			receivedAccount = accountRegistry.getAccount("ACC-42");

			if (receivedAccount != null) {
				break;
			}

			Thread.sleep(200);
		}

		Assert.assertNotNull(
			"Did not receive account-created message within 20 seconds",
			receivedAccount);
		Assert.assertEquals("ACC-42", receivedAccount.getAccountKey());
		Assert.assertEquals("APP-7", receivedAccount.getAppId());
	}

	private static boolean _isEmulatorEnabled() {
		String emulatorHost = System.getenv("PUBSUB_EMULATOR_HOST");

		if ((emulatorHost != null) && !emulatorHost.isEmpty()) {
			return true;
		}

		return false;
	}

	private static final String _NAMESPACE = "smoke-ns-";

	private static final String _PROJECT_ID = "emulator-account-created";

	private static final String _TOPIC = "account-created";

	private static ConfigurableApplicationContext
		_configurableApplicationContext;

}