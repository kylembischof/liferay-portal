/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ClayInput, ClaySelect} from '@clayui/form';
import ClayIcon from '@clayui/icon';
import {useMemo} from 'react';
import {useAppPropertiesContext} from '~/contexts/AppPropertiesContext';
import useCurrentKoroneikiAccount from '~/hooks/useCurrentKoroneikiAccount';
import useProvisioningLicenseKeys from '~/hooks/useProvisioningLicenseKeys';
import useUserAccountsByAccountExternalReferenceCode from '~/features/project/pages/Project/TeamMembers/components/TeamMembersTable/hooks/useUserAccountsByAccountExternalReferenceCode';
import i18n from '~/utils/I18n';
import {Input} from '~/components';
import useBannedDomains from '~/hooks/useBannedDomains';
import {ROLE_TYPES} from '~/utils/constants';
import {liferayDomains} from '~/utils/constants/liferayDomains';
import {
	isLiferayDomain,
	isValidEmail,
} from '~/utils/validations.form';

const FETCH_DELAY_AFTER_TYPING = 500;

const TeamMemberInputs = ({
	administratorsAssetsAvailable,
	disableError,
	errors,
	id,
	invite,
	options,
	placeholderEmail,
	projectHasPrioritySLA,
	isPartnerProject,
	setFieldValue,
}) => {
	const {accountSettingsURL} = useAppPropertiesContext();
	const provisioningService = useProvisioningLicenseKeys();

	const bannedDomains = useBannedDomains(
		invite?.email,
		FETCH_DELAY_AFTER_TYPING
	);

	const {data} = useCurrentKoroneikiAccount();
	const koroneikiAccount = data?.koroneikiAccountByExternalReferenceCode;

	const [
		,
		{data: userAccountsData},
	] = useUserAccountsByAccountExternalReferenceCode(
		koroneikiAccount?.accountKey
	);

	const currentDomain = userAccountsData?.accountUserAccountsByExternalReferenceCode.items
		.map(({emailAddress}) => emailAddress.split('@')[1])
		.flat();

	const [, domain] = invite?.email.split('@');

	const mathEmail = currentDomain?.includes(domain) || false;

	const isEmailValid = !!errors.invites?.[id]?.email;

	const warningMessage =
		invite?.email.length > 1 && !mathEmail && !isEmailValid;

	const validateEmail = useMemo(async () => {
		if (isValidEmail(invite?.email, bannedDomains)) {
			return isValidEmail(invite?.email, bannedDomains);
		}

		const hasLiferayDomain = liferayDomains.includes(domain);

		if (hasLiferayDomain) {
			const emailExistsInOkta = await provisioningService.getUserInOkta(
				invite?.email
			);

			if (!emailExistsInOkta) {
				return isLiferayDomain(invite?.email);
			}

			return false;
		}
	}, [bannedDomains, invite?.email, provisioningService]);

	const isAdministratorOrRequestorRoleSelected =
		invite?.role?.some(role => 
			role.name === ROLE_TYPES.requester.name ||
			role.name === ROLE_TYPES.admin.name
		);

	const optionsFormatted = useMemo(
		() =>
			options.map((option) => {
				const isAdministratorOrRequestorRole =
					option.label === ROLE_TYPES.requester.name ||
					option.label === ROLE_TYPES.admin.name;

				return {
					...option,
					disabled:
						administratorsAssetsAvailable !== -1 &&
						administratorsAssetsAvailable === 0 &&
						isAdministratorOrRequestorRole &&
						!isAdministratorOrRequestorRoleSelected,
				};
			}),
		[
			administratorsAssetsAvailable,
			isAdministratorOrRequestorRoleSelected,
			options,
		]
	);

	const supportRoleOptions = useMemo(() => {
		return optionsFormatted.filter(opt => !opt.key?.startsWith('Partner'));
	}, [optionsFormatted]);

	const partnerRoleOptions = useMemo(() => {
		return optionsFormatted.filter(opt => opt.key?.startsWith('Partner'));
	}, [optionsFormatted]);

	const supportRoleId = useMemo(() => {
		return invite?.role?.find(r => !r.key?.startsWith('Partner'))?.id || '';
	}, [invite?.role]);

	const partnerRoleId = useMemo(() => {
		return invite?.role?.find(r => r.key?.startsWith('Partner'))?.id || '';
	}, [invite?.role]);

	const handleRoleChange = (type, value) => {
		const roleId = Number(value);
		
		let newSupportRole = invite?.role?.find(r => !r.key?.startsWith('Partner'));
		let newPartnerRole = invite?.role?.find(r => r.key?.startsWith('Partner'));

		if (type === 'support') {
			newSupportRole = roleId ? options.find(o => o.value === roleId)?.role : null;
		} else {
			newPartnerRole = roleId ? options.find(o => o.value === roleId)?.role : null;
		}

		const updatedRoles = [newSupportRole, newPartnerRole].filter(Boolean);
		setFieldValue(`invites[${id}].role`, updatedRoles);
	};

	return (
		<>
			<ClayInput.Group className="m-0">
				<ClayInput.GroupItem className="m-0">
					<Input
						disableError={id === 0 && disableError}
						groupStyle="m-0"
						label={i18n.translate('first-name')}
						name={`invites[${id}].givenName`}
						placeholder={i18n.translate('first-name')}
						required
						type="text"
					/>
				</ClayInput.GroupItem>

				<ClayInput.GroupItem className="m-0">
					<Input
						disableError={id === 0 && disableError}
						groupStyle="m-0"
						label={i18n.translate('last-name')}
						name={`invites[${id}].familyName`}
						placeholder={i18n.translate('last-name')}
						required
						type="text"
					/>
				</ClayInput.GroupItem>
			</ClayInput.Group>

			<ClayInput.Group className="m-0">
				<ClayInput.GroupItem className="m-0">
					<Input
						disableError={id === 0 && disableError}
						groupStyle="m-0"
						label={i18n.translate('email')}
						name={`invites[${id}].email`}
						placeholder={placeholderEmail}
						required
						type="email"
						validations={[() => validateEmail]}
					/>
				</ClayInput.GroupItem>

				<ClayInput.GroupItem className="m-0">
					<div className="mx-3 my-1 role-selector-container w-100 d-flex flex-column justify-content-center">
						{projectHasPrioritySLA && (
							<div className="mb-2">
								<span className="role-selector-label mb-1 d-block font-weight-semi-bold">
									{i18n.translate('support-role')}
								</span>
								<ClaySelect
									aria-label={i18n.translate('support-role')}
									onChange={(e) => handleRoleChange('support', e.target.value)}
									value={supportRoleId}
								>
									<ClaySelect.Option label={i18n.translate('none')} value="" />
									{supportRoleOptions.map((opt) => (
										<ClaySelect.Option
											disabled={opt.disabled}
											key={opt.value}
											label={opt.label}
											value={opt.value}
										/>
									))}
								</ClaySelect>
							</div>
						)}

						{isPartnerProject && (
							<div className="mb-2">
								<span className="role-selector-label mb-1 d-block font-weight-semi-bold">
									{i18n.translate('partner-role')}
								</span>
								<ClaySelect
									aria-label={i18n.translate('partner-role')}
									onChange={(e) => handleRoleChange('partner', e.target.value)}
									value={partnerRoleId}
								>
									<ClaySelect.Option label={i18n.translate('none')} value="" />
									{partnerRoleOptions.map((opt) => (
										<ClaySelect.Option
											disabled={opt.disabled}
											key={opt.value}
											label={opt.label}
											value={opt.value}
										/>
									))}
								</ClaySelect>
							</div>
						)}
					</div>
				</ClayInput.GroupItem>
			</ClayInput.Group>

			{warningMessage && (
				<div
					className="alert alert-warning align-items-top d-flex m-3 p-3"
					role="alert"
				>
					<div className="alert-indicator mt-1">
						<span>
							<ClayIcon symbol="warning-full" />
						</span>
					</div>

					<div className="mx-2">
						{`${i18n.translate('is')} `}

						<strong>{invite.email}</strong>

						{` ${i18n.sub(
							'part-of-your-organization-it-looks-like-x-is-a-new-domain-name',
							[`${domain}`]
						)}`}

						<ul className="mb-0">
							<li>
								{`${i18n.translate(
									'to-update-an-existing-users-email-address-have-the-user-log-in-with-their-current-address-to-access'
								)} `}

								<a
									className="alert-link"
									href={accountSettingsURL}
									rel="noreferrer noopener"
									target="_blank"
								>
									<u className="font-weight-semi-bold text-warning">
										{i18n.translate('account-settings')}
									</u>
								</a>
							</li>

							<li>
								{i18n.translate(
									'be-aware-that-adding-new-users-from-outside-your-organization-may-compromise-the-security-of-your-project'
								)}
							</li>
						</ul>
					</div>
				</div>
			)}

			<hr className="mb-3 mt-2" />
		</>
	);
};

export default TeamMemberInputs;
