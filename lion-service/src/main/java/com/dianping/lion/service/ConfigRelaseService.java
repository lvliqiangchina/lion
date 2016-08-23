/**
 * Project: com.dianping.lion.lion-service-0.0.1
 *
 * File Created at 2012-8-10
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
package com.dianping.lion.service;

import com.dianping.lion.entity.ConfigSetTask;
import com.dianping.lion.entity.ConfigSnapshotSet;

/**
 * @author danson.liu
 *
 */
public interface ConfigRelaseService {

	int createSetTask(ConfigSetTask task);

	/**
	 * @param id
	 * @param id2
	 * @param features
	 * @param keys
	 * @param b
	 */
	void executeSetTask(int projectId, int envId, String[] features, String[] keys, boolean push2App);

	/**
	 * @param id
	 * @param id2
	 * @param task
	 */
	int createSnapshotSet(int projectId, int envId, String task);

	/**
	 * 获取指定项目对应指定task所使用的配置镜像
	 * @param id
	 * @param id2
	 * @param task
	 * @return
	 */
	ConfigSnapshotSet findSnapshotSetToRollback(int projectId, int envId, String task);

	/**
	 * @param snapshotSet
	 * @return
	 */
	ConfigRollbackResult rollbackSnapshotSet(ConfigSnapshotSet snapshotSet);

	/**
	 * Rollback selected keys from snapshot
	 *
	 * @param snapshotSet to rollback configs from
	 * @param keys to rollback
	 * @return
	 */
	ConfigRollbackResult rollbackSnapshotSet(ConfigSnapshotSet snapshotSet, String[] keys);

}
