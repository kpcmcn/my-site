package cn.luischen.common.handler;

import cn.luischen.common.builder.AbstractBuilder;
import cn.luischen.common.builder.ImageBuilder;
import cn.luischen.common.builder.TextBuilder;
import cn.luischen.common.dto.WxMenuKey;
import cn.luischen.service.wx.WeiXinService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.session.WxSessionManager;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import org.springframework.stereotype.Component;

import java.util.Map;

import static me.chanjar.weixin.common.api.WxConsts.XmlMsgType;

/**
 * @author andy
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MenuHandler extends AbstractHandler {
    private final MsgHandler msgHandler;

    @Override
    public WxMpXmlOutMessage handle(WxMpXmlMessage wxMessage, Map<String, Object> context, WxMpService wxMpService, WxSessionManager sessionManager) {
        WeiXinService weixinService = (WeiXinService) wxMpService;
        String key = wxMessage.getEventKey();
        WxMenuKey menuKey;
        try {
            menuKey = gson.fromJson(key, WxMenuKey.class);
        } catch (Exception e) {
            log.error("menu event error", e);
            return WxMpXmlOutMessage.TEXT().content(key)
                    .fromUser(wxMessage.getToUser())
                    .toUser(wxMessage.getFromUser()).build();
        }
        AbstractBuilder builder = null;
        switch (menuKey.getType()) {
            case XmlMsgType.TEXT:
                builder = new TextBuilder();
                break;
            case XmlMsgType.IMAGE:
                builder = new ImageBuilder();
                break;
            case XmlMsgType.VOICE:
                break;
            case XmlMsgType.VIDEO:
                break;
            case XmlMsgType.NEWS:
                wxMessage.setContent(menuKey.getContent());
                return msgHandler.handle(wxMessage, context, wxMpService, sessionManager);
            default:
                break;
        }
        if (builder != null) {
            try {
                return builder.build(menuKey.getContent(), wxMessage, weixinService);
            } catch (Exception e) {
                this.logger.error(e.getMessage(), e);
            }
        }
        return null;
    }
}
