package com.dianping.lion.service.impl;

import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.apache.log4j.Logger;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;

import com.dianping.lion.service.ZookeeperService;

/**
 * TODO: add border condition check
 *
 * @author chen.hua
 *
 */
public class ZookeeperServiceImpl implements ZookeeperService {

    private static final Logger LOG = Logger.getLogger(ZookeeperServiceImpl.class);

    private static final String CHARSET = "UTF-8";

    private CuratorFramework client;

    public ZookeeperServiceImpl(String server) throws Exception {
        this(server, null);
    }

    public ZookeeperServiceImpl(String server, String namespace) throws Exception {
        try {
            client = CuratorFrameworkFactory.builder().
                     connectString(server).
                     retryPolicy(new RetryNTimes(5, 1000)).
                     namespace(namespace).
                     build();
            client.start();
        } catch (Exception ex) {
            LOG.error("Failed to initialize zookeeper client", ex);
            throw ex;
        }
    }

    @Override
    public void create(String path, String data) throws Exception {
        try {
            client.create().creatingParentsIfNeeded().forPath(path, data.getBytes(CHARSET));
        } catch (Exception ex) {
            LOG.error("Failed to create path " + path + ":" + data, ex);
            throw ex;
        }
    }

    @Override
    public void delete(String path) throws Exception {
        try {
            client.delete().forPath(path);
        } catch (Exception ex) {
            LOG.error("Failed to delete path " + path, ex);
            throw ex;
        }
    }

    @Override
    public String get(String path) throws Exception {
        try {
            byte[] data = client.getData().forPath(path);
            return new String(data, CHARSET);
        } catch(KeeperException.NoNodeException ex) {
            LOG.warn("", ex);
            return null;
        } catch (Exception ex) {
            LOG.error("Failed to get path " + path, ex);
            throw ex;
        }
    }

    @Override
    public void set(String path, String data) throws Exception {
        try {
            client.setData().forPath(path, data.getBytes(CHARSET));
        } catch (Exception ex) {
            LOG.error("Failed to set path " + path + ":" + data, ex);
            throw ex;
        }
    }

    @Override
    public void createOrSet(String path, String data) throws Exception {
        if(exists(path)) {
            set(path, data);
        } else {
            create(path, data);
        }
    }

    @Override
    public List<String> getChildren(String path) throws Exception {
        try {
            List<String> children = client.getChildren().forPath(path);
            return children;
        } catch (Exception ex) {
            LOG.error("Failed to get children of " + path, ex);
            throw ex;
        }
    }

    @Override
    public boolean exists(String path) throws Exception {
        try {
            Stat stat = client.checkExists().forPath(path);
            return stat != null;
        } catch (Exception ex) {
            LOG.error("Failed to check path exists " + path, ex);
            throw ex;
        }
    }

}
