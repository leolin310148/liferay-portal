/**
 * Copyright (c) 2000-2008 Liferay, Inc. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.liferay.util.portlet;

import com.liferay.portal.kernel.portlet.LiferayWindowState;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.util.xml.DocUtil;

import java.io.IOException;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;

import javax.portlet.ActionRequest;
import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;
import javax.portlet.PortletSession;
import javax.portlet.PortletURL;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.WindowStateException;

/**
 * <a href="PortletRequestUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author Brian Wing Shun Chan
 * @author Raymond Augé
 *
 */
public class PortletRequestUtil {

	public static String toXML(
		PortletRequest portletRequest, PortletResponse portletResponse) {

		String xml = null;

		Document doc = SAXReaderUtil.createDocument();

		Element reqEl = doc.addElement("request");

		DocUtil.add(reqEl, "container-type", "portlet");
		DocUtil.add(
			reqEl, "container-namespace", portletRequest.getContextPath());
		DocUtil.add(
			reqEl, "content-type", portletRequest.getResponseContentType());
		DocUtil.add(reqEl, "server-name", portletRequest.getServerName());
		DocUtil.add(reqEl, "server-port", portletRequest.getServerPort());
		DocUtil.add(reqEl, "secure", portletRequest.isSecure());
		DocUtil.add(reqEl, "auth-type", portletRequest.getAuthType());
		DocUtil.add(reqEl, "remote-user", portletRequest.getRemoteUser());
		DocUtil.add(reqEl, "context-path", portletRequest.getContextPath());
		DocUtil.add(reqEl, "locale", portletRequest.getLocale());
		DocUtil.add(reqEl, "portlet-mode", portletRequest.getPortletMode());
		DocUtil.add(
			reqEl, "portlet-session-id",
			portletRequest.getRequestedSessionId());
		DocUtil.add(reqEl, "scheme", portletRequest.getScheme());
		DocUtil.add(reqEl, "window-state", portletRequest.getWindowState());

		if (portletRequest instanceof RenderRequest) {
			DocUtil.add(reqEl, "action", Boolean.FALSE);
		}
		else if (portletRequest instanceof ActionRequest) {
			DocUtil.add(reqEl, "action", Boolean.TRUE);
		}

		if (portletResponse instanceof RenderResponse) {
			_renderResponseToXML((RenderResponse)portletResponse, reqEl);
		}

		ThemeDisplay themeDisplay = (ThemeDisplay)portletRequest.getAttribute(
			WebKeys.THEME_DISPLAY);

		if (themeDisplay != null) {
			Element themeDisplayEl = reqEl.addElement("theme-display");

			_themeDisplayToXML(themeDisplay, themeDisplayEl);
		}

		Element parametersEl = reqEl.addElement("parameters");

		Enumeration<String> enu = portletRequest.getParameterNames();

		while (enu.hasMoreElements()) {
			String name = enu.nextElement();

			Element parameterEl = parametersEl.addElement("parameter");

			DocUtil.add(parameterEl, "name", name);

			String[] values = portletRequest.getParameterValues(name);

			for (int i = 0; i < values.length; i++) {
				DocUtil.add(parameterEl, "value", values[i]);
			}
		}

		Element attributesEl = reqEl.addElement("attributes");

		enu = portletRequest.getAttributeNames();

		while (enu.hasMoreElements()) {
			String name = enu.nextElement();

			if (!_isValidAttributeName(name)) {
				continue;
			}

			Object value = portletRequest.getAttribute(name);

			if (!_isValidAttributeValue(value)) {
				continue;
			}

			Element attributeEl = attributesEl.addElement("attribute");

			DocUtil.add(attributeEl, "name", name);
			DocUtil.add(attributeEl, "value", String.valueOf(value));
		}

		Element portletSessionEl = reqEl.addElement("portlet-session");

		attributesEl = portletSessionEl.addElement("portlet-attributes");

		PortletSession portletSession = portletRequest.getPortletSession();

		enu = portletSession.getAttributeNames(PortletSession.PORTLET_SCOPE);

		while (enu.hasMoreElements()) {
			String name = enu.nextElement();

			if (!_isValidAttributeName(name)) {
				continue;
			}

			Object value = portletSession.getAttribute(
				name, PortletSession.PORTLET_SCOPE);

			if (!_isValidAttributeValue(value)) {
				continue;
			}

			Element attributeEl = attributesEl.addElement("attribute");

			DocUtil.add(attributeEl, "name", name);
			DocUtil.add(attributeEl, "value", String.valueOf(value));
		}

		attributesEl = portletSessionEl.addElement("application-attributes");

		enu = portletSession.getAttributeNames(
			PortletSession.APPLICATION_SCOPE);

		while (enu.hasMoreElements()) {
			String name = enu.nextElement();

			if (!_isValidAttributeName(name)) {
				continue;
			}

			Object value = portletSession.getAttribute(
				name, PortletSession.APPLICATION_SCOPE);

			if (!_isValidAttributeValue(value)) {
				continue;
			}

			Element attributeEl = attributesEl.addElement("attribute");

			DocUtil.add(attributeEl, "name", name);
			DocUtil.add(attributeEl, "value", String.valueOf(value));
		}

		try {
			xml = doc.formattedString();
		}
		catch (IOException ioe) {
		}

		return xml;
	}

	private static void _renderResponseToXML(
		RenderResponse renderResponse, Element reqEl) {

		DocUtil.add(reqEl, "portlet-namespace", renderResponse.getNamespace());

		PortletURL url = renderResponse.createRenderURL();

		DocUtil.add(reqEl, "render-url", url);

		try {
			url.setWindowState(LiferayWindowState.EXCLUSIVE);

			DocUtil.add(reqEl, "render-url-exclusive", url);
		}
		catch (WindowStateException wse) {
		}

		try {
			url.setWindowState(LiferayWindowState.MAXIMIZED);

			DocUtil.add(reqEl, "render-url-maximized", url);
		}
		catch (WindowStateException wse) {
		}

		try {
			url.setWindowState(LiferayWindowState.MINIMIZED);

			DocUtil.add(reqEl, "render-url-minimized", url);
		}
		catch (WindowStateException wse) {
		}

		try {
			url.setWindowState(LiferayWindowState.NORMAL);

			DocUtil.add(reqEl, "render-url-normal", url);
		}
		catch (WindowStateException wse) {
		}

		try {
			url.setWindowState(LiferayWindowState.POP_UP);

			DocUtil.add(reqEl, "render-url-pop-up", url);
		}
		catch (WindowStateException wse) {
		}
	}

	private static void _themeDisplayToXML(
		ThemeDisplay themeDisplay, Element themeDisplayEl) {

		DocUtil.add(themeDisplayEl, "cdn-host", themeDisplay.getCDNHost());
		DocUtil.add(themeDisplayEl, "company-id", themeDisplay.getCompanyId());
		DocUtil.add(
			themeDisplayEl, "path-context", themeDisplay.getPathContext());
		DocUtil.add(
			themeDisplayEl, "path-friendly-url-private-group",
			themeDisplay.getPathFriendlyURLPrivateGroup());
		DocUtil.add(
			themeDisplayEl, "path-friendly-url-private-user",
			themeDisplay.getPathFriendlyURLPrivateUser());
		DocUtil.add(
			themeDisplayEl, "path-friendly-url-public",
			themeDisplay.getPathFriendlyURLPublic());
		DocUtil.add(themeDisplayEl, "path-image", themeDisplay.getPathImage());
		DocUtil.add(themeDisplayEl, "path-main", themeDisplay.getPathMain());
		DocUtil.add(
			themeDisplayEl, "path-theme-images",
			themeDisplay.getPathThemeImages());
		DocUtil.add(themeDisplayEl, "plid", themeDisplay.getPlid());
		DocUtil.add(
			themeDisplayEl, "url-portal",
			HttpUtil.removeProtocol(themeDisplay.getURLPortal()));
	}

	private static boolean _isValidAttributeName(String name) {
		if (name.equalsIgnoreCase("j_password") ||
			name.equalsIgnoreCase("LAYOUT_CONTENT") ||
			name.equalsIgnoreCase("LAYOUTS") ||
			name.equalsIgnoreCase("PORTLET_RENDER_PARAMETERS") ||
			name.equalsIgnoreCase("USER_PASSWORD") ||
			name.startsWith("javax.") ||
			name.startsWith("liferay-ui:")) {

			return false;
		}
		else {
			return true;
		}
	}

	private static boolean _isValidAttributeValue(Object obj) {
		if (obj == null) {
			return false;
		}
		else if (obj instanceof Collection) {
			Collection<?> col = (Collection<?>)obj;

			if (col.size() == 0) {
				return false;
			}
			else {
				return true;
			}
		}
		else if (obj instanceof Map) {
			Map<?, ?> map = (Map<?, ?>)obj;

			if (map.size() == 0) {
				return false;
			}
			else {
				return true;
			}
		}
		else {
			String objString = String.valueOf(obj);

			if (Validator.isNull(objString)) {
				return false;
			}

			String hashCode =
				StringPool.AT + Integer.toHexString(obj.hashCode());

			if (objString.endsWith(hashCode)) {
				return false;
			}

			return true;
		}
	}

}