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
package com.dianping.lion.entity;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置类型
 * @author danson.liu
 *
 */
public enum ConfigTypeEnum {
	
	//这里添加配置类型时需要在config-*.js中相应添加Type_*常量
	String(10, "string"), 
	Number(20, "number"), 
	Boolean(30, "boolean"), 
	Ref_Shared(60, "reference");

    private static volatile List<ConfigTypeEnum> Non_Ref_Types;

	private int value;
	
	private String label;

    private ConfigTypeEnum(int value, String label) {
		this.value = value;
		this.label = label;
    }

	/**
	 * @return the value
	 */
	public int getValue() {
		return value;
	}

	/**
	 * @param value the value to set
	 */
	public void setValue(int value) {
		this.value = value;
	}

	/**
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * @param label the label to set
	 */
	public void setLabel(String label) {
		this.label = label;
	}

    public static List<ConfigTypeEnum> nonRefs() {
        if (Non_Ref_Types == null) {
            synchronized (ConfigTypeEnum.class) {
                if (Non_Ref_Types == null) {
                    Non_Ref_Types = new ArrayList<ConfigTypeEnum>();
                    for (ConfigTypeEnum typeEnum : values()) {
//                        if (typeEnum != ConfigTypeEnum.Ref_DB && typeEnum != ConfigTypeEnum.Ref_Shared) {
                            Non_Ref_Types.add(typeEnum);
//                        }
                    }
                }
            }
        }
        return Non_Ref_Types;
    }


}
