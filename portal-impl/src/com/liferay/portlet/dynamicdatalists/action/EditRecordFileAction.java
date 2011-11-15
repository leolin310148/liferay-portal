/**
 * Copyright (c) 2000-2011 Liferay, Inc. All rights reserved.
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

package com.liferay.portlet.dynamicdatalists.action;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.servlet.ServletResponseUtil;
import com.liferay.portal.kernel.upload.UploadPortletRequest;
import com.liferay.portal.kernel.util.Constants;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.ServiceContextFactory;
import com.liferay.portal.struts.PortletAction;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.documentlibrary.FileSizeException;
import com.liferay.portlet.dynamicdatalists.model.DDLRecord;
import com.liferay.portlet.dynamicdatalists.service.DDLRecordLocalServiceUtil;
import com.liferay.portlet.dynamicdatalists.util.DDLUtil;
import com.liferay.portlet.dynamicdatamapping.storage.Field;
import com.liferay.portlet.dynamicdatamapping.storage.Fields;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.PortletRequest;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

/**
 * @author Bruno Basto
 */
public class EditRecordFileAction extends PortletAction {

	@Override
	public void processAction(
			ActionMapping mapping, ActionForm form,
			PortletConfig portletConfig, ActionRequest actionRequest,
			ActionResponse actionResponse)
		throws Exception {

		String cmd = ParamUtil.getString(actionRequest, Constants.CMD);

		if (cmd.equals(Constants.DELETE)) {
			deleteRecordFieldFile(actionRequest);
		}

		sendRedirect(actionRequest, actionResponse);
	}

	@Override
	public void serveResource(
			ActionMapping mapping, ActionForm form,
			PortletConfig portletConfig,
			ResourceRequest resourceRequest, ResourceResponse resourceResponse)
		throws Exception {

		JSONObject jsonObject = JSONFactoryUtil.createJSONObject();

		try {
			jsonObject = updateRecordFieldFile(resourceRequest);
		}
		catch (Exception e) {
			if (e instanceof FileSizeException) {
				jsonObject.put("exception", e.toString());
			}
			else {
				throw e;
			}
		}

		HttpServletResponse response = PortalUtil.getHttpServletResponse(
			resourceResponse);

		response.setContentType(ContentTypes.TEXT_PLAIN);

		ServletResponseUtil.write(response, jsonObject.toString());
	}

	protected void deleteRecordFieldFile(PortletRequest request)
		throws PortalException, SystemException {

		long recordId = ParamUtil.getLong(request, "recordId");
		long userId = PortalUtil.getUserId(request);

		String fieldName = ParamUtil.getString(request, "fieldName");

		DDLRecord record = DDLRecordLocalServiceUtil.getRecord(recordId);

		ServiceContext serviceContext = ServiceContextFactory.getInstance(
			DDLRecord.class.getName(), request);

		Fields fields = record.getFields();

		Field field = fields.get(fieldName);

		field.setValue(StringPool.BLANK);

		DDLRecordLocalServiceUtil.updateRecord(
			userId, recordId, false, record.getDisplayIndex(), fields,
			true, serviceContext);
	}

	@Override
	protected boolean isCheckMethodOnProcessAction() {
		return _CHECK_METHOD_ON_PROCESS_ACTION;
	}

	protected JSONObject updateRecordFieldFile(PortletRequest request)
		throws Exception {

		long recordId = ParamUtil.getLong(request, "recordId");

		String fieldName = ParamUtil.getString(request, "fieldName");

		DDLRecord record = DDLRecordLocalServiceUtil.getRecord(recordId);

		UploadPortletRequest uploadPortletRequest =
			PortalUtil.getUploadPortletRequest(request);

		ServiceContext serviceContext = ServiceContextFactory.getInstance(
			DDLRecord.class.getName(), request);

		DDLUtil.uploadRecordFieldFile(
			record, fieldName, uploadPortletRequest, serviceContext);

		String fieldValue = String.valueOf(record.getFieldValue(fieldName));

		return JSONFactoryUtil.createJSONObject(fieldValue);
	}

	private static final boolean _CHECK_METHOD_ON_PROCESS_ACTION = false;

}