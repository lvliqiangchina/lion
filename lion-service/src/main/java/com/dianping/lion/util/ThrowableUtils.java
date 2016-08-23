/**
 * Project: com.dianping.lion.lion-service-0.0.1
 * 
 * File Created at 2012-8-16
 * $Id$
 * 
 * Copyright 2010 dianping.com.
 * All rights reserved.
 * 
 * This software is the confidential and proprietary information of
 * Dianping Company. ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with dianping.com.
 */
package com.dianping.lion.util;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.lang.StringUtils;

/**
 * 
 * @author danson.liu
 * 
 */
public class ThrowableUtils {

    public static String extractStackTrace(Throwable t) {
        StringWriter me = new StringWriter();
        PrintWriter pw = new PrintWriter(me);
        t.printStackTrace(pw);
        pw.flush();
        return me.toString();
    }

    public static String extractStackTrace(Throwable t, int maxLen) {
        StringWriter me = new StringWriter();
        PrintWriter pw = new PrintWriter(me);
        t.printStackTrace(pw);
        pw.flush();
        return StringUtils.substring(me.toString(), 0, maxLen);
    }

}
