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
package com.dianping.lion.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import com.dianping.lion.entity.Config;
import com.dianping.lion.entity.ConfigInstance;
import com.dianping.lion.entity.Environment;
import com.dianping.lion.service.ConfigService;
import com.dianping.lion.service.EnvironmentService;

/**
 * JsonParser 解析DB增量json格式数据，生成返回config、configInstance实例
 * @author youngphy.yang
 */
public class JsonParser {
	private static String TIMESTAMP = "timestamp";
	private static String REMOVED = "remove";
	
	private EnvironmentService envService;
	private ConfigService configService;
	
	/**
	 * 获取增量config信息
	 * @param dbContent 增量DB信息
	 * @return
	 * @throws Exception
	 */
	public Map<String, Boolean> getDBAlias(String dbContent) throws Exception {
		Map<String, Boolean> dbAliases = null;
		JSONObject jsonObj = new JSONObject(dbContent);
		String[] names = JSONObject.getNames(jsonObj);
		dbAliases = new HashMap<String,Boolean>();
		a: for(int i = 0; i < names.length; i++) {
			String dsName = names[i];
			if(TIMESTAMP.equals(dsName)) {
				continue;
			} else {
				JSONObject envDSContent = jsonObj.getJSONObject(dsName);
				dsName = "data-source." + dsName;
				String[] envs = JSONObject.getNames(envDSContent);
				for (int j = 0; j < envs.length; j++) {
					if(REMOVED.equals(envs[j])) {
						dbAliases.put(dsName, true);
						continue a;
					}
				}
				dbAliases.put(dsName, false);
			}
		}
		return dbAliases;
	}
	
	/**
	 * 获取增量configInstances信息
	 * @param dbContent 增量DB信息
	 * @return
	 * @throws Exception
	 */
	public Map<ConfigInstance, Boolean> getConfigInstances(String dbContent) throws Exception {
		Map<ConfigInstance, Boolean> cis = new HashMap<ConfigInstance, Boolean>();
		JSONObject jsonObj = new JSONObject(dbContent);
		String[] names = JSONObject.getNames(jsonObj);
		List<String> dbAliases = new ArrayList<String>();
		for(int i = 0; i < names.length; i++) {
			if(!TIMESTAMP.equals(names[i])) {
				dbAliases.add(names[i]);
			}
		}
		for(int i = 0; i < dbAliases.size(); i++) {
			JSONObject envDSContent = jsonObj.getJSONObject(dbAliases.get(i));
			String[] envs = JSONObject.getNames(envDSContent);
			for (int j = 0; j < envs.length; j++) {
				if(REMOVED.equals(envs[j])) {
					continue;
				}
				ConfigInstance ci = new ConfigInstance();
				Config config = getConfigService().findConfigByKey("data-source." + dbAliases.get(i));
				ci.setConfigId(config.getId());
				Environment env = getEnvService().findEnvByName(envs[j].toLowerCase());
				ci.setEnvId(env.getId());
				ci.setValue(envDSContent.getString(envs[j]));
				boolean isRemovedContains = false;
				isRemovedContains = envDSContent.getJSONObject(envs[j]).has(REMOVED);	
				if(isRemovedContains) {
					cis.put(ci, true);
				} else {
					cis.put(ci, false);
				}
			}
		}
		return cis;
	}
	
	public String getLastFetchTime(String dsContent) throws Exception {
		JSONObject jsonObj = new JSONObject(dsContent);
		String lastFetchTime = jsonObj.getString(TIMESTAMP);
		return lastFetchTime;
	}

	public EnvironmentService getEnvService() {
		return envService;
	}

	public void setEnvService(EnvironmentService envService) {
		this.envService = envService;
	}

	public ConfigService getConfigService() {
		return configService;
	}

	public void setConfigService(ConfigService configService) {
		this.configService = configService;
	}
	
}
