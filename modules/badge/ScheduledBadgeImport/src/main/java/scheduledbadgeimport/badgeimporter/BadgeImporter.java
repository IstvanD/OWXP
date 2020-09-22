/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package scheduledbadgeimport.badgeimporter;

import com.liferay.counter.kernel.service.CounterLocalServiceUtil;
import com.liferay.grow.gamification.model.Badge;
import com.liferay.grow.gamification.model.BadgeType;
import com.liferay.grow.gamification.service.BadgeLocalServiceUtil;
import com.liferay.grow.gamification.service.BadgeTypeLocalServiceUtil;
import com.liferay.grow.gamification.service.LDateLocalServiceUtil;
import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.UserLocalServiceUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.SAXReaderUtil;

import java.io.InputStream;

import java.nio.charset.StandardCharsets;

import java.text.SimpleDateFormat;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import org.osgi.service.component.annotations.Component;

import scheduledbadgeimport.constants.BadgeImportKeys;

/**
 * @author István András Dézsi
 */
@Component(immediate = true, service = BadgeImporter.class)
public class BadgeImporter {

	public void importBadges() throws Exception {
		String authToken = _getAuthToken();

		Map<Integer, Integer> firstBadgeTypeMap = _getFirstBadgeTypeIdMap();

		Map<Integer, Integer> loyaltyBadgeTypeIdMap =
			_getLoyaltyBadgeTypeIdMap();

		_importBadges(
			loyaltyBadgeTypeIdMap, _INITIAL_LOYALTY, BadgeImportKeys.DRYRUN,
			authToken);

		_importBadges(
			loyaltyBadgeTypeIdMap, _LOYALTY, BadgeImportKeys.DRYRUN, authToken);

		_importBadges(
			firstBadgeTypeMap, _FIRST_ARTICLE, BadgeImportKeys.DRYRUN,
			authToken);

		_importBadges(
			firstBadgeTypeMap, _FIRST_CLOSED_TICKET, BadgeImportKeys.DRYRUN,
			authToken);
	}

	private String _getAuthToken() throws Exception {
		String authToken = StringPool.BLANK;

		String signinBody = _getFileContents("signin.xml");

		HttpPost signinRequest = new HttpPost(_SIGNIN_URL);

		StringEntity entity = new StringEntity(signinBody);

		signinRequest.setEntity(entity);

		try (CloseableHttpClient httpClient = HttpClients.createDefault();
			CloseableHttpResponse response = httpClient.execute(
				signinRequest)) {

			String result = EntityUtils.toString(response.getEntity());

			Document document = SAXReaderUtil.read(result);

			Element rootElement = document.getRootElement();

			Element credentialsElement = rootElement.element("credentials");

			authToken = credentialsElement.attributeValue("token");
		}

		return authToken;
	}

	private Date _getBadgeCreationDate(String dateString) {
		Date date = new Date();

		try {
			dateString = StringUtil.strip(dateString, CharPool.QUOTE);

			date = new SimpleDateFormat(
				"MMMM dd, yyyy"
			).parse(
				dateString
			);
		}
		catch (Exception e) {
			_log.error("Cannot determine date id for: " + dateString, e);
		}

		return date;
	}

	private String _getFileContents(String fileName) {
		Class<?> clazz = getClass();

		InputStream inputStream = clazz.getResourceAsStream(
			"dependencies/" + fileName);

		String contents = StringPool.BLANK;

		try (Scanner scanner = new Scanner(
				inputStream, StandardCharsets.UTF_8.name())) {

			contents = scanner.useDelimiter(
				"\\A"
			).next();
		}

		return contents;
	}

	private String _getFirstArticleBadgeDescription() {
		return "Congratulations, you have created your first grow article!";
	}

	private int _getFirstBadgeTypeId(
		int type, Map<Integer, Integer> badgeTypeMap) {

		return badgeTypeMap.get(type);
	}

	private Map<Integer, Integer> _getFirstBadgeTypeIdMap() {
		HashMap<Integer, Integer> firstArticleBadgeTypeMap = new HashMap<>();

		List<BadgeType> badgeTypes =
			BadgeTypeLocalServiceUtil.getAllBadgeTypes();

		for (BadgeType badgeType : badgeTypes) {
			String title = badgeType.getType();

			if (title.equalsIgnoreCase("1st GROW Article")) {
				firstArticleBadgeTypeMap.put(
					(Integer)_FIRST_ARTICLE,
					GetterUtil.getInteger(badgeType.getBadgeTypeId()));
			}
			else if (title.equalsIgnoreCase("1st Closed Ticket")) {
				firstArticleBadgeTypeMap.put(
					(Integer)_FIRST_CLOSED_TICKET,
					GetterUtil.getInteger(badgeType.getBadgeTypeId()));
			}
		}

		return firstArticleBadgeTypeMap;
	}

	private String _getFirstClosedTicketBadgeDescription() {
		return "Congratulations, you have completed your first ticket!";
	}

	private String _getLoyaltyBadgeDescription(String loyalty) {
		String description = StringPool.BLANK;

		int year = _getLoyaltyYear(loyalty);

		if (year > 0) {
			description =
				"You have been a member of the Liferay Family for more than " +
					year + " year";

			if (year > 1) {
				description += "s";
			}

			description += "!";
		}

		return description;
	}

	private int _getLoyaltyBadgeTypeId(
		String loyalty, Map<Integer, Integer> badgeTypeMap) {

		int year = _getLoyaltyYear(loyalty);

		return GetterUtil.getInteger(badgeTypeMap.get(year));
	}

	private Map<Integer, Integer> _getLoyaltyBadgeTypeIdMap() {
		HashMap<Integer, Integer> loyaltyBadgeTypeMap = new HashMap<>();

		List<BadgeType> badgeTypes =
			BadgeTypeLocalServiceUtil.getAllBadgeTypes();

		for (BadgeType badgeType : badgeTypes) {
			try {
				String title = badgeType.getType();

				title = title.toLowerCase();

				if (!title.contains("loyalty")) {
					continue;
				}

				Integer year = _getLoyaltyYear(title);

				if (year <= 0) {
					_log.error(
						"Cannot determine the year for loyalty badge type: " +
							badgeType.getType());

					continue;
				}

				loyaltyBadgeTypeMap.put(
					year, GetterUtil.getInteger(badgeType.getBadgeTypeId()));
			}
			catch (Exception e) {
				_log.error(e);
			}
		}

		return loyaltyBadgeTypeMap;
	}

	private Integer _getLoyaltyYear(String loyalty) {
		String[] loyaltyParts = StringUtil.split(loyalty, StringPool.SPACE);

		Integer year = 0;

		for (String loyaltyPart : loyaltyParts) {
			if (loyaltyPart.endsWith(StringPool.PLUS)) {
				loyaltyPart = loyaltyPart.substring(
					0, loyaltyPart.length() - 1);
			}

			if (GetterUtil.getInteger(loyaltyPart) > 0) {
				year = GetterUtil.getInteger(loyaltyPart);
			}

			break;
		}

		if (year <= 0) {
			_log.error("Cannot determine the year number: " + loyalty);
		}

		return year;
	}

	private String _getUserEmailAddress(String[] fields, int importType) {
		String userEmailAddress = StringPool.BLANK;

		if ((importType == _LOYALTY) || (importType == _FIRST_ARTICLE) ||
			(importType == _FIRST_CLOSED_TICKET)) {

			userEmailAddress = fields[1];
		}
		else if (importType == _INITIAL_LOYALTY) {
			userEmailAddress = fields[0];
		}

		return userEmailAddress;
	}

	private void _importBadges(
			Map<Integer, Integer> badgeTypeMap, int importType, boolean dryRun,
			String authToken)
		throws Exception {

		long companyId = BadgeImportKeys.COMPANY_ID;

		long fromUserId = BadgeImportKeys.FROM_USER_ID;

		String fromUserName = BadgeImportKeys.FROM_USER_NAME;

		Date createDate = new Date();
		Calendar cal = Calendar.getInstance();

		cal.setTime(createDate);

		long dateId = LDateLocalServiceUtil.getDateId(
			cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1,
			cal.get(Calendar.DAY_OF_MONTH));

		String badgeViewId = StringPool.BLANK;

		if (importType == _INITIAL_LOYALTY) {
			badgeViewId = BadgeImportKeys.INITIAL_LOYALTY_BADGE_VIEW_ID;
		}
		else if (importType == _LOYALTY) {
			badgeViewId = BadgeImportKeys.LOYALTY_BADGE_VIEW_ID;
		}
		else if (importType == _FIRST_ARTICLE) {
			badgeViewId = BadgeImportKeys.FIRST_ARTICLE_BADGE_VIEW_ID;
		}
		else if (importType == _FIRST_CLOSED_TICKET) {
			badgeViewId = BadgeImportKeys.FIRST_CLOSED_TICKET_BADGE_VIEW_ID;
		}

		String url = StringPool.BLANK;

		StringBundler sb = new StringBundler(5);

		sb.append("https://reports.liferay.com/api/3.9/sites/");
		sb.append(BadgeImportKeys.SUPPORT_SITE_ID);
		sb.append("/views/");
		sb.append(badgeViewId);
		sb.append("/data");

		url = sb.toString();

		HttpGet request = new HttpGet(url);

		request.addHeader("X-Tableau-Auth", authToken);

		try (CloseableHttpClient httpClient = HttpClients.createDefault();
			CloseableHttpResponse response = httpClient.execute(request)) {

			_log.debug(response.getProtocolVersion());
			_log.debug(
				response.getStatusLine(
				).getStatusCode());
			_log.debug(
				response.getStatusLine(
				).getReasonPhrase());
			_log.debug(
				response.getStatusLine(
				).toString());

			String result = EntityUtils.toString(response.getEntity());

			String[] lines = StringUtil.split(result, StringPool.NEW_LINE);

			boolean header = true;

			for (String line : lines) {
				if (header) {
					header = false;

					continue;
				}

				String[] fields = line.split(_SPLIT_PATTERN, -1);

				String userEmailAddress = _getUserEmailAddress(
					fields, importType);

				User user = UserLocalServiceUtil.fetchUserByEmailAddress(
					companyId, userEmailAddress);

				if (user == null) {
					_log.warn("Cannot find user for line: " + line);

					continue;
				}

				long badgeTypeId = 0;
				String description = StringPool.BLANK;

				if (importType == _INITIAL_LOYALTY) {
					badgeTypeId = _getLoyaltyBadgeTypeId(
						fields[1], badgeTypeMap);

					description = _getLoyaltyBadgeDescription(fields[1]);

					createDate = new Date();
				}
				else if (importType == _LOYALTY) {
					badgeTypeId = _getLoyaltyBadgeTypeId(
						fields[2], badgeTypeMap);

					description = _getLoyaltyBadgeDescription(fields[2]);

					createDate = _getBadgeCreationDate(fields[0]);
				}
				else if (importType == _FIRST_ARTICLE) {
					badgeTypeId = _getFirstBadgeTypeId(
						_FIRST_ARTICLE, badgeTypeMap);

					description = _getFirstArticleBadgeDescription();

					createDate = _getBadgeCreationDate(fields[1]);
				}
				else if (importType == _FIRST_CLOSED_TICKET) {
					badgeTypeId = _getFirstBadgeTypeId(
						_FIRST_CLOSED_TICKET, badgeTypeMap);

					description = _getFirstClosedTicketBadgeDescription();

					createDate = _getBadgeCreationDate(fields[0]);
				}

				BadgeType badgeType = BadgeTypeLocalServiceUtil.fetchBadgeType(
					badgeTypeId);

				if (badgeType == null) {
					_log.error("Cannot find badge type for line: " + line);

					continue;
				}

				if (!dryRun &&
					_isBadgeTypeProcessed(user.getUserId(), badgeTypeId)) {

					_log.info(
						"Skipping user " + user.getEmailAddress() +
							" because badge type " + badgeType.getType() +
								" was already processed");

					continue;
				}

				if (Validator.isNull(description)) {
					_log.warn(
						"Cannot determine description for line : " + line);
				}

				if (dryRun) {
					_log.info(
						"Dry run: " + badgeType.getType() + "badge for " +
							user.getEmailAddress());

					continue;
				}

				long badgeId = CounterLocalServiceUtil.increment(
					Badge.class.getName());

				Badge badge = BadgeLocalServiceUtil.createBadge(badgeId);

				badge.setUserId(fromUserId);
				badge.setAssignedDateId(dateId);
				badge.setBadgeTypeId(badgeTypeId);
				badge.setCompanyId(companyId);
				badge.setCreateDate(createDate);
				badge.setDescription(description);
				badge.setGroupId(user.getGroupId());
				badge.setToUserId(user.getUserId());
				badge.setUserName(fromUserName);
				badge.setUuid(String.valueOf(UUID.randomUUID()));

				BadgeLocalServiceUtil.addBadge(badge, false);

				_log.info(
					"Created " + badgeType.getType() + "badge for " +
						user.getEmailAddress());
			}
		}
	}

	private boolean _isBadgeTypeProcessed(long userId, long badgeTypeId) {
		List<Badge> userBadges = BadgeLocalServiceUtil.getBadgesOfUser(userId);

		for (Badge userBadge : userBadges) {
			if (userBadge.getBadgeTypeId() == badgeTypeId) {
				return true;
			}
		}

		return false;
	}

	private static final int _FIRST_ARTICLE = 3;

	private static final int _FIRST_CLOSED_TICKET = 4;

	private static final int _INITIAL_LOYALTY = 1;

	private static final int _LOYALTY = 2;

	private static final String _SIGNIN_URL =
		"https://reports.liferay.com/api/3.9/auth/signin";

	private static final String _SPLIT_PATTERN =
		",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)";

	private Log _log = LogFactoryUtil.getLog(BadgeImporter.class.getName());

}