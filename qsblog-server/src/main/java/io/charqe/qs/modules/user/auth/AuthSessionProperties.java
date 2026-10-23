package io.charqe.qs.modules.user.auth;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.time.Clock;
import java.time.Duration;

@Data
@Configuration
@ConfigurationProperties(prefix = "auth.session")
public class AuthSessionProperties {
    private Policy web = new Policy(Duration.ofDays(7), Duration.ofDays(30));
    private Policy admin = new Policy(Duration.ofHours(2), Duration.ofHours(12));

    @Data
    public static class Policy {
        private Duration idle;
        private Duration absolute;
        public Policy() { }
        public Policy(Duration idle, Duration absolute) {
            this.idle = idle;
            this.absolute = absolute;
        }
    }

    public Policy policy(SessionScope scope) {
        if (scope == null || scope == SessionScope.EITHER) throw new IllegalArgumentException("必须指定 WEB 或 ADMIN 登录域");
        return scope == SessionScope.ADMIN ? admin : web;
    }

    @PostConstruct
    public void validate() {
        for (Policy policy : new Policy[]{web, admin}) {
            if (policy == null || policy.idle == null || policy.absolute == null
                    || policy.idle.getSeconds() <= 0 || policy.absolute.getSeconds() <= 0
                    || policy.idle.getNano() != 0 || policy.absolute.getNano() != 0
                    || policy.idle.compareTo(policy.absolute) > 0) {
                throw new IllegalArgumentException("会话时长必须是正整数秒，且闲置时间不能超过绝对寿命");
            }
        }
    }

    @Bean
    public Clock authClock() { return Clock.systemUTC(); }
}
