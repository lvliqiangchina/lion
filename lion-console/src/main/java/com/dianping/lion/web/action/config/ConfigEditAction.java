/**
 * Project: com.dianping.lion.lion-console-0.0.1
 *
 * File Created at 2012-7-17
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
package com.dianping.lion.web.action.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;

import com.dianping.lion.util.GroupPatternUtils;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang.StringUtils;
import org.apache.struts2.json.JSONException;
import org.apache.struts2.json.JSONUtil;
import org.json.JSONObject;

import com.dianping.lion.entity.Config;
import com.dianping.lion.entity.ConfigInstance;
import com.dianping.lion.entity.ConfigTypeEnum;
import com.dianping.lion.entity.Environment;
import com.dianping.lion.entity.OperationLog;
import com.dianping.lion.entity.OperationTypeEnum;
import com.dianping.lion.entity.Project;
import com.dianping.lion.exception.NoPrivilegeException;
import com.dianping.lion.exception.RuntimeBusinessException;
import com.dianping.lion.util.SecurityUtils;

/**
 * @author danson.liu
 *
 */
@SuppressWarnings("serial")
public class ConfigEditAction extends AbstractConfigAction {

	private Config config;

	private int configId;

	private boolean trim;

	private String value;

	private String context;

	private List<Integer> envIds;

	public String create() {
		int configType = config.getType();
		boolean isStringType = configType == ConfigTypeEnum.String.getValue();
		if ((isStringType && trim) || !isStringType) {
			value = value.trim();
		}
		int configId;
		try {
			configId = configService.createConfig(config);
			operationLogService.createOpLog(new OperationLog(OperationTypeEnum.Config_Add, config.getProjectId(), "创建配置项["
			      + config.getKey() + "]").key(config.getKey()));
		} catch (RuntimeBusinessException e) {
			createErrorStreamResponse(e.getMessage());
			return SUCCESS;
		}
		List<String> failedEnvs = new ArrayList<String>();
		Map<Integer, Environment> envMap = environmentService.findEnvMap();
		Integer currentUserId = SecurityUtils.getCurrentUserId();
		for (Integer envId : envIds) {
			ConfigInstance instance = new ConfigInstance();
			instance.setConfigId(configId);
			instance.setEnvId(envId);
			instance.setValue(value);
			try {
				if (!privilegeDecider.hasAddConfigPrivilege(config.getProjectId(), envId, currentUserId)) {
					throw NoPrivilegeException.INSTANCE;
				}
				configService.createInstance(instance);
				operationLogService.createOpLog(new OperationLog(OperationTypeEnum.Config_Edit, config.getProjectId(),
				      envId, "设置配置项[" + config.getKey() + "]").key(config.getKey(), ConfigInstance.NO_CONTEXT, null,
				      instance.getValue()));
			} catch (NoPrivilegeException e) {
				String env = envMap.get(envId).getLabel();
				failedEnvs.add(env + "(无权限)");
			} catch (RuntimeException e) {
				String env = envMap.get(envId).getLabel();
				logger.error("创建配置[key=" + config.getKey() + ", env=" + env + "]失败.", e);
				failedEnvs.add(env);
			}
		}
		if (failedEnvs.isEmpty()) {
			createSuccessStreamResponse();
		} else {
			createWarnStreamResponse("保存[" + StringUtils.join(failedEnvs, ',') + "]环境下的配置项值失败.");
		}
		return SUCCESS;
	}

	public String saveDefaultValue() {
		Config configFound = configService.getConfig(configId);
		if (configFound == null) {
			createErrorStreamResponse("该配置已不存在!");
			return SUCCESS;
		}
		boolean isStringType = configFound.getType() == ConfigTypeEnum.String.getValue();
		if ((isStringType && trim) || !isStringType) {
			value = value.trim();
		}
		List<String> failedEnvs = new ArrayList<String>();
		Map<Integer, Environment> envMap = environmentService.findEnvMap();
		Integer currentUserId = SecurityUtils.getCurrentUserId();
		for (Integer envId : envIds) {
			try {
				if (!privilegeDecider.hasEditConfigPrivilege(configFound.getProjectId(), envId, configFound.getId(),
				      currentUserId)) {
					throw NoPrivilegeException.INSTANCE;
				}
				ConfigInstance existInstance = configService.findInstance(configId, envId, ConfigInstance.NO_CONTEXT);
				configService.setConfigValue(configId, envId, ConfigInstance.NO_CONTEXT, value);
				setJDBCConfigGroup(envId, configFound.getKey(), reference2Key(value));
				operationLogService.createOpLog(new OperationLog(OperationTypeEnum.Config_Edit, configFound.getProjectId(),
				      envId, "设置配置项: " + configFound.getKey()).key(configFound.getKey(), ConfigInstance.NO_CONTEXT,
				      existInstance != null ? existInstance.getValue() : null, value));
			} catch (NoPrivilegeException e) {
				String env = envMap.get(envId).getLabel();
				failedEnvs.add(env + "(无权限)");
			} catch (Exception e) {
				String env = envMap.get(envId).getLabel();
				logger.error("保存配置[key=" + configFound.getKey() + ", env=" + env + "]失败.", e);
				failedEnvs.add(env);
			}
		}
		if (failedEnvs.isEmpty()) {
			createSuccessStreamResponse();
		} else {
			createWarnStreamResponse("保存[" + StringUtils.join(failedEnvs, ',') + "]环境下的配置项值失败.");
		}
		return SUCCESS;
	}

	private String key2Reference(String key) {
		return "${" + key + "}";
	}

	private String reference2Key(String reference) {
		reference = reference.replace("${", "");
		reference = reference.replace("}", "");
		return reference;
	}

	private void setJDBCConfigGroup(int envId, String privateKey, String publicKey) {
		GroupPatternUtils.JDBCGroup privateJDBCGroup = GroupPatternUtils.typeofJDBC(privateKey);
		GroupPatternUtils.JDBCGroup publicJDBCGroup = GroupPatternUtils.typeofJDBC(publicKey);

		if (privateJDBCGroup != publicJDBCGroup) {
			return;
		} else {
			String privateJDBCKeyPattern = GroupPatternUtils.getPrivateJDBCKeyPattern(privateKey);
			String publicJDBCKeyPattern = GroupPatternUtils.getPublicJDBCKeyPattern(publicKey);

			List<Config> privateJDBCConfigs = configService.findConfigByKeyPattern(privateJDBCKeyPattern);
			List<Config> publicJDBCConfigs = configService.findConfigByKeyPattern(publicJDBCKeyPattern);

			pairJDBCConfigs(envId, privateJDBCConfigs, publicJDBCConfigs);
		}
	}

	private void pairJDBCConfigs(int envId, List<Config> privateJDBCConfigs, List<Config> publicJDBCConfigs) {
		for (Config privateJDBCConfig : privateJDBCConfigs) {
			String privateKey = privateJDBCConfig.getKey();
			GroupPatternUtils.JDBCGroup privateJDBCGroup = GroupPatternUtils.typeofJDBC(privateKey);

			if (privateJDBCGroup == GroupPatternUtils.JDBCGroup.JDBC_NO) {
				continue;
			}

			for (Config publicJDBCConfig : publicJDBCConfigs) {
				String publicKey = publicJDBCConfig.getKey();
				GroupPatternUtils.JDBCGroup publicJDBCGroup = GroupPatternUtils.typeofJDBC(publicKey);

				if (privateJDBCGroup == publicJDBCGroup) {
					configService.setConfigValue(privateJDBCConfig.getId(), envId, ConfigInstance.NO_CONTEXT,
					      key2Reference(publicKey));
				}
			}
		}
	}

	public String saveContextValue() {
		Config config = null;
		try {
			config = configService.getConfig(configId);
			ConfigInstance instance = new ConfigInstance();
			instance.setConfigId(configId);
			instance.setContext(context);
			instance.setValue(value);
			instance.setEnvId(envId);
			configService.createInstance(instance);
 			operationLogService			      .createOpLog(new OperationLog(OperationTypeEnum.Config_Add, config.getProjectId(), envId, "添加泳道配置: "
			            + config.getKey() + "/" + context).key(config.getKey(), context, null, value));
			createSuccessStreamResponse();
		} catch (RuntimeException ex) {
			logger.error("添加泳道配置" + (config == null ? configId : config.getKey()) + "/" + context + "失败", ex);
			createErrorStreamResponse("添加泳道配置" + config.getKey() + "/" + context + "失败: \n" + ex.getMessage());
		}
		return SUCCESS;
	}

	public String deleteContextValue() {
		Config config = null;
		try {
			config = configService.getConfig(configId);
			configService.deleteInstance(configId, envId, context);
			operationLogService.createOpLog(new OperationLog(OperationTypeEnum.Config_Delete, config.getProjectId(),
			      envId, "删除泳道配置: " + config.getKey() + "/" + context).key(config.getKey(), context));
			createSuccessStreamResponse();
		} catch (RuntimeException ex) {
			logger.error("删除泳道配置" + (config == null ? configId : config.getKey()) + "/" + context + "失败", ex);
			createErrorStreamResponse("删除泳道配置" + config.getKey() + "/" + context + "失败: \n" + ex.getMessage());
		}
		return SUCCESS;
	}

	public String updateContextValue() {
		Config config = null;
		try {
			config = configService.getConfig(configId);
			ConfigInstance instance = configService.findInstance(configId, envId, context);
			String oldValue = instance.getValue();
			instance.setValue(value);
			configService.updateInstance(instance);
			operationLogService.createOpLog(new OperationLog(OperationTypeEnum.Config_Edit, config.getProjectId(), envId,
			      "编辑泳道配置: " + (config == null ? configId : config.getKey()) + "/" + context).key(config.getKey(), context, oldValue, value));
			createSuccessStreamResponse();
		} catch (RuntimeException ex) {
			logger.error("编辑泳道配置" + config.getKey() + "/" + context + "失败", ex);
			createErrorStreamResponse("编辑泳道配置" + config.getKey() + "/" + context + "失败: \n" + ex.getMessage());
		}
		return SUCCESS;
	}

	public String loadDefaultValue() {
		Config configFound = configService.getConfig(configId);
		if (configFound == null) {
			createErrorStreamResponse("该配置已不存在!");
			return SUCCESS;
		}
		ConfigInstance instanceFound = configService.findDefaultInstance(configId, envId);
		Integer currentUserId = SecurityUtils.getCurrentUserId();
		String message = null;
		if (instanceFound == null) {
			Environment prevEnv = environmentService.findPrevEnv(envId);
			if (prevEnv != null) {
				instanceFound = configService.findDefaultInstance(configId, prevEnv.getId());
				if (instanceFound != null) {
					if (privilegeDecider.hasReadConfigPrivilege(configFound.getProjectId(), prevEnv.getId(), configId,
					      currentUserId)) {
						message = "Value预填[" + prevEnv.getLabel() + "]环境的值.";
					} else {
						instanceFound = null;
					}
				}
			}
		} else {
			if (!privilegeDecider.hasReadConfigPrivilege(configFound.getProjectId(), envId, configId, currentUserId)) {
				instanceFound = null;
			}
		}
		try {
			createStreamResponse("{\"code\":0, \"value\":"
			      + JSONObject.quote(instanceFound != null ? instanceFound.getValue() : "") + ", \"privilege\":"
			      + JSONUtil.serialize(getEditPrivileges(configFound.getProjectId(), configId)) + ", \"msg\":\""
			      + (message != null ? message : "") + "\"}");
		} catch (JSONException e) {
			logger.error("get config edit privilege failed.", e);
			createErrorStreamResponse("get config edit privilege failed.");
		}
		return SUCCESS;
	}

	public String decodePassword() {
		config = configService.getConfig(configId);
		String password = configService.resolveConfigValue(config.getId(), envId, ConfigInstance.NO_CONTEXT);
		String decodedPassword = SecurityUtils.tryDecode(password);
		createStreamResponse(0, "Password: " + decodedPassword);
		return SUCCESS;
	}

	private boolean isOnline() {
		Environment environment = environmentService.findEnvByID(envId);
		return environment.isOnline();
	}

	public String testJdbcConnection() {
		String url = null;
		Connection conn = null;
		try {
			Config config = configService.getConfig(configId);
			String urlKey = config.getKey();
			String keyPrefix = urlKey.substring(0, urlKey.lastIndexOf('.'));

			url = configService.resolveConfigValue(configId, envId, ConfigInstance.NO_CONTEXT);
			assertNotNull(url, "JDBC url is null, key: " + config.getKey());
			// to avoid waiting for a long time before the getConnection(url) returns, append timeout params to the url
			url = appendTimeoutParams(url);

			String driverClassKey = keyPrefix + ".driverClassName";
			config = configService.findConfigByKey(driverClassKey);
			String driverClassName = null;
			if (config != null)
				driverClassName = configService.resolveConfigValue(config.getId(), envId, ConfigInstance.NO_CONTEXT);
			if (driverClassName == null) {
				// infer driver class from url
				driverClassName = getDriverFromUrl(url);
				assertNotNull(driverClassName, "JDBC driver class name is null, url: " + url);
			}

			Class.forName(driverClassName);

			String usernameKey = keyPrefix + ".username";
			config = configService.findConfigByKey(usernameKey);
			assertNotNull(config, "No config for key: " + usernameKey);
			String username = configService.resolveConfigValue(config.getId(), envId, ConfigInstance.NO_CONTEXT);
			assertNotNull(username, "JDBC user name is null, key: " + usernameKey);

			String passwordKey = keyPrefix + ".password";
			config = configService.findConfigByKey(passwordKey);
			assertNotNull(config, "No config for key: " + passwordKey);
			String password = configService.resolveConfigValue(config.getId(), envId, ConfigInstance.NO_CONTEXT);
			assertNotNull(password, "JDBC password is null, key: " + passwordKey);

			String decodedPassword = SecurityUtils.tryDecode(password);

			if (isOnline()) {
				conn = DriverManager.getConnection(url, username, decodedPassword);
				createStreamResponse(0, "Connected to: " + url + "\n\tusername: " + username + "\n\tpassword: " + password);
			} else {
				createStreamResponse(0, "Connection information:" + "\n\turl: " + url + ",\n\tusername: " + username
				      + ",\n\tpassword: " + decodedPassword + ",\n\tdriverClass: " + driverClassName);
			}

		} catch (Exception ex) {
			if (isOnline()) {
				createStreamResponse(-1, "Failed to connect to: " + url + ", " + ex.getMessage());
			} else {
				createStreamResponse(-1, "Miss connection information: " + "\n\t" + ex.getMessage());
			}
		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
				}
			}
		}
		return SUCCESS;
	}

	private void assertNotNull(Object value, String message) {
		if (value == null) {
			throw new RuntimeException(message);
		}
	}

	private String appendTimeoutParams(String url) {
		if (url.indexOf("jdbc:mysql:") != -1) {
			return url.indexOf('?') == -1 ? url + "?connectTimeout=5000" : url + "&connectTimeout=5000";
		} else if (url.indexOf("jdbc:postgresql:") != -1) {
			return url.indexOf('?') == -1 ? url + "?loginTimeout=5" : url + "&loginTimeout=5";
		} else if (url.indexOf("jdbc:sqlserver:") != -1) {
			return url + ";loginTimeout=5";
		}
		// unsupported database schema
		return url;
	}

	private String getDriverFromUrl(String url) {
		if (url.indexOf("jdbc:mysql:") != -1) {
			return "com.mysql.jdbc.Driver";
		} else if (url.indexOf("jdbc:postgresql:") != -1) {
			return "org.postgresql.Driver";
		} else if (url.indexOf("jdbc:sqlserver:") != -1) {
			return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
		}
		// unsupported database schema
		return null;
	}

	/**
	 * @return the project
	 */
	public Project getProject() {
		return project;
	}

	/**
	 * @return the config
	 */
	public Config getConfig() {
		return config;
	}

	/**
	 * @param config
	 *           the config to set
	 */
	public void setConfig(Config config) {
		this.config = config;
	}

	/**
	 * @return the projectId
	 */
	public int getPid() {
		return projectId;
	}

	/**
	 * @param projectId
	 *           the projectId to set
	 */
	public void setPid(int projectId) {
		this.projectId = projectId;
	}

	/**
	 * @return the trim
	 */
	public boolean isTrim() {
		return trim;
	}

	/**
	 * @param trim
	 *           the trim to set
	 */
	public void setTrim(boolean trim) {
		this.trim = trim;
	}

	/**
	 * @return the environments
	 */
	public List<Integer> getEnvIds() {
		return envIds;
	}

	/**
	 */
	public void setEnvIds(List<Integer> envIds) {
		this.envIds = envIds;
	}

	public String getContext() {
		return context;
	}

	public void setContext(String context) {
		this.context = context;
	}

	/**
	 * @return the value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * @param value
	 *           the value to set
	 */
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * @return the configId
	 */
	public int getConfigId() {
		return configId;
	}

	/**
	 * @param configId
	 *           the configId to set
	 */
	public void setConfigId(int configId) {
		this.configId = configId;
	}

}
