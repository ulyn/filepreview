package cn.keking.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import cn.keking.service.FilePreview;

/**
 * Created by kl on 2018/1/17.
 * Content :处理pdf文件
 */
@Service
public class PdfFilePreviewImpl implements FilePreview {

    @Override
    public String filePreviewHandle(String url, Model model) {
        //将一些请求头参数带上
        Map<String, String[]> parameterMap = (Map<String, String[]>) RequestContextHolder.currentRequestAttributes()
            .getAttribute("_params_", RequestAttributes.SCOPE_REQUEST);
        String otherParams = "";
        if (parameterMap != null) {
            for (String key : parameterMap.keySet()) {
                if (key.startsWith("_head_")) {
                    otherParams += (key + "=" + parameterMap.get(key)[0] + "&");
                }
            }
        }
        if (url.indexOf("?") == -1) {
            url += "?";
        } else {
            url += "&";
        }
        model.addAttribute("pdfUrl", url + otherParams);
        return "pdf";
    }
}
