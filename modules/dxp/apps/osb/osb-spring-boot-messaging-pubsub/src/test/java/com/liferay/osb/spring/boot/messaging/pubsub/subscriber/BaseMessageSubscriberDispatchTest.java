/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.osb.spring.boot.messaging.pubsub.subscriber;

import com.liferay.osb.spring.boot.messaging.pubsub.Message;
import com.liferay.petra.string.StringBundler;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;

import org.json.JSONObject;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Kyle Bischof
 */
public class BaseMessageSubscriberDispatchTest {

	@Test
	public void testDoParseCalledOnceForJSONObject() {
		String id = RandomStringUtils.randomAlphanumeric(12);

		RecordingSubscriber subscriber = new RecordingSubscriber();

		Message message = new Message("{\"id\":\"" + id + "\"}");

		subscriber.receive(message);

		Assert.assertEquals(1, subscriber.getParseCount());
		Assert.assertEquals(
			id,
			subscriber.getParsedObjects(
			).get(
				0
			));
	}

	@Test
	public void testDoParseCalledOncePerArrayElement() {
		String id1 = RandomStringUtils.randomAlphanumeric(12);
		String id2 = RandomStringUtils.randomAlphanumeric(12);

		RecordingSubscriber subscriber = new RecordingSubscriber();

		Message message = new Message(
			StringBundler.concat(
				"[{\"id\":\"", id1, "\"},{\"id\":\"", id2, "\"}]"));

		subscriber.receive(message);

		Assert.assertEquals(2, subscriber.getParseCount());
		Assert.assertEquals(
			id1,
			subscriber.getParsedObjects(
			).get(
				0
			));
		Assert.assertEquals(
			id2,
			subscriber.getParsedObjects(
			).get(
				1
			));
	}

	@Test
	public void testHandleErrorReceivesParseException() {
		String id = RandomStringUtils.randomAlphanumeric(12);
		String topic = "topic-" + RandomStringUtils.randomAlphabetic(8);

		RecordingSubscriber subscriber = new RecordingSubscriber();

		subscriber.setThrowOnParse(true);

		String payload = "{\"id\":\"" + id + "\"}";

		Message message = new Message(payload);

		message.setDestinationName(topic);

		subscriber.receive(message);

		Assert.assertEquals(1, subscriber.getHandleErrorCount());
		Assert.assertEquals(topic, subscriber.getLastRoutingKey());
		Assert.assertEquals(payload, subscriber.getLastErrorPayload());
	}

	@Test
	public void testIsParseMessageFalseSkipsDoParse() {
		String id = RandomStringUtils.randomAlphanumeric(12);

		RecordingSubscriber subscriber = new RecordingSubscriber();

		subscriber.setSkipParse(true);

		Message message = new Message("{\"id\":\"" + id + "\"}");

		subscriber.receive(message);

		Assert.assertEquals(0, subscriber.getParseCount());
		Assert.assertEquals(1, subscriber.getPostParseCount());
	}

	@Test
	public void testPostParseFiresAfterParseException() {
		String id = RandomStringUtils.randomAlphanumeric(12);

		RecordingSubscriber subscriber = new RecordingSubscriber();

		subscriber.setThrowOnParse(true);

		Message message = new Message("{\"id\":\"" + id + "\"}");

		subscriber.receive(message);

		Assert.assertEquals(1, subscriber.getPostParseCount());
	}

	@Test
	public void testPostParseFiresAfterSuccess() {
		String id = RandomStringUtils.randomAlphanumeric(12);

		RecordingSubscriber subscriber = new RecordingSubscriber();

		Message message = new Message("{\"id\":\"" + id + "\"}");

		subscriber.receive(message);

		Assert.assertEquals(1, subscriber.getPostParseCount());
	}

	private static class RecordingSubscriber extends BaseMessageSubscriber {

		public int getHandleErrorCount() {
			return _handleErrorCount;
		}

		public String getLastErrorPayload() {
			return _lastErrorPayload;
		}

		public String getLastRoutingKey() {
			return _lastRoutingKey;
		}

		public int getParseCount() {
			return _parseCount;
		}

		public List<String> getParsedObjects() {
			return _parsedObjects;
		}

		public int getPostParseCount() {
			return _postParseCount;
		}

		public void setSkipParse(boolean skipParse) {
			_skipParse = skipParse;
		}

		public void setThrowOnParse(boolean throwOnParse) {
			_throwOnParse = throwOnParse;
		}

		@Override
		protected void doParse(JSONObject jsonObject) throws Exception {
			if (_throwOnParse) {
				throw new Exception("intentional");
			}

			_parseCount++;
			_parsedObjects.add(jsonObject.getString("id"));
		}

		@Override
		protected void handleError(
			String routingKey, String message, Exception[] exceptions) {

			_handleErrorCount++;
			_lastRoutingKey = routingKey;
			_lastErrorPayload = message;
		}

		@Override
		protected boolean isParseMessage(Message message) {
			return !_skipParse;
		}

		@Override
		protected void postParseMessage(Message message) {
			_postParseCount++;
		}

		private int _handleErrorCount;
		private String _lastErrorPayload;
		private String _lastRoutingKey;
		private int _parseCount;
		private final List<String> _parsedObjects = new ArrayList<>();
		private int _postParseCount;
		private boolean _skipParse;
		private boolean _throwOnParse;

	}

}