package zwc.service.Impl;

import org.apache.poi.xwpf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import zwc.dao.DeleteDao;
import zwc.dao.UploadDao;
import zwc.pojo.Regular;
import zwc.pojo.Store;
import zwc.service.UploadService;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

@Service
public class UploadServiceImpl implements UploadService {

    @Autowired
    public RedisTemplate redisTemplate;

    @Autowired
    private UploadDao uploadDao;

    @Autowired
    private DeleteDao deleteDao;

    @Value("${check_dir}")
    private String checkFilePath;

    @Value("${upload_dir}")
    private String uploadFilePath;

    @Value("${download_dir}")
    private String downloadFilePath;

    //查询表中的元组
    @Override
    public boolean addStore(int id) throws IOException {

        //设置要匹配的高亮颜色,只需要匹配黄色和蓝色即可
        String[] colors = {
                "yellow", //黄色
                "cyan", //蓝色
        };

        boolean flag = true;
        //根据id读取到对应的模板
        InputStream in = new FileInputStream(uploadFilePath + id + ".docx"); //docx文件
        @SuppressWarnings("resource")
        XWPFDocument xdoc = new XWPFDocument(in);
        Iterator<XWPFTable> itTable = xdoc.getTablesIterator();  //获取word文件中的表格

        //如果文档能检测到表格
        if(itTable.hasNext()){
            flag = check_table(itTable, colors, id);
        }
        //如果文档检测不到表格
        else{
            flag = check_word(xdoc, colors, id);
        }
        return flag;
        /*for(int i = 0; i < paras.size(); i++){
                List<XWPFRun> runsLists = paras.get(i).getRuns();//获取段落中的列表
                for(int j = 0; j < runsLists.size(); j++){
                    XWPFRun xL = runsLists.get(j);
                    if(xL.getCTR().getRPr().getHighlight() != null){
                        String c = xL.getCTR().getRPr().getHighlight().getVal().toString();
                        String text = xL.text();

                        //设置需要进行传递的类
                        Store temp = new Store();
                        if (c!=null&&Arrays.asList(colors).contains(c)) {
                            temp.setRol(i);
                            temp.setCol(j);
                            temp.setCheck_id(c);
                            //不管是黄色还是蓝色都存储内容
                            temp.setText(text);
                            temp.setFile_id(id);
                        }
                        //如果有一个update没有成功直接break返回错误
                        if(uploadDao.addStore(temp) < 0){
                            flag = false;
                            break;
                        }
                    }
                }
            }*/
    }

    //检验表格
    private boolean check_table (Iterator<XWPFTable> itTable, String[] colors, int id){

        boolean flag = true;

        XWPFTable table;
        int tableIndex = 0; //表格编号
        while (itTable.hasNext()) {  //循环word中的每个表格

            table = itTable.next();
            XWPFTableRow row;

            List<XWPFTableCell> cells;
            for (int i = 0; i < table.getNumberOfRows(); i++) {
                    /*if(i == 0)  //是否略过表头
                    {
                        continue;
                    }*/

                //获取word表格的每一行
                row = table.getRow(i);

                //针对每一行的所有单元格
                cells = row.getTableCells();

                for (int j = 0; j < cells.size(); j++) {

                    //获取单个单元格
                    XWPFTableCell cell = cells.get(j);

                    //获取包含段落的列表
                    List<XWPFParagraph> paras_mul = cell.getParagraphs();
                    //如果某个单元格中的段落超过一个则按照文本处理，因为单个单元格无法完成
                    if(paras_mul.size() == 1){
                        XWPFParagraph paras = paras_mul.get(0);
                        List<XWPFRun> runsLists = paras.getRuns();//获取段落中的列表
                        StringBuilder Str = new StringBuilder();

                        //判断是否存在高亮
                        boolean flag_color = false;
                        boolean flag_nocolor = false;

                        //存储颜色字段
                        String color = "";

                        for(XWPFRun xL:runsLists){
                            if(xL.getCTR().getRPr().getHighlight() != null && Arrays.asList(colors).contains(xL.getCTR().getRPr().getHighlight().getVal().toString())){
                                flag_color = true;
                                color = xL.getCTR().getRPr().getHighlight().getVal().toString();
                                Str.append(xL.text());
                            }else{
                                //表格中有不为高亮的地方
                                flag_nocolor = true;
                                break;
                            }
                        }

                        //如果检测到某个单元格内存在高亮部分和非高亮部分，则将该单元格中的内容作为文本处理
                        if(flag_nocolor == true){
                            flag = check_table_word(cell, colors, id, i, j, tableIndex);
                        }
                        //如果全部都为高亮，则进行数据库交互
                        else if(flag_color == true){
                            Store temp = new Store();
                            temp.setRol(i);
                            temp.setCol(j);
                            if(color.equals("cyan")){
                                temp.setCheck_id("not-null");
                            }else{
                                temp.setCheck_id("specific-context");
                            }
                            temp.setFile_id(id);
                            temp.setText(Str.toString());
                            temp.setTable_id(tableIndex);
                            temp.setPara_id(-1);
                            //用于检验是否存在方框对号的情况
                            if(Str.toString().contains("☑") || Str.toString().contains("□")){
                                temp.setRegular("☑");
                            }
                            if(uploadDao.addStore(temp) < 0){
                                //如果有一个update没有成功直接break返回错误
                                flag = false;
                                break;
                            }else{
                                redisTemplate.opsForList().rightPush("Store" + id, temp);
                            }
                        }
                    }

                    //当存在多个段落则按照单元格文本进行处理
                    else{
                        flag = check_table_word(cell, colors, id, i, j, tableIndex);
                    }
                }
            }
            tableIndex++;
        }
        return flag;
    }

    //检验纯文本
    private boolean check_word (XWPFDocument xdoc, String[] colors, int id){
        boolean flag = true;

        List<XWPFParagraph> paras = xdoc.getParagraphs();

        //用于将整个文本转化为一段内容
        StringBuilder x = new StringBuilder();

        //保存整个纯文档的转义字符
        StringBuilder str = new StringBuilder();

        int num = 0;

        //对于每个段落
        for(int i = 0; i < paras.size(); i++){
            List<XWPFRun> runsLists = paras.get(i).getRuns();//获取段落中的列表
            x.append(paras.get(i).getText());

            //对于段落中的每个内容
            for(int j = 0; j < runsLists.size(); j++){

                XWPFRun xL = runsLists.get(j);

                //防止出现相同高亮的段落分为好几段，进行内容提取
                if(xL.getCTR().getRPr().getHighlight() != null){
                    StringBuilder temp_text = new StringBuilder(xL.text());
                    String color = xL.getCTR().getRPr().getHighlight().getVal().toString();
                    while(j + 1 < runsLists.size()
                            && runsLists.get(j + 1).getCTR().getRPr().getHighlight() != null
                            && runsLists.get(j).getCTR().getRPr().getHighlight().getVal().equals(runsLists.get(j + 1).getCTR().getRPr().getHighlight().getVal())){
                        temp_text.append(runsLists.get(j + 1).text());
                        j++;
                    }
                    Store temp = new Store();
                    if (Arrays.asList(colors).contains(color)) {
                        temp.setFile_id(id);
                        if(color.equals("cyan")){
                            temp.setCheck_id("not-null");
                        }else{
                            temp.setCheck_id("specific-context");
                        }
                        //不管是黄色还是蓝色都存储内容
                        temp.setText(temp_text.toString());
                        temp.setPara_id(num);
                        num++;
                    }
                    //如果有一个update没有成功直接break返回错误
                    if(uploadDao.addStore(temp) < 0){
                        flag = false;
                        break;
                    }else{
                        redisTemplate.opsForList().rightPush("Store" + id, temp);
                    }
                }

                //进行正则表达式的转换
                if(xL.getCTR().getRPr().getHighlight() != null){

                    String reg = "(.*?)";
                    //编写纯文本校验的正则表达式
                    if(str.length() < reg.length()){
                        str.append(reg);
                    }else if(!(str.substring(str.length() - reg.length(),str.length()).equals(reg))){
                        str.append(reg);
                    }
                }else{
                    //如果文本内存在转义字符则对其进行转义处理
                    if(xL.text().contains("\\")){
                        StringBuilder s = new StringBuilder(xL.text());
                        s.insert(s.indexOf("\\") + 1, "\\");
                        str.append(s);
                    }else{
                        str.append(xL.text());
                    }
                }
            }
            //防止出现多个段落的高亮连接在一起的情况
            if(str.charAt(str.length() - 1) != 'o'){
                str.append("o");
            }
                /*//匹配段落之间的换行
                if(str.length() < 7){
                    str.append("[\\s\\S]*");
                }else if(!(str.substring(str.length() - 7,str.length()).equals("[\\s\\S]*"))){
                    str.append("[\\s\\S]*");
                }*/
        }

        //存储校验规则
        Regular temp = new Regular();
        temp.setRegular(str.toString());
        temp.setFile_id(id);
        if(uploadDao.addRegular(temp) < 0){
            flag = false;
        }else{
            redisTemplate.opsForList().rightPush("Regular" + id,temp);
        }
        return flag;
    }

    //检验表格中的某个单元格的文本
    private boolean check_table_word (XWPFTableCell cell, String[] colors, int id, int Rol, int Col, int table_id){

        boolean flag = true;

        List<XWPFParagraph> paras = cell.getParagraphs();

        //用于将整个文本转化为一段内容
        StringBuilder x = new StringBuilder();

        //保存整个纯文档的正则表达式
        StringBuilder str = new StringBuilder();

        //检测到有高亮再向数据库内存储
        boolean f = false;

        //用来记录是某单元格文本中第几个高亮的位置
        int num = 0;

        //对于每个段落
        for(int i = 0; i < paras.size(); i++){
            List<XWPFRun> runsLists = paras.get(i).getRuns();//获取段落中的列表
            x.append(paras.get(i).getText());

            //用来判断整个段落是否都是高亮
            boolean ishigh = true;
            //对于段落中的每个内容
            for(int j = 0; j < runsLists.size(); j++){

                XWPFRun xL = runsLists.get(j);

                //只要这个段落中任意一个不是高亮，则不会有逻辑联系上的段落情况
                if(xL.getCTR().getRPr().getHighlight() == null){
                    ishigh = false;
                }

                //防止出现相同高亮的段落分为好几段，进行内容提取
                else{

                    f = true;

                    StringBuilder temp_text = new StringBuilder(xL.text());
                    String color = xL.getCTR().getRPr().getHighlight().getVal().toString();

                    //防止多个高亮的run被分散为多个校验规则
                    while(j + 1 < runsLists.size()){
                        if(runsLists.get(j + 1).getCTR().getRPr().getHighlight() != null
                                && runsLists.get(j).getCTR().getRPr().getHighlight().getVal().equals(runsLists.get(j + 1).getCTR().getRPr().getHighlight().getVal())){
                            temp_text.append(runsLists.get(j + 1).text());
                            j++;
                        }
                        else{
                            ishigh = false;
                            break;
                        }
                    }

                    //如果前一个段落全部都是高亮
                    if(ishigh){
                        while(i + 1 < paras.size() && ishigh){
                            StringBuilder str_temp = new StringBuilder();
                            XWPFParagraph para_temp = paras.get(i + 1);
                            List<XWPFRun> runslist_temp = para_temp.getRuns();

                            for(XWPFRun run_temp : runslist_temp){
                                if(run_temp.getCTR().getRPr().getHighlight() != null &&
                                        (run_temp.getCTR().getRPr().getHighlight().getVal().toString().equals(color))){
                                    str_temp.append(run_temp.text());
                                } else if(run_temp.text().trim().equals("")){
                                    //如果段落之间有空格则跳过
                                    continue;
                                } else{
                                    ishigh = false;
                                    break;
                                }
                            }

                            //如果下一段还是全部高亮，则添加到字符串中
                            if(ishigh){
                                temp_text.append(str_temp);
                                i++;
                            }
                        }
                    }

                    //防止具有相同逻辑关系的多个段落被分散为单独的校验规则
                    Store temp = new Store();
                    if (Arrays.asList(colors).contains(color)) {
                        temp.setRol(Rol);
                        temp.setCol(Col);
                        temp.setFile_id(id);
                        if(color.equals("cyan")){
                            temp.setCheck_id("not-null");
                        }else{
                            temp.setCheck_id("specific-context");
                        }
                        //不管是黄色还是蓝色都存储内容
                        temp.setText(temp_text.toString());
                        temp.setTable_id(table_id);
                        temp.setPara_id(num);
                        //代表存在方框对号需要单独处理
                        if(temp_text.toString().contains("☑") || temp_text.toString().contains("□")){
                            temp.setRegular("☑");
                        }
                        num++;
                    }
                    //如果有一个update没有成功直接break返回错误
                    if(uploadDao.addStore(temp) < 0){
                        flag = false;
                        break;
                    }else{
                        redisTemplate.opsForList().rightPush("Store" + id, temp);
                    }
                }

                //进行正则表达式的转换
                if(xL.getCTR().getRPr().getHighlight() != null){

                    String reg = "(.*?)";
                    //编写纯文本校验的正则表达式
                    if(str.length() < reg.length()){
                        str.append(reg);
                    }else if(!(str.substring(str.length() - reg.length(),str.length()).equals(reg))){
                        str.append(reg);
                    }
                }else{
                    //如果文本内存在转义字符则对其进行转义处理
                    if(xL.text().contains("\\")){
                        StringBuilder s = new StringBuilder(xL.text());
                        s.insert(s.indexOf("\\") + 1, "\\");
                        str.append(s);
                    }else{
                        str.append(xL.text());
                    }
                }
            }
            //进行段落的划分，防止出现高亮位置在最后没办法提取的情况
            if(str.charAt(str.length() - 1) != 'o'){
                str.append("o");
            }
                /*//匹配段落之间的换行
                if(str.length() < 7){
                    str.append("[\\s\\S]*");
                }else if(!(str.substring(str.length() - 7,str.length()).equals("[\\s\\S]*"))){
                    str.append("[\\s\\S]*");
                }*/
        }

        //存储校验规则到regular表
        if(f){
            Regular temp = new Regular();
            temp.setRegular(str.toString());
            temp.setFile_id(id);
            temp.setRol(Rol);
            temp.setCol(Col);
            temp.setTable_id(table_id);
            if(uploadDao.addRegular(temp) < 0){
                flag = false;
            }else{
                redisTemplate.opsForList().rightPush("Regular" + id, temp);
            }
        }
        return flag;
    }
}
