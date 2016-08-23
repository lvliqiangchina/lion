/**
 * Project: com.dianping.lion.lion-api-0.0.1-new
 * 
 * File Created at 2012-7-31
 * $Id$
 * 
 * Copyright 2010 dianping.com.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Dianping Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with dianping.com.
 */
package com.dianping.lion.db;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.dianping.lion.ServiceConstants;
import com.dianping.lion.entity.JobExecTime;
import com.dianping.lion.entity.OperationLog;
import com.dianping.lion.entity.OperationTypeEnum;
import com.dianping.lion.entity.User;
import com.dianping.lion.job.SyncJob;
import com.dianping.lion.service.OperationLogService;
import com.dianping.lion.service.UserService;
import com.dianping.lion.util.JsonParser;
import com.dianping.lion.util.SecurityUtils;

/**
 * Scheduler
 * @author youngphy.yang
 *
 */
public class DataSourceJob extends SyncJob{
	private static Logger logger = Logger.getLogger(DataSourceJob.class);
	
	
	@Autowired
	private OperationLogService operationLogService;
	
	@Autowired
	private DataSourceFetcher dataSourceFetcher;
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private JsonParser jsonParser;
	
	@Autowired
	private Storager storager;
	
	/**
	 * 记录异常数
	 */
	private static int count = 0;
	/**
	 * 记录多少次异常发送一次邮件
	 */
	private static int alarmthreshold = 10;
	/**
	 * mail code
	 */
	private static int mailCode = 15;
	
	private static String title = "DB信息同步lion失败";

	public DataSourceFetcher getDataSourceFetcher() {
		return dataSourceFetcher;
	}

	public void setDataSourceFetcher(DataSourceFetcher dataSourceFetcher) {
		this.dataSourceFetcher = dataSourceFetcher;
	}

	public Storager getStorager() {
		return storager;
	}

	public void setStorager(Storager storager) {
		this.storager = storager;
	}
	
	@Override
	public void doBusiness() throws Exception {
		JobExecTime jobExecTime = jobExecTimeDao.getJobExecTime(jobName);
		Calendar can = Calendar.getInstance();
		can.setTime(jobExecTime.getLastFetchTime());
		String dsContent = dataSourceFetcher.fetchDS(can.getTimeInMillis() / 1000);
		if(StringUtils.isBlank(dsContent)) {
			return;
		}
		try {
			try {
				User user = userService.findById(ServiceConstants.USER_SA_ID);
				SecurityUtils.setCurrentUser(user);
				storager.store(dsContent);
				jobExecTime.setLastFetchTime(new Date(Long.parseLong(jsonParser.getLastFetchTime(dsContent)) * 1000));
				jobExecTimeDao.updateLastFetchTime(jobExecTime);
				operationLogService.createOpLog(new OperationLog(OperationTypeEnum.Job_DSFetcher, "数据源有变更.").key(null, null, dsContent));
			} finally {
				SecurityUtils.clearCurrentUser();
			}
		} catch (Exception e) {
			logger.debug("Failed to store the config.",e);
			if(jobExecTime.getFailMail() != null && (count % alarmthreshold == 0)) {
				String[] mails = jobExecTime.getFailMail().split(",");
				StringWriter out = new StringWriter();
				e.printStackTrace(new PrintWriter(out));
				for(String email : mails) {
					StringBuffer body = new StringBuffer();
					body.append("从"+jobExecTime.getLastFetchTime()+"开始增量DB信息:\n"+dsContent);
					body.append("\n异常信息->\n"+out.toString());
					//boolean emailSendResult = mailService.send(mailCode, email, title, body.toString());
					boolean emailSendResult = true;
					if (!emailSendResult) {
						logger.warn("Send mail Fail!Email Address:" + email);
					}
				}
			}
			count++;
		}
		logger.debug("running");
	}

	public void setOperationLogService(OperationLogService operationLogService) {
		this.operationLogService = operationLogService;
	}
	
}
