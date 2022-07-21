package zwc.controller;

import cn.hutool.core.io.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import zwc.service.Producer.UploadProducer;
import zwc.service.CheckService;
import zwc.service.DeleteService;
import zwc.service.UploadService;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

@Controller
@RequestMapping("file")
@Slf4j
public class FileController {

    @Autowired
    private UploadProducer uploadProducer;

    @Autowired
    private CheckService checkService;

    @Autowired
    private UploadService uploadService;

    @Autowired
    private DeleteService deleteService;

    //代表上传的是待校验文件
    @Value("${check_dir}")
    private String checkFilePath;

    //代表上传的是校验模板
    @Value("${upload_dir}")
    private String uploadFilePath;

    //代表下载文件的路径
    @Value("${download_dir}")
    private String downloadFilePath;

    //校验模板上传
    @ResponseBody
    @PostMapping("/uploadFile")
    public String fileUpload(@RequestParam("file") MultipartFile file, int id) throws JSONException, IOException {
        JSONObject result = new JSONObject();
        if (file.isEmpty()) {
            result.put("error", "空文件!");
            return result.toString();
        }
        // 文件名
        String fileName = file.getOriginalFilename();
        log.info("上传模板名称为:{}，模板id为:{}", fileName, id);

        //用id作为校验文件的文件名，同时也是不同校验文件的标识（每个模板都有自己的id）
        File fileTempObj = new File(uploadFilePath + "/" + id + ".docx");
        //改进：如果该模板存在，则删除并更新数据库，如果想修改文件规则则直接上传文件即可
        if (fileTempObj.exists()) {
            //删除原模板文件
            fileTempObj.delete();
            //删除数据库的原规则
            deleteService.delete(id);
            deleteService.deleteRegular(id);
            deleteService.deleteCache(id);
            deleteService.deleteCacheRegular(id);
            result.put("sucess","模板已存在，删除旧模板完成");
        }

        //写入新模板
        try {
            FileUtil.writeBytes(file.getBytes(), fileTempObj);
        } catch (Exception e) {
            log.error("发生错误: {}", e);
            result.put("error", e.getMessage());
            return result.toString();
        }

        result.put("success", "文件上传成功!");

        //向消息队列发送消息
        uploadProducer.send(String.valueOf(id));

        //进行上传文件的解析
        if(uploadService.addStore(id)){
            result.put("sucess","文件解析成功！");
        }else{
            result.put("fail", "文件解析失败");
        }
        return result.toString();
    }

    //校验文件上传与校验,同时需要输入需要校对的模板id号
    @ResponseBody
    @PostMapping("/checkFile")
    public String fileCheck(@RequestParam("file") MultipartFile file, int id) throws JSONException, IOException {
        JSONObject result = new JSONObject();
        if (file.isEmpty()) {
            result.put("error", "空文件!");
            return result.toString();
        }

        // 文件名
        String fileName = file.getOriginalFilename();
        log.info("上传文件名称为:{},选择的校验模板id为{}", fileName, id);
        File fileTempObj = new File(checkFilePath + "/" + fileName);

        // 使用文件名称检测文件是否已经存在,如果存在则要进行删除
        if (fileTempObj.exists()) {
            fileTempObj.delete();
        }
        try {
            FileUtil.writeBytes(file.getBytes(), fileTempObj);
        } catch (Exception e) {
            log.error("发生错误: {}", e);
            result.put("error", e.getMessage());
            return result.toString();
        }
        result.put("success", "文件上传成功!");

        //调用service生成TXT文档
        checkService.liststores(fileName, id);

        result.put("sucess","文件校验完成！可以返回下载校验文件");

        return result.toString();
    }

    //TXT文档下载功能
    @ResponseBody
    @GetMapping("/downloadFile")
    public String fileDownload(HttpServletResponse response) throws JSONException, IOException {

        //提供文档下载服务
        JSONObject result = new JSONObject();
        File file = new File(downloadFilePath + "result");
        if (!file.exists()) {
            result.put("error", "下载文件不存在!");
            return result.toString();
        }
        response.reset();
        response.setContentType("application/octet-stream");
        response.setCharacterEncoding("utf-8");
        response.setContentLength((int) file.length());
        response.setHeader("Content-Disposition", "attachment;filename=" + "result");
        byte[] readBytes = FileUtil.readBytes(file);
        OutputStream os = response.getOutputStream();
        os.write(readBytes);
        result.put("success", "下载成功!");
        return result.toString();
    }
}
