/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ClayTooltipProvider} from '@clayui/tooltip';
import i18n from '~/utils/I18n';
import getKebabCase from '~/utils/getKebabCase';
import RolesDropdown from './components/RolesDropdown';
import {ROLE_TYPES} from '~/utils/constants';

const partnerMemberRoles = [
	ROLE_TYPES.partnerDeliveryTechnical?.key,
	ROLE_TYPES.partnerMarketing?.key,
	ROLE_TYPES.partnerNewUser?.key,
	ROLE_TYPES.partnerPreSalesTechnical?.key,
	ROLE_TYPES.partnerPrimary?.key,
	ROLE_TYPES.partnerSales?.key,
	ROLE_TYPES.partnerSalesAndMarketing?.key,
].filter(Boolean);

const RolesColumn = ({
	accountRoles,
	availableSupportSeatsCount,
	category,
	currentRoleBriefName,
	edit,
	hasAccountSupportSeatRole,
	onClick,
	supportSeatsCount,
	selectedAccountRoleItems,
	setSelectedAccountRoleItems
}) => {
	const isPartnerRole = (roleName) => partnerMemberRoles.includes(roleName) || roleName.includes('Partner');

	const filteredRoles = currentRoleBriefName.filter((roleName) => {
		if (category === 'partner') {
			return isPartnerRole(roleName);
		}
		return !isPartnerRole(roleName);
	});

	if (filteredRoles.length === 0) {
		filteredRoles.push('None');
	}

	const roleProductNames = filteredRoles
		.map((roleBriefName) => {
			if (roleBriefName === 'None') return i18n.translate('none') || 'None';
			return i18n.translate(getKebabCase(roleBriefName));
		})
		.join(', ');

	return edit ? (
		<RolesDropdown
			accountRoles={accountRoles}
			availableSupportSeatsCount={availableSupportSeatsCount}
			category={category}
			currentRoleBriefName={filteredRoles}
			hasAccountSupportSeatRole={hasAccountSupportSeatRole}
			onClick={onClick}
			supportSeatsCount={supportSeatsCount}
			selectedAccountRoleItems={selectedAccountRoleItems}
			setSelectedAccountRoleItems={setSelectedAccountRoleItems}
		/>
	) : (
		<div className="d-flex">
			<ClayTooltipProvider delay={100}>
				<p className="m-0 pt-1 text-truncate" title={roleProductNames}>
					{roleProductNames}
				</p>
			</ClayTooltipProvider>
		</div>
	);
};

export default RolesColumn;
