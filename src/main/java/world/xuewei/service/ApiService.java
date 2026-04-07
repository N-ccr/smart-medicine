package world.xuewei.service;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationOutput;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.aigc.generation.models.QwenParam;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.MessageManager;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.utils.Constants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import world.xuewei.dto.DoctorChatMessage;

import java.util.List;

/**
 * 智慧医生服务
 */
@Service
public class ApiService {

    private static final String SYSTEM_PROMPT = "你是智能医疗助手，只回答与医疗相关的问题，不要回答其他问题。";

    /**
     * 与 MessageManager 上限配合，控制多轮轮数（含当前轮）
     */
    private static final int MESSAGE_MANAGER_CAP = 32;

    @Value("${ai-key}")
    private String apiKey;

    /**
     * 单轮对话（兼容旧调用）
     */
    public String query(String queryMessage) {
        return queryWithHistory(java.util.Collections.emptyList(), queryMessage);
    }

    /**
     * 多轮对话：history 为当前轮之前的 user/assistant 消息（按时间顺序），不含本次用户输入。
     */
    public String queryWithHistory(List<DoctorChatMessage> history, String newUserMessage) {
        Constants.apiKey = apiKey;
        try {
            //创建大模型客户端
            Generation gen = new Generation();
            MessageManager msgManager = new MessageManager(MESSAGE_MANAGER_CAP);
            msgManager.add(Message.builder().role(Role.SYSTEM.getValue()).content(SYSTEM_PROMPT).build());
            if (history != null) {
                for (DoctorChatMessage turn : history) {
                    if (turn == null || turn.getContent() == null || turn.getContent().trim().isEmpty()) {
                        continue;
                    }
                    String r = turn.getRole();
                    if (!Role.USER.getValue().equals(r) && !Role.ASSISTANT.getValue().equals(r)) {
                        continue;
                    }
                    msgManager.add(Message.builder().role(r).content(turn.getContent()).build());
                }
            }
            msgManager.add(Message.builder().role(Role.USER.getValue()).content(newUserMessage).build());
            QwenParam param = QwenParam.builder().model(Generation.Models.QWEN_TURBO).messages(msgManager.get()).resultFormat(QwenParam.ResultFormat.MESSAGE).build();
            GenerationResult result = gen.call(param);
            GenerationOutput output = result.getOutput();
            Message message = output.getChoices().get(0).getMessage();
            return message.getContent();
        } catch (Exception e) {
            return "智能医生现在不在线，请稍后再试～";
        }
    }
}
