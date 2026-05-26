/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.osb.spring.boot.messaging.pubsub.subscriber;

import com.liferay.osb.spring.boot.messaging.pubsub.Message;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kyle Bischof
 */
public abstract class BaseMessageSubscriber implements MessageSubscriber {

	@Override
	public void receive(Message message) {
		try {
			if (!isParseMessage(message)) {
				if (_log.isDebugEnabled()) {
					_log.debug("Skip parsing message: " + message.getPayload());
				}

				return;
			}

			try {
				JSONObject jsonObject = new JSONObject(
					(String)message.getPayload());

				doParse(jsonObject);
			}
			catch (JSONException jsonException) {
				if (_log.isDebugEnabled()) {
					_log.debug(
						"Payload is not a JSON object, parsing as JSON array",
						jsonException);
				}

				JSONArray jsonArray = new JSONArray(
					(String)message.getPayload());

				for (int i = 0; i < jsonArray.length(); i++) {
					doParse(jsonArray.getJSONObject(i));
				}
			}
		}
		catch (Exception exception1) {
			try {
				handleError(
					message.getDestinationName(), (String)message.getPayload(),
					new Exception[] {exception1});
			}
			catch (Exception exception2) {
				_log.error(String.valueOf(message));
				_log.error(String.valueOf(exception2), exception2);
			}
		}
		finally {
			postParseMessage(message);
		}
	}

	protected abstract void doParse(JSONObject jsonObject) throws Exception;

	protected void handleError(
			String routingKey, String message, Exception[] exceptions)
		throws Exception {

		for (Exception exception : exceptions) {
			_log.error(message, exception);
		}
	}

	protected boolean isParseMessage(Message message) {
		return true;
	}

	protected void postParseMessage(Message message) {
	}

	private static final Logger _log = LoggerFactory.getLogger(
		BaseMessageSubscriber.class);

}