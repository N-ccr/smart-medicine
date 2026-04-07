package world.xuewei.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import world.xuewei.dto.DoctorChatMessage;
import world.xuewei.dto.RespResult;
import world.xuewei.entity.User;
import world.xuewei.utils.Assert;

import com.alibaba.dashscope.common.Role;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 消息控制器
 *
 * @author XUEW
 */
@RestController
@RequestMapping("/message")
public class MessageController extends BaseController<User> {

    private static final String SESSION_DOCTOR_CHAT = "doctorChatHistory";

    /**
     * Session 内最多保留的消息条数（user+assistant 合计），约 10 轮对话
     */
    private static final int MAX_CHAT_MESSAGES = 20;

    /**
     * 当前会话内的对话历史（用于前端恢复与多轮上下文）
     */
    @SuppressWarnings("unchecked")
    @GetMapping("/history")
    public RespResult history() {
        List<DoctorChatMessage> list = (List<DoctorChatMessage>) session.getAttribute(SESSION_DOCTOR_CHAT);
        if (list == null || list.isEmpty()) {
            return RespResult.success("ok", Collections.emptyList());
        }
        return RespResult.success("ok", new ArrayList<>(list));
    }

    /**
     * 清空当前会话的对话记忆
     */
    @PostMapping("/clear")
    public RespResult clear() {
        session.removeAttribute(SESSION_DOCTOR_CHAT);
        return RespResult.success("已清空对话");
    }

    /**
     * 发送消息（多轮：服务端 Session 累积历史并传给模型）
     */
    @PostMapping("/query")
    public RespResult query(String content) {
        if (Assert.isEmpty(content) || content.trim().isEmpty()) {
            return RespResult.fail("内容不能为空");
        }
        String text = content.trim();
        @SuppressWarnings("unchecked")
        List<DoctorChatMessage> history = (List<DoctorChatMessage>) session.getAttribute(SESSION_DOCTOR_CHAT);
        if (history == null) {
            history = new ArrayList<>();
        }
        String reply = apiService.queryWithHistory(history, text);
        boolean offline = "智能医生现在不在线，请稍后再试～".equals(reply);
        if (!offline) {
            history.add(DoctorChatMessage.builder().role(Role.USER.getValue()).content(text).build());
            history.add(DoctorChatMessage.builder().role(Role.ASSISTANT.getValue()).content(reply).build());
            trimHistory(history);
            session.setAttribute(SESSION_DOCTOR_CHAT, history);
        }
        return RespResult.success(reply);
    }

    private static void trimHistory(List<DoctorChatMessage> history) {
        while (history.size() > MAX_CHAT_MESSAGES) {
            history.remove(0);
            if (!history.isEmpty()) {
                history.remove(0);
            }
        }
    }
}
