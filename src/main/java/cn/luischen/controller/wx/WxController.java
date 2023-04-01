package cn.luischen.controller.wx;


import cn.luischen.utils.CheckUtil;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@RestController
public class WxController {

    private static final Logger LOG = LoggerFactory.getLogger(WxController.class);

    @Autowired
    private WxMpService mpService;

    @RequestMapping("/wx/access_token")
    public String test() throws WxErrorException {
        // this.mpService.getWxMpConfigStorage().getAppId();
        return this.mpService.getAccessToken();
    }

    @GetMapping("/wx/signature")
    public void fun(HttpServletRequest request, HttpServletResponse response) {
        // 接收微信服务器以Get请求发送的4个参数
        String signature = request.getParameter("signature");
        String timestamp = request.getParameter("timestamp");
        String nonce = request.getParameter("nonce");
        String echostr = request.getParameter("echostr");

        PrintWriter out = null;
        try {
            out = response.getWriter();
        } catch (IOException e) {
            LOG.error("获取response.out IO异常", e);
        }
        if (CheckUtil.checkSignature(signature, timestamp, nonce)) {
            out.print(echostr);        // 校验通过，原样返回echostr参数内容
        } else {
            LOG.error("不是微信发来的请求!");
        }
    }

}
