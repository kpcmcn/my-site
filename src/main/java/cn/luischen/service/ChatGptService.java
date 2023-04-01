package cn.luischen.service;

import com.plexpt.chatgpt.ChatGPT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ChatGptService {

    @Value("${chatGpt.apikey}")
    private String apikey;

    @Value("${chatGpt.timeout}")
    private Long timeout;

    private String maxTokens;

    private String model;

    private String temperature;

    private String sessionClearToken;

    public String doChat(String msg) {
        ChatGPT chatGPT = ChatGPT.builder()
                .apiKey(apikey)
                .timeout(timeout)
                .apiHost("https://api.openai.com/") //反向代理地址
                .build()
                .init();

        return chatGPT.chat(msg);
    }

}
