package com.graduation.his.listener;

import cn.dev33.satoken.listener.SaTokenListener;
import cn.dev33.satoken.stp.SaLoginModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 用户登录事件监听器
 * @author hua
 */
@Slf4j
@Component
public class UserLoginEventListener implements SaTokenListener {

    @Override
    public void doLogin(String loginType, Object loginId, String tokenValue, SaLoginModel loginModel) {
        // 登录成功事件
        try {
            Long userId = Long.valueOf(loginId.toString());
            log.info("用户登录成功，userId: {}", userId);
        } catch (Exception e) {
            log.error("处理用户登录事件异常", e);
        }
    }

    // 以下是SaTokenListener接口的其他方法，实现空方法

    @Override
    public void doLogout(String loginType, Object loginId, String tokenValue) {
        // 登出事件，不需要处理
    }

    @Override
    public void doKickout(String loginType, Object loginId, String tokenValue) {
        // 被踢出事件，不需要处理
    }

    @Override
    public void doReplaced(String loginType, Object loginId, String tokenValue) {
        // 被顶下线事件，不需要处理
    }

    @Override
    public void doDisable(String loginType, Object loginId, String service, int level, long disableTime) {
        // 账号被封禁事件，不需要处理
    }

    @Override
    public void doUntieDisable(String loginType, Object loginId, String service) {
        // 账号被解封事件，不需要处理
    }

    @Override
    public void doCreateSession(String id) {
        // 会话创建事件，不需要处理
    }

    @Override
    public void doLogoutSession(String id) {
        // 会话注销事件，不需要处理
    }

    @Override
    public void doRenewTimeout(String tokenValue, Object loginId, long timeout) {
        // Token续期事件，不需要处理
    }

    @Override
    public void doOpenSafe(String loginType, String tokenValue, String service, long safeTime) {
        // 二级认证开启事件，不需要处理
    }

    @Override
    public void doCloseSafe(String loginType, String tokenValue, String service) {
        // 二级认证关闭事件，不需要处理
    }
}
