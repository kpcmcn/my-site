package cn.luischen.common.handler;

import cn.luischen.common.utils.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import cn.luischen.common.utils.*;
import me.chanjar.weixin.common.session.WxSessionManager;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author andy
 */
@Component
public class LogHandler extends AbstractHandler {

    @Override
    public WxMpXmlOutMessage handle(WxMpXmlMessage wxMessage, Map<String, Object> context, WxMpService wxMpService, WxSessionManager sessionManager) {
        try {
            this.logger.info("\n接收到请求消息，内容：{}", JsonUtil.getJSON().writeValueAsString(wxMessage));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

}
