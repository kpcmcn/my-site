package cn.luischen.common.handler;

import cn.luischen.common.builder.TextBuilder;
import cn.luischen.service.ChatGptService;
import cn.luischen.service.wx.WeiXinService;
import com.google.common.cache.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.api.WxConsts;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.common.session.WxSessionManager;
import me.chanjar.weixin.mp.api.WxMpKefuService;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.kefu.WxMpKefuMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutNewsMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author andy
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MsgHandler extends AbstractHandler {

    private static final String wx_msg_handel_prefix_ = "wx_msg_handel_prefix_";

    @Autowired
    private ChatGptService chatGptService;

    @Autowired
    private RedisTemplate redisTemplate;

    private static final LoadingCache<Long, String> gpt_result_msg_cache = createGuavaCache();

    public static LoadingCache<Long, String> createGuavaCache() {
        return CacheBuilder.newBuilder()
                // 设置并发级别为5，并发级别是指可以同时写缓存的线程数
                .concurrencyLevel(5)
                // 设置写缓存后10秒钟后过期
                .expireAfterWrite(20, TimeUnit.SECONDS)
                // 设置缓存容器的初始容量为8
                .initialCapacity(100)
                // 设置缓存最大容量为10，超过10之后就会按照LRU最近虽少使用算法来移除缓存项
                .maximumSize(1000)
                // 设置缓存的移除通知
                .removalListener(notification -> log.info("本地缓存gpt消息移除, {} was removed, cause is {}", notification.getKey(), notification.getCause()))
                // 指定CacheLoader，在缓存不存在时通过CacheLoader的实现自动加载缓存
                .build(new CacheLoader<Long, String>() {
                    @Override
                    public String load(Long key) throws Exception {
                        log.error("缓存中没有数据, key:{}", key);
                        return "暂无消息！";
                    }
                });
    }

    @Override
    public WxMpXmlOutMessage handle(WxMpXmlMessage wxMessage,
                                    Map<String, Object> context, WxMpService wxMpService,
                                    WxSessionManager sessionManager) {
        WeiXinService weixinService = (WeiXinService) wxMpService;
        if (!wxMessage.getMsgType().equals(WxConsts.XmlMsgType.EVENT)) {
            //TODO 可以选择将消息保存到本地
        }
        //当用户输入关键词如“你好”，“客服”等，并且有客服在线时，把消息转发给在线客服
        if (StringUtils.startsWithAny(wxMessage.getContent(), "你好", "客服")
                && weixinService.hasKefuOnline()) {
            return WxMpXmlOutMessage
                    .TRANSFER_CUSTOMER_SERVICE().fromUser(wxMessage.getToUser())
                    .toUser(wxMessage.getFromUser()).build();
        }

        //TODO 组装回复消息
        switch (wxMessage.getContent()) {
//            case "签到表单":
//                return sendInSignMessage(wxMessage, context, wxMpService, sessionManager);
//            case "发送弹幕":
//                return handleDanmu(wxMessage, context, wxMpService, sessionManager);
//            case "年会节目单":
//                return handleContent(wxMessage, context, wxMpService, sessionManager);
//            case "投票":
//                return handleVote(wxMessage, context, wxMpService, sessionManager);
 /*           case "发送中奖消息":
                return sendLuckyMessage();*/
            default:
                // 默认走chatGPT回复
                String content = defaultHandel(wxMessage);
                return new TextBuilder().build(content, wxMessage, weixinService);
        }
    }

    private String defaultHandel(WxMpXmlMessage wxMessage) {
        // value为null -> 第一次处理消息直接调用gpt
        // value为false -> 已经处理过，但是gpt还没有返回
        // value为true -> 已经处理过，gpt也处理完缓存中有数据
        String key = wx_msg_handel_prefix_ + wxMessage.getMsgId();
        String value = (String) redisTemplate.opsForValue().get(key);
        logger.info("request chatGpt redis-key:{} key-value:{}", key, value);
        if (Objects.isNull(value)) {
            redisTemplate.opsForValue().set(key, "false", Duration.ofSeconds(20));
            String gptResMsg = chatGptService.doChat(wxMessage.getContent());
            // 存到缓存
            gpt_result_msg_cache.put(wxMessage.getMsgId(), gptResMsg);
            redisTemplate.opsForValue().set(key, "true");
            return gptResMsg;
        } else if (StringUtils.equals(value, "false")) {
            // 说明是重试的消息，上次还没处理完，等待
            log.info("重试的消息，上次GPT还没回复，等待不做处理！");
        } else {
            // 说明是重试的消息，从缓存中获取，获取不到
            try {
                return gpt_result_msg_cache.get(wxMessage.getMsgId());
            } catch (ExecutionException e) {
                log.error("从缓存中获取数据异常", e);
            }
        }
        log.error("不会吧！！！太阳从西边出来了！");
        return null;
    }

//    private void sendLuckyMessage(LuckyUser user, WxMpKefuService wxMpKefuService, UserService userService) throws Exception {
//        if (null != user) {
//            if (StringUtils.isBlank(user.getOpenId()) || null == user.getDegree() || StringUtils.isBlank(user.getName())) {
//                throw new ServerInternalException("非法id=" + user.getOpenId() + "，degree=" + user.getDegree() + "，name=" + user.getName());
//            }
//        }
//        String[] degreeList = {"一", "二", "三"};
//        String messageText = weiYaConfig.getPrizeMessage();
//        String format = MessageFormat.format(messageText, user.getName(), degreeList[user.getDegree()]);
//        WxMpKefuMessage message = WxMpKefuMessage.TEXT().content(format).toUser(user.getOpenId()).build();
//        try {
//            userService.saveMessage(user, format);
//            wxMpKefuService.sendKefuMessage(message);
//        } catch (WxErrorException e) {
//            log.error("error", e);
//            throw new ServerInternalException(e);
//        }
//    }

//    public void sendLuckyMessage(List<LuckyUser> luckyUsers, WxMpKefuService wxMpKefuService, UserService userService) throws Exception {
//        for (LuckyUser user : luckyUsers) {
//            this.sendLuckyMessage(user, wxMpKefuService, userService);
//        }
//    }

//    private WxMpXmlOutNewsMessage sendInSignMessage(WxMpXmlMessage wxMessage,
//                                                    Map<String, Object> context, WxMpService wxMpService,
//                                                    WxSessionManager sessionManager) {
//        String signUrl = weiYaConfig.getSignInUrl();
//        String url = MessageFormat.format(signUrl, wxMessage.getFromUser());
//        log.info("签到url:{}", signUrl);
//        WxMpXmlOutNewsMessage.Item item = new WxMpXmlOutNewsMessage.Item();
//        item.setDescription("请点击进去智业尾牙年会签到页面");
//        item.setPicUrl("https://mmbiz.qpic.cn/mmbiz_jpg/B0md6NdhhMRguia0l7AUGZ1mRUzm3ibv9fVqiblSON5VyS6ceAjWLZHGJQ9CnbeUKOOg1xkvQQB4QprfdkLmA9gicw/0?wx_fmt=jpeg");
//        item.setTitle("签到");
//        item.setUrl(url);
//
//        WxMpXmlOutNewsMessage m = WxMpXmlOutMessage.NEWS()
//                .fromUser(wxMessage.getToUser())
//                .toUser(wxMessage.getFromUser())
//                .addArticle(item)
//                .build();
//        return m;
//    }

//    private WxMpXmlOutMessage handleDanmu(WxMpXmlMessage wxMessage, Map<String, Object> context,
//                                          WxMpService wxMpService, WxSessionManager sessionManager) {
//        String pattern = weiYaConfig.getCommentUrl();
//        String url = MessageFormat.format(pattern, wxMessage.getFromUser());
//        log.info("弹幕墙url:{}", pattern);
//        WxMpXmlOutNewsMessage.Item item = new WxMpXmlOutNewsMessage.Item();
//        item.setDescription("点击图文进入弹幕互动");
//        item.setPicUrl("https://mmbiz.qlogo.cn/mmbiz_jpg/rFTQWsGze4G89XqNehSdSBGt1ic6ricfgBfr8ThJnpIIibwpPhGjGrKpraiaNULFLfv238cC3sIxgCYZza6TYLKicBg/0?wx_fmt=jpeg");
//        item.setTitle("评论上墙");
//        item.setUrl(url);
//        WxMpXmlOutNewsMessage m = WxMpXmlOutMessage.NEWS()
//                .fromUser(wxMessage.getToUser())
//                .toUser(wxMessage.getFromUser())
//                .addArticle(item)
//                .build();
//        return m;
//    }

//    private WxMpXmlOutMessage handleContent(WxMpXmlMessage wxMessage, Map<String, Object> context,
//                                            WxMpService wxMpService, WxSessionManager sessionManager) {
//        String url = MessageFormat.format(weiYaConfig.getCardUrl(), wxMessage.getFromUser());
//        WxMpXmlOutNewsMessage.Item item = new WxMpXmlOutNewsMessage.Item();
//        item.setDescription("年会节目单");
//        item.setPicUrl("https://mmbiz.qlogo.cn/mmbiz/bVoOkrvEGHqgetjIc7VcFoCWgLCNaTOnZaXvR9J04EgxMfbm3WM9OreMfTcMcKN8UFkWtDwUbiatU7Qtxsutglg/0?wx_fmt=png");
//        item.setTitle("节目单");
//        item.setUrl(url);
//
//        WxMpXmlOutNewsMessage m = WxMpXmlOutMessage.NEWS()
//                .fromUser(wxMessage.getToUser())
//                .toUser(wxMessage.getFromUser())
//                .addArticle(item)
//                .build();
//        return m;
//    }

//    private WxMpXmlOutMessage handleVote(WxMpXmlMessage wxMessage, Map<String, Object> context,
//                                         WxMpService wxMpService, WxSessionManager sessionManager) {
//        String url = MessageFormat.format(weiYaConfig.getVoteUrl(), wxMessage.getFromUser());
//        WxMpXmlOutNewsMessage.Item item = new WxMpXmlOutNewsMessage.Item();
//        item.setDescription("来投票吧");
//        item.setPicUrl("https://mmbiz.qlogo.cn/mmbiz_jpg/rFTQWsGze4EdewBW92AAD6Ap8ydAQrgBnndVMdAIXB4CmGiaGiassibiaKhWID6icmdMg3kvWSejFd5omyUdjcvb0GA/0?wx_fmt=jpeg");
//        item.setTitle("投票");
//        item.setUrl(url);
//        WxMpXmlOutNewsMessage m = WxMpXmlOutMessage.NEWS()
//                .fromUser(wxMessage.getToUser())
//                .toUser(wxMessage.getFromUser())
//                .addArticle(item)
//                .build();
//        return m;
//    }
}
