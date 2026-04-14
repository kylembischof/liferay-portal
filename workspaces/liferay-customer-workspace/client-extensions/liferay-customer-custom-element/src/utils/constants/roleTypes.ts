/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import i18n from '~/utils/I18n';

export const ROLE_TYPES = {
	admin: {
		key: 'Administrator',
		name: i18n.translate('administrator'),
		raysourceName: 'Support Administrator',
	},
	member: {
		key: 'User',
		name: i18n.translate('user'),
		raysourceName: 'Support User',
	},
	partnerDeliveryTechnical: {
		key: 'Partner Delivery Technical',
		name: i18n.translate('partner-delivery-technical'),
		raysourceName: 'Partner Delivery Technical',
	},
	partnerManager: {
		key: 'Partner Primary',
		name: i18n.translate('partner-primary'),
		raysourceName: 'Partner Primary',
	},
	partnerMarketingUser: {
		key: 'Partner Marketing',
		name: i18n.translate('partner-marketing'),
		raysourceName: 'Partner Marketing',
	},
	partnerMember: {
		key: 'Partner New User',
		name: i18n.translate('partner-new-user'),
		raysourceName: 'Partner New User',
	},
	partnerSalesAndMarketing: {
		key: 'Partner Sales & Marketing',
		name: i18n.translate('partner-sales-and-marketing'),
		raysourceName: 'Partner Sales & Marketing',
	},
	partnerSalesUser: {
		key: 'Partner Sales',
		name: i18n.translate('partner-sales'),
		raysourceName: 'Partner Sales',
	},
	partnerTechnicalUser: {
		key: 'Partner Pre-Sales Technical',
		name: i18n.translate('partner-pre-sales-technical'),
		raysourceName: 'Partner Pre-Sales Technical',
	},
	requester: {
		key: 'Requester',
		name: i18n.translate('requester'),
		raysourceName: 'Support Requester',
	},
};
