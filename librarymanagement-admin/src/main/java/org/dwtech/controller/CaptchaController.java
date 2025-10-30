package org.dwtech.controller;

import com.google.code.kaptcha.Producer;
import jakarta.servlet.ServletOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.dwtech.common.config.LBConfig;
import org.dwtech.common.constant.CacheConstants;
import org.dwtech.common.core.controller.BaseController;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.dwtech.common.core.redis.RedisCache;
import org.dwtech.common.utils.uuid.IdUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 图片验证码（支持算术形式）
 *
 */
@Slf4j
@Controller
public class CaptchaController extends BaseController {
    @Resource(name = "captchaProducer")
    private Producer captchaProducer;

    @Resource(name = "captchaProducerMath")
    private Producer captchaProducerMath;

    private final RedisCache redisCache;

    private final LBConfig lbConfig;

    public CaptchaController(RedisCache redisCache, LBConfig lbConfig) {
        this.redisCache = redisCache;
        this.lbConfig = lbConfig;
    }

    /**
     * 验证码生成
     */
    @GetMapping(value = "/captchaImage")
    public ModelAndView getKaptchaImage(HttpServletResponse response) throws IOException {

        ServletOutputStream out = null;
        try {
            response.setDateHeader("Expires", 0);
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            response.addHeader("Cache-Control", "post-check=0, pre-check=0");
            response.setHeader("Pragma", "no-cache");
            response.setContentType("image/jpeg");

            String captchaType = lbConfig.getCaptchaType();
            Integer captchaExpiration = lbConfig.getCaptchaExpiration();
            String uuid = IdUtils.simpleUUID();
            String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + uuid;

            response.setHeader("Authorization", verifyKey);

            String capStr, code = null;
            BufferedImage image = null;

            if ("math".equals(captchaType)) {
                String capText = captchaProducerMath.createText();
                capStr = capText.substring(0, capText.lastIndexOf("@"));
                code = capText.substring(capText.lastIndexOf("@") + 1);
                image = captchaProducerMath.createImage(capStr);
            } else if ("char".equals(captchaType)) {
                capStr = code = captchaProducer.createText();
                image = captchaProducer.createImage(capStr);
            }

            log.info("uuid：{}｜验证码：{}", verifyKey, code);

            redisCache.setCacheObject(verifyKey, code, captchaExpiration, TimeUnit.MINUTES);

            out = response.getOutputStream();
            ImageIO.write(image, "jpg", out);
            out.flush();
        } catch (Exception e) {
            log.error("e: ", e);
        } finally {
            if (out != null) {
                out.close();
            }
        }

        return null;
    }
}