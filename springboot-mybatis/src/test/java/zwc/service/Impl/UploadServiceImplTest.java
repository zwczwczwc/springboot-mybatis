package zwc.service.Impl;

import org.apache.poi.xwpf.usermodel.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import zwc.dao.DeleteDao;
import zwc.dao.UploadDao;
import zwc.pojo.Regular;
import zwc.pojo.Store;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * @author zwc
 * 2022-07-05
 * 9:30
 */

@SpringBootTest
public class UploadServiceImplTest {

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

    @Test
    void addStore() throws IOException {

        //设置要匹配的高亮颜色,只需要匹配黄色和蓝色即可
        String[] colors = {
                "yellow", //黄色
                "cyan", //蓝色
        };

        boolean flag = true;
        //根据id读取到对应的模板
        InputStream in = new FileInputStream(uploadFilePath + "2.docx"); //docx文件
        @SuppressWarnings("resource")
        XWPFDocument xdoc = new XWPFDocument(in);
        Iterator<XWPFTable> itTable = xdoc.getTablesIterator();  //获取word文件中的表格

        //如果文档能检测到表格
        if(itTable.hasNext()){
            flag = check_table(itTable, colors, 2);
        }
        //如果文档检测不到表格
        else{
            flag = check_word(xdoc, colors, 2);
        }
    }
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
                            temp.setCheck_id(color);
                            temp.setFile_id(id);
                            temp.setText(Str.toString());
                            temp.setTable_id(tableIndex);
                            //用于检验是否存在方框对号的情况
                            if(Str.toString().contains("☑") || Str.toString().contains("□")){
                                temp.setRegular("☑");
                            }
                            /*if(uploadDao.addStore(temp) < 0){
                                //如果有一个update没有成功直接break返回错误
                                flag = false;
                            }*/
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

    private boolean check_table_word (XWPFTableCell cell, String[] colors, int id, int Rol, int Col, int table_id){

        boolean flag = true;

        List<XWPFParagraph> paras = cell.getParagraphs();

        //用于将整个文本转化为一段内容
        StringBuilder x = new StringBuilder();

        //保存整个纯文档的转义字符
        StringBuilder str = new StringBuilder();

        //检测到有高亮再向数据库内存储
        boolean f = false;

        //用来记录是某单元格文本中第几个高亮的位置
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

                    f = true;

                    StringBuilder temp_text = new StringBuilder(xL.text());
                    String c = xL.getCTR().getRPr().getHighlight().getVal().toString();
                    while(j + 1 < runsLists.size()
                            && runsLists.get(j + 1).getCTR().getRPr().getHighlight() != null
                            && runsLists.get(j).getCTR().getRPr().getHighlight().getVal().equals(runsLists.get(j + 1).getCTR().getRPr().getHighlight().getVal())){
                        temp_text.append(runsLists.get(j + 1).text());
                        j++;
                    }
                    Store temp = new Store();
                    if (Arrays.asList(colors).contains(c)) {
                        temp.setRol(Rol);
                        temp.setCol(Col);
                        temp.setFile_id(id);
                        temp.setCheck_id(c);
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
                    /*if(uploadDao.addStore(temp) < 0){
                        flag = false;
                        break;
                    }*/
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

                //进行段落的划分，防止多个高亮段无法提取高亮位置的情况
                if(str.charAt(str.length() - 1) != 'o'){
                    str.append("o");
                }

            }
                /*//匹配段落之间的换行
                if(str.length() < 7){
                    str.append("[\\s\\S]*");
                }else if(!(str.substring(str.length() - 7,str.length()).equals("[\\s\\S]*"))){
                    str.append("[\\s\\S]*");
                }*/
            x.append("o");
        }

        //存储校验规则到regular表
        if(f){
            Regular temp = new Regular();
            System.out.println(x);
            temp.setRegular(str.toString());
            System.out.println(str);
            temp.setFile_id(id);
            temp.setRol(Rol);
            temp.setCol(Col);
            temp.setTable_id(table_id);
            /*if(uploadDao.addRegular(temp) < 0){
                flag = false;
            }*/
        }
        return flag;
    }

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
                    String c = xL.getCTR().getRPr().getHighlight().getVal().toString();
                    while(j + 1 < runsLists.size()
                            && runsLists.get(j + 1).getCTR().getRPr().getHighlight() != null
                            && runsLists.get(j).getCTR().getRPr().getHighlight().getVal().equals(runsLists.get(j + 1).getCTR().getRPr().getHighlight().getVal())){
                        temp_text.append(runsLists.get(j + 1).text());
                        j++;
                    }
                    Store temp = new Store();
                    if (Arrays.asList(colors).contains(c)) {
                        temp.setFile_id(id);
                        temp.setCheck_id(c);
                        //不管是黄色还是蓝色都存储内容
                        temp.setText(temp_text.toString());
                        temp.setPara_id(num);
                        num++;
                    }
                    //如果有一个update没有成功直接break返回错误
                    if(uploadDao.addStore(temp) < 0){
                        flag = false;
                        break;
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
        }
        return flag;
    }

    //设置要匹配的高亮颜色,只需要匹配黄色和蓝色即可
//        String[] colors = {
//                "yellow", //黄色
//                "cyan", //蓝色
//        };
//        InputStream in_2 = new FileInputStream(uploadFilePath + "2.docx");
//        InputStream in_5 = new FileInputStream(uploadFilePath + "5.docx");
//        XWPFDocument xdoc_5 = new XWPFDocument(in_5);
//        if(check_word(xdoc_5, colors, 5)){
//            System.out.println("success");
//        }
//
//        //根据id读取到对应的模板
//        InputStream in = new FileInputStream(uploadFilePath + "2.docx"); //docx文件
//        @SuppressWarnings("resource")
//        XWPFDocument xdoc = new XWPFDocument(in);
//        Iterator<XWPFTable> itTable = xdoc.getTablesIterator();  //获取word文件中的表格
//
//        //如果文档内容检测不到表格，则直接按多段落文档进行处理
//        if(itTable.hasNext()){
//
//            XWPFTable table;
//            int tableIndex = 0; //表格编号
//            while (itTable.hasNext()) {  //循环word中的每个表格
//
//                table = itTable.next();
//                XWPFTableRow row;
//                List<XWPFTableCell> cells;
//                for (int i = 0; i < table.getNumberOfRows(); i++) {
//                    /*if(i == 0)  //是否略过表头
//                    {
//                        continue;
//                    }*/
//                    row = table.getRow(i);  //获取word表格的每一行
//                    cells = row.getTableCells();  //针对每一行的所有单元格
//                    for (int j = 0; j < cells.size(); j++) {
//                        XWPFTableCell cell = cells.get(j); //获取单个单元格
//                        //获取单元格相同字体颜色+文字
//                        List<XWPFParagraph> paras_mul = cell.getParagraphs(); //获取包含段落的列表
//
//                        //用于存储在未完成判断是否有多段之前存储temp
//                        List<Store> list_temp = new LinkedList<Store>();
//
//                        //如果某个单元格中的段落超过一个则按照文本处理，因为单个单元格无法完成
//                        if(paras_mul.size() == 1){
//                            XWPFParagraph paras = paras_mul.get(0);
//                            List<XWPFRun> runsLists = paras.getRuns();//获取段落中的列表
//                            StringBuilder Str = new StringBuilder();
//
//                            //判断是否存在高亮
//                            boolean flag_color = false;
//                            boolean flag_nocolor = false;
//
//                            //存储颜色字段
//                            StringBuilder color = new StringBuilder();
//
//                            for(XWPFRun xL:runsLists){
//                                if(xL.getCTR().getRPr().getHighlight() != null && Arrays.asList(colors).contains(xL.getCTR().getRPr().getHighlight().getVal().toString())){
//                                    flag_color = true;
//                                    color.append(xL.getCTR().getRPr().getHighlight().getVal().toString());
//                                    Str.append(xL.text());
//                                }else{
////                                    System.out.println("表格中有不为高亮的地方");
//                                    flag_nocolor = true;
//                                    break;
////                                    check_table_word();
//                                }
//                            }
//                            //判断是否为高亮
//                            /*for(int k = 0; k < runsLists.size(); k++){
//                                XWPFRun xL = runsLists.get(k);
//                                //如果检测到高亮文本
//                                if(xL.getCTR().getRPr().getHighlight() != null && Arrays.asList(colors).contains(xL.getCTR().getRPr().getHighlight().getVal().toString())){
//                                    StringBuilder temp_text = new StringBuilder(xL.text());
//                                    String c = xL.getCTR().getRPr().getHighlight().getVal().toString();
//                                    while(k + 1 < runsLists.size()
//                                            && runsLists.get(k + 1).getCTR().getRPr().getHighlight() != null
//                                            && runsLists.get(k).getCTR().getRPr().getHighlight().getVal().equals(runsLists.get(k + 1).getCTR().getRPr().getHighlight().getVal())){
//                                        temp_text.append(runsLists.get(k + 1).text());
//                                        j++;
//                                    }
//                                    //将检测到的高亮文本暂时存放在list中
//                                    Store temp = new Store();
//                                    if (Arrays.asList(colors).contains(c)) {
//                                        System.out.println(temp_text);
//                                        *//*temp.setCheck_id(c);
//                                        //不管是黄色还是蓝色都存储内容
//                                        temp.setText(temp_text.toString());
//                                        temp.setFile_id(id);
//                                        temp.setPara_id(i);*//*
//                                    }
//                                    list_temp.add(temp);
//                                } else if(xL.getCTR().getRPr().getHighlight() == null && !xL.text().equals(" ")){
//                                    f = true;
//                                    //只要检测到不含高亮位置的文本直接跳出循环
//                                    System.out.println(xL.text());
//                                    break;
//                                }
//                            }*/
////                            if(Str.length() > 0)
////                                System.out.println(Str);
//
//                            //如果检测到某个单元格内存在高亮部分和非高亮部分，则将该单元格中的内容作为文本处理
//                            if(flag_nocolor == true){
////                                System.out.println("进行单元格文本检测");
//                                check_table_word(cell, colors, 1, i, j);
////                                check_table_word (cell, colors, 3, i, j);
////                                check_table_word();
//                            }
//                            //如果全部都为高亮，则进行数据库交互
//                            else if(flag_color == true){
//                                System.out.println("完成单个单元格的存储");
//                                /*for(Store temp : list_temp){
//                                    System.out.println("存入数据库中");
//                                    if(uploadDao.addStore(temp) < 0){
//                                        //如果有一个update没有成功直接break返回错误
//                                        flag = false;
//                                        break;
//                                    }
//                                }*/
//                            }
//                        }
//
//                        //当存在多个段落则按照单元格文本进行处理
//                        else{
//                            check_table_word(cell, colors, 1, i, j);
////                            System.out.println();
////                            System.out.println("多个段落进行文本校验");
////                            System.out.println();
////                            check_table_word();
//                        }
//                    }
//                }
//                tableIndex++;
//            }
//            /*List<XWPFParagraph> paras = xdoc.getParagraphs();
//            for(int i = 1; i <= paras.size(); i++){
//                List<XWPFRun> runsLists = paras.get(i - 1).getRuns();//获取段落中的列表
//                for(int j = 1; j <= runsLists.size(); j++){
//                    XWPFRun xL = runsLists.get(j - 1);
//                    *//*if(xL.getCTR().getRPr().getHighlight() != null){
//                        String c = xL.getCTR().getRPr().getHighlight().getVal().toString();
//                        String text = xL.text();
//                        System.out.println(text);
//                        System.out.println(i + "and" + j);
//                        //设置需要进行传递的类
//                        *//**//*Store temp = new Store();
//                        if (c!=null&& Arrays.asList(colors).contains(c)) {
//                            temp.setRol(i);
//                            temp.setCol(j);
//                            temp.setCheck_id(c);
//                            //不管是黄色还是蓝色都存储内容
//                            temp.setText(text);
//                            temp.setFile_id(id);
//                        }
//                        //如果有一个update没有成功直接break返回错误
//                        if(uploadDao.addStore(temp) < 0){
//                            flag = false;
//                            break;
//                        }*//**//*
//                    }*//*
//                }
//                System.out.println(i);
//            }*/
//
//            /*int tableIndex = 0; //表格编号
//
//            while (itTable.hasNext()) {  //循环word中的每个表格
//                System.out.println(tableIndex);
//
//                XWPFTable table;
//                table = itTable.next();
//                XWPFTableRow row;
//                List<XWPFTableCell> cells;
//                for (int i = 0; i < table.getNumberOfRows(); i++) {
//                    row = table.getRow(i);  //获取word表格的每一行
//                    cells = row.getTableCells();  //针对每一行的所有单元格
//                    for (int j = 0; j < cells.size(); j++) {
//                        XWPFTableCell cell = cells.get(j); //获取单个单元格
//                        System.out.println(cell.getText());
//                        //获取单元格相同字体颜色+文字
//                        List<XWPFParagraph> paras_mul = cell.getParagraphs(); //获取包含段落的列表
//                        for(XWPFParagraph paras_ : paras_mul){
//                            StringBuilder Str = new StringBuilder();
//                            List<XWPFRun> runsLists = paras_.getRuns();//获取段落中的列表
//                            for(XWPFRun xL:runsLists){
//                                Str.append(xL.text());
//                                if(xL.getCTR().getRPr().getHighlight() != null){
//
//
//
//                                    //设置需要进行传递的类
//                                   *//* Store temp = new Store();
//                                    if (c!=null&& Arrays.asList(colors).contains(c)) {
//                                        temp.setRol(i);
//                                        temp.setCol(j);
//                                        temp.setCheck_id(c);
//                                        //不管是黄色还是蓝色都存储内容
//                                        temp.setText(text);
//                                        temp.setFile_id(id);
//                                    }
//                                    //如果有一个update没有成功直接break返回错误
//                                    if(uploadDao.addStore(temp) < 0){
//                                        flag = false;
//                                        break;
//                                    }*//*
//                                }
//                            }
//                            System.out.println(Str.toString());
//                        }
//                    }
////                    System.out.println(i);
//                }
//                tableIndex++;
//            }*/
//        }
//        //如果文档内容不能检测到表格
//        else{
//            StringBuilder x = new StringBuilder();
//            List<XWPFParagraph> paras = xdoc.getParagraphs();
//            StringBuilder str = new StringBuilder();
//            for(int i = 0; i < paras.size(); i++){
//                List<XWPFRun> runsLists = paras.get(i).getRuns();//获取段落中的列表
//                x.append(paras.get(i).getText());
//                for(int j = 0; j < runsLists.size(); j++){
//
//                    //防止出现相同高亮的段落分为好几段
//                    XWPFRun xL = runsLists.get(j);
//                    if(xL.getCTR().getRPr().getHighlight() != null){
//                        StringBuilder temp_text = new StringBuilder(xL.text());
//                        String c = xL.getCTR().getRPr().getHighlight().getVal().toString();
//                        System.out.println(c);
//                        while(j + 1 < runsLists.size()
//                                && runsLists.get(j + 1).getCTR().getRPr().getHighlight() != null
//                                && runsLists.get(j).getCTR().getRPr().getHighlight().getVal().equals(runsLists.get(j + 1).getCTR().getRPr().getHighlight().getVal())){
//                            temp_text.append(runsLists.get(j + 1).text());
//                            j++;
//                        }
//                        System.out.println(temp_text.toString());
//                    }
//
//                    if(xL.getCTR().getRPr().getHighlight() != null){
//                        String c = xL.text();
//                        if(str.length() < 5){
//                            str.append("(.*?)");
//                        }else if(!(str.substring(str.length() - 5,str.length()).equals("(.*?)"))){
//                            str.append("(.*?)");
//                        }
//                    }else{
//                        //对转义字符进行转义
//                        if(xL.text().contains("\\")){
//                            StringBuilder s = new StringBuilder(xL.text());
////                            System.out.println(s);
//                            s.insert(s.indexOf("\\") + 1, "\\");
////                            System.out.println(s);
//                            str.append(s);
//                        }else{
//                            str.append(xL.text());
//                        }
//                    }
//                }
//                /*//匹配段落之间的换行
//                if(str.length() < 7){
//                    str.append("[\\s\\S]*");
//                }else if(!(str.substring(str.length() - 7,str.length()).equals("[\\s\\S]*"))){
//                    str.append("[\\s\\S]*");
//                }*/
//            }
//            Pattern pat = Pattern.compile(str.toString());
//            System.out.println(str);
//            Matcher mat = pat.matcher(x.toString());
//            System.out.println(x);
//            if(mat.matches()) {
//                for(int k = 1; k <= mat.groupCount(); k++){
//                    System.out.println(mat.group(k));
//                }
//            }
//        }
//    private boolean check_table_word (XWPFTableCell cell, String[] colors, int id, int Rol, int Col){
//        boolean flag = true;
//
//        List<XWPFParagraph> paras = cell.getParagraphs();
//
//        //用于将整个文本转化为一段内容
//        StringBuilder x = new StringBuilder();
//
//        //保存整个纯文档的转义字符
//        StringBuilder str = new StringBuilder();
//
//        boolean f = false;
//
//        //对于每个段落
//        for(int i = 0; i < paras.size(); i++){
//            List<XWPFRun> runsLists = paras.get(i).getRuns();//获取段落中的列表
//            x.append(paras.get(i).getText());
//
//            //对于段落中的每个内容
//
//            for(int j = 0; j < runsLists.size(); j++){
//
//                XWPFRun xL = runsLists.get(j);
//
//                //防止出现相同高亮的段落分为好几段，进行内容提取
//                if(xL.getCTR().getRPr().getHighlight() != null){
//
//                    f = true;
//
//                    StringBuilder temp_text = new StringBuilder(xL.text());
//                    String c = xL.getCTR().getRPr().getHighlight().getVal().toString();
//                    while(j + 1 < runsLists.size()
//                            && runsLists.get(j + 1).getCTR().getRPr().getHighlight() != null
//                            && runsLists.get(j).getCTR().getRPr().getHighlight().getVal().equals(runsLists.get(j + 1).getCTR().getRPr().getHighlight().getVal())){
//                        temp_text.append(runsLists.get(j + 1).text());
//                        j++;
//                    }
////                    Store temp = new Store();
//                    if (Arrays.asList(colors).contains(c)) {
//
////                        System.out.println(temp_text);
////                        temp.setCheck_id(c);
//                        //不管是黄色还是蓝色都存储内容
////                        temp.setText(temp_text.toString());
////                        temp.setFile_id(id);
////                        temp.setPara_id(i);
////                        temp.setRol(Rol);
////                        temp.setCol(Col);
//                    }
//                    //如果有一个update没有成功直接break返回错误
//                    /*if(uploadDao.addStore(temp) < 0){
//                        flag = false;
//                        break;
//                    }*/
//                }
//
//                //进行正则表达式的转换
//                if(xL.getCTR().getRPr().getHighlight() != null){
//
//                    String reg = "(.*?)";
//                    //编写纯文本校验的正则表达式
//                    if(str.length() < reg.length()){
//                        str.append(reg);
//                    }else if(!(str.substring(str.length() - reg.length(),str.length()).equals(reg))){
//                        str.append(reg);
//                    }
//                }else{
//                    //如果文本内存在转义字符则对其进行转义处理
//                    if(xL.text().contains("\\")){
//                        StringBuilder s = new StringBuilder(xL.text());
//                        s.insert(s.indexOf("\\") + 1, "\\");
//                        str.append(s);
//                    }else{
//                        str.append(xL.text());
//                    }
//                }
//            }
//
//                /*//匹配段落之间的换行
//                if(str.length() < 7){
//                    str.append("[\\s\\S]*");
//                }else if(!(str.substring(str.length() - 7,str.length()).equals("[\\s\\S]*"))){
//                    str.append("[\\s\\S]*");
//                }*/
//        }
//
//        //如果存在高亮则进行存储
//        if(f){
//            System.out.println(x.append("。"));
//            System.out.println(str.append("。"));
//        }
//        //存储校验规则
////        Store temp = new Store();
////        temp.setRegular(str.toString());
////        temp.setFile_id(id);
////        if(uploadDao.addStore(temp) < 0){
////            flag = false;
////        }
//        return flag;
//    }
//
//    private boolean check_word (XWPFDocument xdoc, String[] colors, int id){
//        boolean flag = true;
//
//        List<XWPFParagraph> paras = xdoc.getParagraphs();
//
//        //用于将整个文本转化为一段内容
//        StringBuilder x = new StringBuilder();
//
//        //保存整个纯文档的转义字符
//        StringBuilder str = new StringBuilder();
//
//        //对于每个段落
//        for(int i = 0; i < paras.size(); i++){
//            List<XWPFRun> runsLists = paras.get(i).getRuns();//获取段落中的列表
//            x.append(paras.get(i).getText());
//
//            //对于段落中的每个内容
//            for(int j = 0; j < runsLists.size(); j++){
//
//                XWPFRun xL = runsLists.get(j);
//
//                //防止出现相同高亮的段落分为好几段，进行内容提取
//                if(xL.getCTR().getRPr().getHighlight() != null){
//                    StringBuilder temp_text = new StringBuilder(xL.text());
//                    String c = xL.getCTR().getRPr().getHighlight().getVal().toString();
//                    while(j + 1 < runsLists.size()
//                            && runsLists.get(j + 1).getCTR().getRPr().getHighlight() != null
//                            && runsLists.get(j).getCTR().getRPr().getHighlight().getVal().equals(runsLists.get(j + 1).getCTR().getRPr().getHighlight().getVal())){
//                        temp_text.append(runsLists.get(j + 1).text());
//                        j++;
//                    }
//                    Store temp = new Store();
//                    if (Arrays.asList(colors).contains(c)) {
////                        System.out.println(temp_text);
////                        temp.setCheck_id(c);
////                        //不管是黄色还是蓝色都存储内容
////                        temp.setText(temp_text.toString());
////                        temp.setFile_id(id);
////                        temp.setPara_id(i);
//                    }
//                    //如果有一个update没有成功直接break返回错误
////                    if(uploadDao.addStore(temp) < 0){
////                        flag = false;
////                        break;
////                    }
//                }
//
//                //进行正则表达式的转换
//                if(xL.getCTR().getRPr().getHighlight() != null){
//
//                    String reg = "(.*?)";
//                    //编写纯文本校验的正则表达式
//                    if(str.length() < reg.length()){
//                        str.append(reg);
//                    }else if(!(str.substring(str.length() - reg.length(),str.length()).equals(reg))){
//                        str.append(reg);
//                    }
//                }else{
//                    //如果文本内存在转义字符则对其进行转义处理
//                    if(xL.text().contains("\\")){
//                        StringBuilder s = new StringBuilder(xL.text());
//                        s.insert(s.indexOf("\\") + 1, "\\");
//                        str.append(s);
//                    }else{
//                        str.append(xL.text());
//                    }
//                }
//            }
//                /*//匹配段落之间的换行
//                if(str.length() < 7){
//                    str.append("[\\s\\S]*");
//                }else if(!(str.substring(str.length() - 7,str.length()).equals("[\\s\\S]*"))){
//                    str.append("[\\s\\S]*");
//                }*/
//        }
//
//        System.out.println(str);
//        System.out.println(x);
//        //存储校验规则
////        Store temp = new Store();
////        temp.setRegular(str.toString());
////        temp.setFile_id(id);
////        if(uploadDao.addStore(temp) < 0){
////            flag = false;
////        }
//        return flag;
//    }
}

