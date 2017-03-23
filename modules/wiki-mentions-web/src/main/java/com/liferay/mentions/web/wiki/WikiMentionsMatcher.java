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

package com.liferay.mentions.web.wiki;

import com.liferay.mentions.matcher.BaseRegularExpressionMentionsMatcher;
import com.liferay.mentions.matcher.MentionsMatcher;
import com.liferay.mentions.matcher.MentionsMatcherUtil;

import org.osgi.service.component.annotations.Component;

/**
 * @author Norbert Kocsis
 */
@Component(
	immediate = true,
	property = {
		"model.class.name=com.liferay.wiki.model.WikiPage",
		"service.ranking:Integer=" + Integer.MIN_VALUE
	},
	service = MentionsMatcher.class
)
public class WikiMentionsMatcher extends BaseRegularExpressionMentionsMatcher {

	@Override
	protected String getRegularExpression() {
		return String.format(
			_MENTIONS_REGULAR_EXPRESSION_TEMPLATE,
			MentionsMatcherUtil.getScreenNameRegularExpression());
	}

	private static final String _MENTIONS_REGULAR_EXPRESSION_TEMPLATE =
		"(?:\\s|^|\\]|>|\\|)(?:@|&#64;)(%s)(?=[<\\[\\s]|$)";

}