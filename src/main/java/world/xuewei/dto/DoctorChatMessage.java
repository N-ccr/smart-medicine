package world.xuewei.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 智能医生对话中的一条消息（存于 Session，用于多轮上下文）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorChatMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * user 或 assistant（与通义 Role 取值一致）
     */
    private String role;

    private String content;
}
