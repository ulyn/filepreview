package cn.keking.web.controller;

import org.apache.commons.io.IOUtils;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.keking.service.FileConverQueueTask;
import cn.keking.service.FilePreview;
import cn.keking.service.FilePreviewFactory;
import cn.keking.utils.DownloadUtils;

/**
 * @author yudian-it
 */
@Controller
public class OnlinePreviewController {
    Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    FilePreviewFactory previewFactory;

    @Autowired
    RedissonClient redissonClient;
    @Value("${file.dir}")
    String fileDir;

    /**
     * @param url
     * @param model
     * @return
     */
    @RequestMapping(value = "onlinePreview", method = RequestMethod.GET)
    public String onlinePreview(String url, Model model, HttpServletRequest req) {
        req.setAttribute("fileKey", req.getParameter("fileKey"));
        req.setAttribute("_params_", req.getParameterMap());
        logger.info("fileurl:" + url);
        FilePreview filePreview = previewFactory.get(url);
        return filePreview.filePreviewHandle(url, model);
    }

    /**
     * 多图片切换预览
     *
     * @param model
     * @param req
     * @return
     * @throws UnsupportedEncodingException
     */
    @RequestMapping(value = "picturesPreview", method = RequestMethod.GET)
    public String picturesPreview(String urls, String currentUrl, Model model, HttpServletRequest req) throws UnsupportedEncodingException {
        // 路径转码
        String decodedUrl = URLDecoder.decode(urls, "utf-8");
        String decodedCurrentUrl = URLDecoder.decode(currentUrl, "utf-8");
        // 抽取文件并返回文件列表
        String[] imgs = decodedUrl.split("\\|");
        List imgurls = Arrays.asList(imgs);
        model.addAttribute("imgurls", imgurls);
        model.addAttribute("currentUrl", decodedCurrentUrl);
        return "picture";
    }

    @RequestMapping(value = "picturesPreview", method = RequestMethod.POST)
    public String picturesPreview(Model model, HttpServletRequest req) throws UnsupportedEncodingException {
        String urls = req.getParameter("urls");
        String currentUrl = req.getParameter("currentUrl");
        // 路径转码
        String decodedUrl = URLDecoder.decode(urls, "utf-8");
        String decodedCurrentUrl = URLDecoder.decode(currentUrl, "utf-8");
        // 抽取文件并返回文件列表
        String[] imgs = decodedUrl.split("\\|");
        List imgurls = Arrays.asList(imgs);
        model.addAttribute("imgurls", imgurls);
        model.addAttribute("currentUrl", decodedCurrentUrl);
        return "picture";
    }

    @RequestMapping(value = "/office/{filename:.+}", method = RequestMethod.GET)
    public void getOfficeFiles(@PathVariable String filename, HttpServletResponse resp) throws IOException {
        File file = new File(fileDir + filename);
        logger.info("file:" + file.toURI().getRawPath());
        FileInputStream inputStream = new FileInputStream(file);
        try {
            IOUtils.copy(new FileInputStream(file), resp.getOutputStream());
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    /**
     * 根据url获取文件内容
     * 当pdfjs读取存在跨域问题的文件时将通过此接口读取
     *
     * @param urlPath
     * @param resp
     */
    @RequestMapping(value = "/getCorsFile", method = RequestMethod.GET)
    public void getCorsFile(String urlPath, HttpServletRequest req, HttpServletResponse resp) {
        InputStream inputStream = null;
        try {
            String strUrl = urlPath.trim();
            Map<String, String[]> headParams = new HashMap();
            if (strUrl.indexOf("?") != -1) {
                String pathString = strUrl.substring(0, strUrl.indexOf("?") + 1);
                String queryString = strUrl.substring(strUrl.indexOf("?") + 1);
                String[] params = queryString.split("\\&");
                for (String p : params) {
                    String[] arr = p.split("=");
                    if (arr[0].startsWith("_head_")) {
                        headParams.put(arr[0], new String[]{arr.length == 2 ? arr[1] : ""});
                    } else {
                        pathString += p + "&";
                    }
                }
                strUrl = pathString.substring(0, pathString.length() - 1);
            }
            req.setAttribute("_params_", headParams);
            logger.info("fileurl:" + strUrl);
            URL url = new URL(DownloadUtils.encodeUrlParam(strUrl));
            //打开请求连接
            URLConnection connection = url.openConnection();
            DownloadUtils.trySetHeader(connection);
            HttpURLConnection httpURLConnection = (HttpURLConnection) connection;
            httpURLConnection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
            inputStream = httpURLConnection.getInputStream();
            byte[] bs = new byte[1024];
            int len;
            while (-1 != (len = inputStream.read(bs))) {
                resp.getOutputStream().write(bs, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                IOUtils.closeQuietly(inputStream);
            }
        }
    }

    /**
     * 通过api接口入队
     * @param url 请编码后在入队
     */
    @GetMapping("/addTask")
    @ResponseBody
    public String addQueueTask(String url) {
        final RBlockingQueue<String> queue = redissonClient.getBlockingQueue(FileConverQueueTask.queueTaskName);
        queue.addAsync(url);
        return "success";
    }

}
