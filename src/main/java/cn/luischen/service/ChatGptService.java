package cn.luischen.service;

import cn.luischen.utils.JsonUtil;
import com.plexpt.chatgpt.ChatGPT;
import com.plexpt.chatgpt.util.Proxys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.Proxy;

@Service
public class ChatGptService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${chatGpt.apikey}")
    private String apikey;

    @Value("${chatGpt.timeout}")
    private Long timeout;

    private String maxTokens;

    private String model;

    private String temperature;

    private String sessionClearToken;

    public String doChat(String msg) {
        Proxy proxy = Proxys.http("127.0.0.1", 7890);
        ChatGPT chatGPT = ChatGPT.builder()
                .apiKey(apikey)
                .timeout(30)
                .proxy(proxy)
                .apiHost("https://api.openai.com/") //反向代理地址
                .build()
                .init();
        logger.info("chatGPT request info: {}", JsonUtil.of(chatGPT));
        return chatGPT.chat(msg);
    }

}
