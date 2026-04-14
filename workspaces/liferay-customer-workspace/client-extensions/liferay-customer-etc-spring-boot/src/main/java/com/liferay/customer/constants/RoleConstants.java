/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.customer.constants;

/**
 * @author Felipe Franca
 */
public class RoleConstants {

	public static final String NAME_ACCOUNT_ADMINISTRATOR =
		"Account Administrator";

	public static final String NAME_ACCOUNT_MEMBER = "Account Member";

	public static final String NAME_ADMINISTRATOR = "Administrator";

	public static final String NAME_LIFERAY_STAFF = "Liferay Staff";

	public static final String NAME_PARTNER = "Partner";

	public static final String NAME_PARTNER_DELIVERY_TECHNICAL =
		"Partner Delivery Technical";

	public static final String NAME_PARTNER_MARKETING = "Partner Marketing";

	public static final String NAME_PARTNER_NEW_USER = "Partner New User";

	public static final String NAME_PARTNER_PRE_SALES_TECHNICAL =
		"Partner Pre-Sales Technical";

	public static final String NAME_PARTNER_PRIMARY = "Partner Primary";

	public static final String NAME_PARTNER_SALES = "Partner Sales";

	public static final String NAME_PARTNER_SALES_AND_MARKETING =
		"Partner Sales & Marketing";

	public static final String NAME_PROVISIONING_MEMBER = "Provisioning Member";

	public static final String NAME_REQUESTER = "Requester";

	public static final String[] PARTNER_ACCOUNT_ROLES = {
		NAME_PARTNER_DELIVERY_TECHNICAL, NAME_PARTNER_MARKETING,
		NAME_PARTNER_NEW_USER, NAME_PARTNER_PRE_SALES_TECHNICAL,
		NAME_PARTNER_PRIMARY, NAME_PARTNER_SALES,
		NAME_PARTNER_SALES_AND_MARKETING
	};

	public static final String[] SUPPORT_ACCOUNT_ROLES = {
		NAME_ACCOUNT_ADMINISTRATOR, NAME_ACCOUNT_MEMBER, NAME_REQUESTER
	};

	public static final String[] SUPPORT_ACCOUNT_TICKET_ROLES = {
		NAME_ACCOUNT_ADMINISTRATOR, NAME_REQUESTER
	};

}