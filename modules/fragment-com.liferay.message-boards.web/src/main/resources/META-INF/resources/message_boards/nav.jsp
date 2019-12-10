<%--
/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of the Liferay Enterprise
 * Subscription License ("License"). You may not use this file except in
 * compliance with the License. You can obtain a copy of the License by
 * contacting Liferay, Inc. See the License for the specific language governing
 * permissions and limitations under the License, including but not limited to
 * distribution rights of the Software.
 *
 *
 *
 */
--%>

<%@ include file="/message_boards/init.jsp" %>

<%
String mvcRenderCommandName = ParamUtil.getString(request, "mvcRenderCommandName", "/message_boards/view");

boolean signedIn = themeDisplay.isSignedIn();
String recentQuestionLabel= "Recent Questions";
String allQuestionsLabel = "All Questions";

%>

<clay:navigation-bar
	navigationItems='<%=
		new JSPNavigationItemList(pageContext) {
			{
				PortletURL messageBoardsHomeURL = renderResponse.createRenderURL();

				messageBoardsHomeURL.setParameter("mvcRenderCommandName", "/message_boards/view");
				messageBoardsHomeURL.setParameter("tag", StringPool.BLANK);

				add(
					navigationItem -> {
						navigationItem.setActive(mvcRenderCommandName.equals("/message_boards/edit_category") || mvcRenderCommandName.equals("/message_boards/edit_message") || mvcRenderCommandName.equals("/message_boards/view") || mvcRenderCommandName.equals("/message_boards/view_category") || mvcRenderCommandName.equals("/message_boards/view_message"));
						navigationItem.setHref(messageBoardsHomeURL);
						navigationItem.setLabel(allQuestionsLabel);
					});

				PortletURL viewRecentPostsURL = renderResponse.createRenderURL();

				viewRecentPostsURL.setParameter("mvcRenderCommandName", "/message_boards/view_recent_posts");

				add(
					navigationItem -> {
						navigationItem.setActive(mvcRenderCommandName.equals("/message_boards/view_recent_posts"));
						navigationItem.setHref(viewRecentPostsURL);
						navigationItem.setLabel(recentQuestionLabel);
					});

				if (signedIn) {
					PortletURL viewMyPostsURL = renderResponse.createRenderURL();

					viewMyPostsURL.setParameter("mvcRenderCommandName", "/message_boards/view_my_posts");

					add(
						navigationItem -> {
							navigationItem.setActive(mvcRenderCommandName.equals("/message_boards/view_my_posts"));
							navigationItem.setHref(viewMyPostsURL);
							navigationItem.setLabel(LanguageUtil.get(request, "my-posts"));
						});

					if (mbGroupServiceSettings.isEmailMessageAddedEnabled() || mbGroupServiceSettings.isEmailMessageUpdatedEnabled()) {
						PortletURL viewMySubscriptionsURL = renderResponse.createRenderURL();

						viewMySubscriptionsURL.setParameter("mvcRenderCommandName", "/message_boards/view_my_subscriptions");

						add(
							navigationItem -> {
								navigationItem.setActive(mvcRenderCommandName.equals("/message_boards/view_my_subscriptions"));
								navigationItem.setHref(viewMySubscriptionsURL);
								navigationItem.setLabel(LanguageUtil.get(request, "my-subscriptions"));
							});
					}
				}

				PortletURL viewStatisticsURL = renderResponse.createRenderURL();

				viewStatisticsURL.setParameter("mvcRenderCommandName", "/message_boards/view_statistics");

				add(
					navigationItem -> {
						navigationItem.setActive(mvcRenderCommandName.equals("/message_boards/view_statistics"));
						navigationItem.setHref(viewStatisticsURL);
						navigationItem.setLabel(LanguageUtil.get(request, "statistics"));
					});
			}
		}
	%>'
/>

<div class="lfr-alert-container"></div>