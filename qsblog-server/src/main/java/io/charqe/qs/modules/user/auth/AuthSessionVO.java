package io.charqe.qs.modules.user.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.charqe.qs.modules.user.domain.pojo.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "当前端会话；accessToken仅签发时返回")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthSessionVO {
    private String accessToken;
    private String sessionId;
    private SessionScope sessionType;
    private long issuedAt;
    private long idleExpiresAt;
    private long absoluteExpiresAt;
    private Profile user;

    @Data
    public static class Profile {
        private Long id;
        private String username;
        private String avatar;
        private Integer userType;
        private String email;
        private String phoneNumber;
        private Integer gender;
        private String introduction;
        private String subscribe;
    }

    public static AuthSessionVO from(SaTokenSessionAdapter.Metadata session, User user, String token) {
        AuthSessionVO vo = new AuthSessionVO();
        vo.setAccessToken(token);
        vo.setSessionId(session.getSessionId());
        vo.setSessionType(session.getSessionType());
        vo.setIssuedAt(session.getIssuedAt());
        vo.setIdleExpiresAt(session.getIdleExpiresAt());
        vo.setAbsoluteExpiresAt(session.getAbsoluteExpiresAt());
        Profile profile = new Profile();
        profile.setId(user.getId());
        profile.setUsername(user.getUsername());
        profile.setAvatar(user.getAvatar());
        profile.setUserType(user.getUserType());
        profile.setEmail(user.getEmail());
        profile.setPhoneNumber(user.getPhoneNumber());
        profile.setGender(user.getGender());
        profile.setIntroduction(user.getIntroduction());
        profile.setSubscribe(user.getSubscribe());
        vo.setUser(profile);
        return vo;
    }
}
