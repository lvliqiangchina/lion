/**
 * 
 */
package com.dianping.lion.service.impl;

import org.springframework.beans.factory.annotation.Autowired;

import com.dianping.lion.entity.Config;
import com.dianping.lion.entity.Environment;
import com.dianping.lion.entity.User;
import com.dianping.lion.service.ConfigService;
import com.dianping.lion.service.EnvironmentService;
import com.dianping.lion.service.ProjectPrivilegeDecider;
import com.dianping.lion.service.ProjectService;
import com.dianping.lion.service.UserService;

/**
 * @author danson.liu
 *
 */
public class ProjectPrivilegeDeciderImpl implements ProjectPrivilegeDecider {
    
    @Autowired
    private EnvironmentService environmentService;
    
    @Autowired
    private ProjectService projectService;
    
    @Autowired
    private ConfigService configService;
    
    @Autowired
    private UserService userService;

    @Override
    public boolean hasReadConfigPrivilege(int projectId, int envId, int configId, Integer userId) {
        Environment environment = environmentService.findEnvByID(envId);
        User user = userId != null ? userService.loadById(userId) : null;
        if (user != null && (user.isAdmin() || user.isSystem() || user.isSA())) {
            return true;
        }
        if (environment != null) {
            if (environment.isOnline()) {
                if (user != null) {
                    Config config = configService.getConfig(configId);
                    if (config != null && !config.isPrivatee()) {
                        return user.isOnlineConfigView() || projectService.isMember(projectId, userId) || projectService.isOwner(projectId, userId)
                                || projectService.isOperator(projectId, userId);
                    }
                    return false;
                }
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean hasAddConfigPrivilege(int projectId, int envId, Integer userId) {
        if (userId == null) {
            return false;
        }
        Environment environment = environmentService.findEnvByID(envId);
        User user = userService.loadById(userId);
        if (user != null && (user.isAdmin() || user.isSystem() || user.isSA())) {
            return true;
        }
        if (environment != null) {
            if (environment.isOnline()) {
                return projectService.isOwner(projectId, userId) || projectService.isOperator(projectId, userId);
            } else {
                return projectService.isMember(projectId, userId) || projectService.isOwner(projectId, userId) 
                            || projectService.isOperator(projectId, userId);
            }
        }
        return false;
    }

    @Override
    public boolean hasEditConfigPrivilege(int projectId, int envId, int configId, Integer userId) {
        if (userId == null) {
            return false;
        }
        Environment environment = environmentService.findEnvByID(envId);
        User user = userService.loadById(userId);
        if (user != null && (user.isAdmin() || user.isSystem() || user.isSA())) {
            return true;
        }
        if (environment != null) {
            if (environment.isOnline()) {
                Config config = configService.getConfig(configId);
                if (config != null && !config.isPrivatee()) {
                    return projectService.isOwner(projectId, userId) || projectService.isOperator(projectId, userId);
                }
                return false;
            } else {
                return projectService.isMember(projectId, userId) || projectService.isOwner(projectId, userId) 
                            || projectService.isOperator(projectId, userId);
            }
        }
        return false;
    }

    @Override
    public boolean hasLockConfigPrivilege(Integer userId) {
        if (userId == null) {
            return false;
        }
        User user = userService.loadById(userId);
        return user.isAdmin() || user.isSA();
    }

	@Override
	public boolean hasReadApplogPrivilege(int projectId, Integer userId) {
		if (userId == null) {
			return false;
		}
		User user = userService.loadById(userId);
		return user.isAdmin() || user.isSA() || projectService.isOwner(projectId, userId) || projectService.isMember(projectId, userId)
			|| projectService.isOperator(projectId, userId);
	}

	@Override
	public boolean hasEditConfigAttrPrivilege(int projectId, Integer userId) {
		if (userId == null) {
			return false;
		}
		User user = userService.loadById(userId);
		return user.isAdmin() || user.isSA() || projectService.isOwner(projectId, userId) || projectService.isMember(projectId, userId)
			|| projectService.isOperator(projectId, userId);
	}

	@Override
	public boolean hasReadDSFetchLogPrivilege(Integer userId) {
		if (userId == null) {
			return false;
		}
		User user = userService.loadById(userId);
		return user.isAdmin() || user.isSA();
	}

	@Override
	public boolean hasManageProjectMemberPrivilege(int projectId, User user) {
		if (user == null) {
			return false;
		}
		return user.isAdmin() || user.isSA() || projectService.isOwner(projectId, user.getId());
	}

    /**
     * @param environmentService the environmentService to set
     */
    public void setEnvironmentService(EnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

    /**
     * @param projectService the projectService to set
     */
    public void setProjectService(ProjectService projectService) {
        this.projectService = projectService;
    }

    public void setConfigService(ConfigService configService) {
        this.configService = configService;
    }

    /**
     * @param userService the userService to set
     */
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

}
