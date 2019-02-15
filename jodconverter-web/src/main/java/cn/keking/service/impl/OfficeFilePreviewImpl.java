package cn.keking.service.impl;

import com.sunsharing.component.utils.crypto.Md5;

import cn.keking.model.FileAttribute;
import cn.keking.model.ReturnResponse;
import cn.keking.service.FilePreview;
import cn.keking.utils.DownloadUtils;
import cn.keking.utils.FileUtils;
import cn.keking.utils.OfficeToPdf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;

import java.io.File;

/**
 * Created by kl on 2018/1/17.
 * Content :处理office文件
 */
@Service
public class OfficeFilePreviewImpl implements FilePreview {

    Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    FileUtils fileUtils;

    @Value("${file.dir}")
    String fileDir;

    @Autowired
    DownloadUtils downloadUtils;

    @Autowired
    private OfficeToPdf officeToPdf;

    @Override
    public String filePreviewHandle(String url, Model model) {
        logger.info("filePreviewHandle : " + url);
        FileAttribute fileAttribute=fileUtils.getFileAttribute(url);
        String suffix=fileAttribute.getSuffix();
        String fileName=fileAttribute.getName();
        String decodedUrl=fileAttribute.getDecodedUrl();
        boolean isHtml = suffix.equalsIgnoreCase("xls") || suffix.equalsIgnoreCase("xlsx");
        logger.info("fileName : " + fileName);
        String pdfName = Md5.MD5(url) + "." + (isHtml ? "html" : "pdf");
        logger.info("pdfName : " + pdfName);
        // 判断之前是否已转换过，如果转换过，直接返回，否则执行转换
        // if (!fileUtils.listConvertedFiles().containsKey(pdfName)) {
            String filePath = fileDir + fileName;
            if (!new File(filePath).exists()) {
                ReturnResponse<String> response = downloadUtils.downLoad(decodedUrl, suffix, null);
                if (0 != response.getCode()) {
                        model.addAttribute("msg", response.getMsg());
                    return "fileNotSupported";
                }
                filePath = response.getContent();
            }
            String outFilePath = fileDir + pdfName;
            if (StringUtils.hasText(outFilePath)) {
                logger.info("openOfficeToPDF outFilePath：" + outFilePath);
                officeToPdf.openOfficeToPDF(filePath, outFilePath);
                File f = new File(filePath);
                if (f.exists()) {
                    f.delete();
                }
                if (isHtml) {
                    // 对转换后的文件进行操作(改变编码方式)
                    fileUtils.doActionConvertedFile(outFilePath);
                }
                // 加入缓存
                fileUtils.addConvertedFile(pdfName, fileUtils.getRelativePath(outFilePath));
            }
        // }
        model.addAttribute("pdfUrl", "office/" + pdfName);
        return isHtml ? "html" : "pdf";
    }
}
