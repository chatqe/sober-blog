package io.charqe.qs.infra.thirdparty.mail;

import com.alibaba.fastjson2.JSON;
import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.modules.comment.enums.CommentTypeEnum;
import io.charqe.qs.modules.im.http.entity.ImChatUserMessage;
import io.charqe.qs.modules.article.domain.pojo.Article;
import io.charqe.qs.modules.comment.domain.pojo.Comment;
import io.charqe.qs.modules.comment.controller.web.vo.CommentVO;
import io.charqe.qs.modules.user.domain.pojo.User;
import io.charqe.qs.modules.webInfo.domain.pojo.WebInfo;
import io.charqe.qs.modules.comment.service.CommentService;
import io.charqe.qs.utils.BlogUtil;
import io.charqe.qs.utils.CommonQuery;
import io.charqe.qs.utils.cache.QingSiCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class MailSendUtil {

    @Autowired
    private CommonQuery commonQuery;

    @Autowired
    private MailUtil mailUtil;

    @Resource
    private QingSiCache qingSiCache;

    public void sendCommentMail(CommentVO commentVO, Article one, CommentService commentService) {
        List<String> mail = new ArrayList<>();
        String toName = "";
        if (commentVO.getReplyToUserId() != null) {
            User user = commonQuery.getUser(commentVO.getReplyToUserId());
            if (user != null && !user.getId().equals(BlogUtil.getUserId()) && StringUtils.hasText(user.getEmail())) {
                toName = user.getUsername();
                mail.add(user.getEmail());
            }
        } else {
            if (CommentTypeEnum.MESSAGE.code().equals(commentVO.getBizType()) ||
                    CommentTypeEnum.LOVE.code().equals(commentVO.getBizType())) {
                User adminUser = BlogUtil.getAdminUser();
                if (StringUtils.hasText(adminUser.getEmail()) && !Objects.equals(BlogUtil.getUserId(), adminUser.getId())) {
                    mail.add(adminUser.getEmail());
                }
            } else if (CommentTypeEnum.ARTICLE.code().equals(commentVO.getBizType())) {
                User user = commonQuery.getUser(one.getUserId());
                if (user != null && StringUtils.hasText(user.getEmail()) && !user.getId().equals(BlogUtil.getUserId())) {
                    mail.add(user.getEmail());
                }
            }
        }

        if (!CollectionUtils.isEmpty(mail)) {
            String sourceName = "";
            if (CommentTypeEnum.ARTICLE.code().equals(commentVO.getBizType())) {
                sourceName = one.getArticleTitle();
            }
            String commentMail = getCommentMail(commentVO.getBizType(), sourceName,
                    BlogUtil.getUsername(),
                    commentVO.getContent(),
                    toName,
                    commentVO.getParentId(), commentService);

            AtomicInteger count = JSON.parseObject(qingSiCache.get(CommonConst.COMMENT_IM_MAIL + mail.get(0)), AtomicInteger.class);
            if (count == null || count.get() < CommonConst.COMMENT_IM_MAIL_COUNT) {
                WebInfo webInfo = JSON.parseObject(qingSiCache.get(CommonConst.WEB_INFO), WebInfo.class);
                mailUtil.sendMailMessage(mail, "您有一封来自" + (webInfo == null ? "youngwanton" : webInfo.getWebName()) + "的回执！", commentMail);
                if (count == null) {
                    qingSiCache.put(CommonConst.COMMENT_IM_MAIL + mail.get(0), new AtomicInteger(1), CommonConst.CODE_EXPIRE);
                } else {
                    count.incrementAndGet();
                }
            }
        }
    }

    /**
     * source：0留言 其他是文章标题
     * fromName：评论人
     * toName：被评论人
     */
    private String getCommentMail(Integer commentType, String source, String fromName, String fromContent, String toName, Long toCommentId, CommentService commentService) {
        WebInfo webInfo = JSON.parseObject(qingSiCache.get(CommonConst.WEB_INFO), WebInfo.class);
        String webName = (webInfo == null ? "youngwanton" : webInfo.getWebName());

        String mailType = "";
        String toMail = "";
        if (StringUtils.hasText(toName)) {
            mailType = String.format(MailUtil.replyMail, fromName);
            Comment toComment = commentService.lambdaQuery().select(Comment::getContent).eq(Comment::getId, toCommentId).one();
            if (toComment != null) {
                toMail = String.format(MailUtil.originalText, toName, toComment.getContent());
            }
        } else {
            if (CommentTypeEnum.MESSAGE.code().equals(commentType)) {
                mailType = String.format(MailUtil.messageMail, fromName);
            } else if (CommentTypeEnum.ARTICLE.code().equals(commentType)) {
                mailType = String.format(MailUtil.commentMail, source, fromName);
            } else if (CommentTypeEnum.LOVE.code().equals(commentType)) {
                mailType = String.format(MailUtil.loveMail, fromName);
            }
        }

        return String.format(mailUtil.getMailText(),
                webName,
                mailType,
                fromName,
                fromContent,
                toMail,
                webName);
    }

    public void sendImMail(ImChatUserMessage message) {
        if (!message.getMessageStatus()) {
            List<String> mail = new ArrayList<>();
            String username = "";
            User toUser = commonQuery.getUser(message.getToId());
            if (toUser != null && StringUtils.hasText(toUser.getEmail())) {
                mail.add(toUser.getEmail());
            }
            User fromUser = commonQuery.getUser(message.getFromId());
            if (fromUser != null) {
                username = fromUser.getUsername();
            }

            if (!CollectionUtils.isEmpty(mail)) {
                String commentMail = getImMail(username, message.getContent());

                AtomicInteger count = JSON.parseObject(qingSiCache.get(CommonConst.COMMENT_IM_MAIL + mail.get(0)), AtomicInteger.class);
                if (count == null || count.get() < CommonConst.COMMENT_IM_MAIL_COUNT) {
                    WebInfo webInfo = JSON.parseObject(qingSiCache.get(CommonConst.WEB_INFO), WebInfo.class);
                    mailUtil.sendMailMessage(mail, "您有一封来自" + (webInfo == null ? "youngwanton" : webInfo.getWebName()) + "的回执！", commentMail);
                    if (count == null) {
                        qingSiCache.put(CommonConst.COMMENT_IM_MAIL + mail.get(0), new AtomicInteger(1), CommonConst.CODE_EXPIRE);
                    } else {
                        count.incrementAndGet();
                    }
                }
            }
        }
    }

    private String getImMail(String fromName, String fromContent) {
        WebInfo webInfo = JSON.parseObject(qingSiCache.get(CommonConst.WEB_INFO), WebInfo.class);
        String webName = (webInfo == null ? "youngwanton" : webInfo.getWebName());

        return String.format(mailUtil.getMailText(),
                webName,
                String.format(MailUtil.imMail, fromName),
                fromName,
                fromContent,
                "",
                webName);
    }
}


