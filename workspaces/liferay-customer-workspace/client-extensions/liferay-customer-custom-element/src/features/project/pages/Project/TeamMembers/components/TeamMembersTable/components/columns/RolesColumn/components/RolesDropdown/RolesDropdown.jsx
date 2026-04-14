/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */
import {useEffect, useState, useMemo} from 'react';
import {Button} from '@clayui/core';
import DropDown from '@clayui/drop-down';
import ClayIcon from '@clayui/icon';
import {ROLE_TYPES} from '~/utils/constants';
import i18n from '~/utils/I18n';
import getKebabCase from '~/utils/getKebabCase';
import isSupportSeatRole from '~/utils/isSupportSeatRole';

const partnerMemberRoles = [
	ROLE_TYPES.partnerDeliveryTechnical?.key,
	ROLE_TYPES.partnerMarketing?.key,
	ROLE_TYPES.partnerNewUser?.key,
	ROLE_TYPES.partnerPreSalesTechnical?.key,
	ROLE_TYPES.partnerPrimary?.key,
	ROLE_TYPES.partnerSales?.key,
	ROLE_TYPES.partnerSalesAndMarketing?.key,
].filter(Boolean);

const supportMemberRoles = [
	ROLE_TYPES.admin?.key,
	ROLE_TYPES.requester?.key,
	ROLE_TYPES.member?.key,
].filter(Boolean);

const RolesDropdown = ({
	accountRoles,
	availableSupportSeatsCount,
	category,
	currentRoleBriefName,
	hasAccountSupportSeatRole,
	supportSeatsCount,
	selectedAccountRoleItems,
	setSelectedAccountRoleItems
}) => {
	const [active, setActive] = useState(false);

	const isPartnerRole = (roleName) => partnerMemberRoles.includes(roleName) || roleName.includes('Partner');

	const currentSelectedRoleObj = selectedAccountRoleItems?.find(role => {
		if (category === 'partner') return isPartnerRole(role.label || role.name);
		return supportMemberRoles.includes(role.label || role.name);
	});

	const selectedRole = currentSelectedRoleObj ? (currentSelectedRoleObj.label || currentSelectedRoleObj.name) : 'None';

	const options = useMemo(() => {
		const filteredRoles = accountRoles.filter((accountRole) => {
			if (category === 'partner') {
				return isPartnerRole(accountRole.name);
			}
			return supportMemberRoles.includes(accountRole.name);
		});

		const optionList = filteredRoles.map((role) => {
			const isSupportRole = isSupportSeatRole(role.name);
			const noSeatsAvailable = availableSupportSeatsCount === 0;
			const isAlreadySelected = currentRoleBriefName.includes(role.name);
			let disabled = false;

			if (category === 'support' && isSupportRole && !isAlreadySelected && noSeatsAvailable) {
				disabled = true;
			}
			if (category === 'support' && hasAccountSupportSeatRole && supportSeatsCount === 1 && !isAlreadySelected) {
				disabled = true;
			}

			return {
				disabled,
				label: role.name,
				value: role,
			};
		});

		optionList.unshift({
			disabled: false,
			label: 'None',
			value: null,
		});

		return optionList;
	}, [accountRoles, category, availableSupportSeatsCount, currentRoleBriefName, hasAccountSupportSeatRole, supportSeatsCount]);

	const handleSelection = (option) => {
		setActive(false);
		
		if (setSelectedAccountRoleItems) {
			setSelectedAccountRoleItems(prev => {
				const currentList = prev || [];
				const otherRoles = currentList.filter(role => {
					if (category === 'partner') return supportMemberRoles.includes(role.label || role.name);
					return isPartnerRole(role.label || role.name);
				});

				if (option.value === null) {
					return otherRoles;
				}

				return [...otherRoles, { label: option.label, value: option.value.id, raysourceName: option.value.raysourceName }];
			});
		}
	};

	const displayRoleName = selectedRole === 'None' ? i18n.translate('none') : i18n.translate(getKebabCase(selectedRole)) || selectedRole;

	return (
		<DropDown
			active={active}
			closeOnClickOutside
			menuWidth="shrink"
			onActiveChange={setActive}
			trigger={
				<Button
					aria-label={displayRoleName}
					className="align-items-center bg-white d-flex justify-content-between w-100"
					displayType="secondary"
					outline
					small
				>
					<div className="text-truncate">
						{displayRoleName}
					</div>
					<span className="inline-item inline-item-after mt-1 ml-2">
						<ClayIcon symbol="caret-bottom" />
					</span>
				</Button>
			}
		>
			<DropDown.ItemList>
				{category === 'support' && typeof supportSeatsCount === 'number' && (
					<div className="bg-primary-l3 px-3 py-2 mx-3 mt-2 mb-2 d-flex align-items-center text-primary font-weight-semi-bold rounded">
						<ClayIcon className="mr-2" symbol="info-circle" />
						{i18n.sub('x-of-x-available', [availableSupportSeatsCount, supportSeatsCount])}
					</div>
				)}
				{options.map((option, index) => {
					const isSelected = selectedRole === option.label;
					const optionLabel = option.label === 'None' ? i18n.translate('none') : i18n.translate(getKebabCase(option.label)) || option.label;

					return (
						<DropDown.Item
							active={isSelected}
							disabled={option.disabled}
							key={index}
							onClick={() => handleSelection(option)}
							className="d-flex justify-content-between align-items-center"
						>
							{optionLabel}
							{isSelected && (
								<ClayIcon className="ml-2" symbol="check" />
							)}
						</DropDown.Item>
					);
				})}
			</DropDown.ItemList>
		</DropDown>
	);
};

export default RolesDropdown;