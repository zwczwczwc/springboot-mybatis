package zwc.service.Impl;

import org.apache.poi.xwpf.usermodel.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import zwc.dao.CheckDao;
import zwc.pojo.Regular;
import zwc.pojo.Store;

import javax.annotation.Resource;
import java.io.*;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author zwc
 * 2022-06-29
 * 16:55
 */

@SpringBootTest
class CheckServiceImplTest {

    @Resource
    private CheckDao checkDao;

    @Value("${check_dir}")
    private String checkFilePath;

    @Value("${upload_dir}")
    private String uploadFilePath;

    @Value("${download_dir}")
    private String downloadFilePath;

    @Test
    void liststores() throws IOException {
        //从数据库中提取出所有的数据准备进行校验
        List<Store> liststores = checkDao.liststores(2);

        List<Regular> listRegular = checkDao.listregulars(2);

        //读取下载到data的文件
        InputStream in = new FileInputStream(uploadFilePath + "2.docx");

        //创建需要输出的TXT文档
//        creatTxtFile("result");

        @SuppressWarnings("resource")
        XWPFDocument xdoc = new XWPFDocument(in);
        //获取word文件中的表格
        Iterator<XWPFTable> itTable = xdoc.getTablesIterator();
        XWPFTable table = null;

        //如果能检测到表格
        if (itTable.hasNext()) {
            check_table(itTable, table, liststores, listRegular);
        }
        //如果检测不到表格
        else{
            check_word(liststores, listRegular, xdoc);
        }
    }
    //检验表格
    private void check_table(Iterator<XWPFTable> itTable, XWPFTable table, List<Store> list, List<Regular> listRegular){
        //word中表格编号
        int tableIndex = 0;

        //按照表格顺序一一进行检查
        while (itTable.hasNext()) {
            XWPFTableRow row = null;
            List<XWPFTableCell> cells = null;
            table = itTable.next();

            //检验普通单元格
            for (int i = 0; i < list.size(); i++) {
                //按照得到的store一一进行校验
                Store x = list.get(i);
                //按照表格顺序一一进行处理，处理正常的单元格
                if(x.getTable_id() == tableIndex && x.getPara_id() == null){
                    //对于普通单元格
                    int Rol = x.getRol();
                    int Col = x.getCol();

                    //获取word表格对应的单元格
                    row = table.getRow(Rol);

                    //针对每一行的所有单元格
                    cells = row.getTableCells();

                    //获取单个单元格
                    XWPFTableCell cell = cells.get(Col);

                    //进行单元格检查
                    check_table_noword(row, table, cell, x);
                }
            }

            //检验需要进行文本检测的单元格
            for(int i = 0; i < listRegular.size(); i++){
                Regular r = listRegular.get(i);
                if(r.getTable_id() == tableIndex){
                    int Rol = r.getRol();
                    int Col = r.getCol();

                    //获取word表格对应的单元格
                    row = table.getRow(Rol);

                    //针对每一行的所有单元格
                    cells = row.getTableCells();

                    //获取单个单元格
                    XWPFTableCell cell = cells.get(Col);

                    //按照需要进行校验的单元格进行校验
                    check_table_word(cell, r, list);
                }
            }
            tableIndex++;
        }
    }

    //检验表格某单元格中存在的多段文档
    private void check_table_word(XWPFTableCell cell, Regular regular, List<Store> list) {

        //获取正则表达式
        int Rol = regular.getRol();
        int Col = regular.getCol();
        int table_id = regular.getTable_id();

        //将符合要求的store筛选出来
        List<Store> liststores = new LinkedList<>();
        for (Store store : list) {
            if (store.getRol() == Rol && store.getCol() == Col && store.getTable_id() == table_id) {
                liststores.add(store);
            }
        }

        //将cell中的内容转化为整段的文字
        List<XWPFParagraph> paras = cell.getParagraphs();
        StringBuilder text = new StringBuilder();
        for(int i = 0; i < paras.size(); i++) {
            text.append(paras.get(i).getText());
            //添加分隔符
            if(text.charAt(text.length() - 1) != 'o'){
                text.append("o");
            }
        }
        /*System.out.println(text);
        System.out.println(regular.getRegular());*/

        //按照正则表达式提取出需要比对的内容
        Pattern pat = Pattern.compile(regular.getRegular());
        Matcher mat = pat.matcher(text.toString());

        if(mat.matches()) {
            /*for(int i = 0; i < mat.groupCount(); i++){
                System.out.println(mat.group());
            }*/
            for(Store store : liststores){
                //找到每个高亮内容的匹配项
                int num = store.getPara_id() + 1;
                if(store.getCheck_id().equals("yellow")){
                    if (!mat.group(num).equals(store.getText())) {
                        writeTxtFile("第" + (store.getRol() + 1) + "行" + "第" + (store.getCol() + 1) + "列" + "第" + (store.getPara_id() + 1) + "处" + "所填内容不为指定内容", "result");
                    }
                }else{
                    if(mat.group(num).equals("")){
//                        System.out.println("第" + store.getRol() + "行" + "第" + store.getCol() + "列" + "第" + store.getPara_id() + "处" + "存在未填选项");
                        writeTxtFile("第" + (store.getRol() + 1) + "行" + "第" + (store.getCol() + 1) + "列" + "第" + (store.getPara_id() + 1) + "处" + "存在未填选项", "result");
                    }
                    else if(store.getRegular() != null){
                        if(!mat.group(num).contains("☑")){
//                            System.out.println("第" + store.getRol() + "行" + "第" + store.getCol() + "列" + "第" + store.getPara_id() + "处" + "存在未填选项");
                            writeTxtFile("第" + (store.getRol() + 1) + "行" + "第" + (store.getCol() + 1) + "列" + "第" + (store.getPara_id() + 1) + "处" + "存在未填选项", "result");
                        }
                    }
                }
            }
        }
    }

    //校验纯单元格
    private void check_table_noword(XWPFTableRow row, XWPFTable table, XWPFTableCell cell, Store x){

        int Rol = x.getRol();
        int Col = x.getCol();
        String check_id = x.getCheck_id();
        String check_text = x.getText();
        String regular = x.getRegular();

        //获取包含段落的列表
        List<XWPFParagraph> paras_mul = cell.getParagraphs();

        StringBuilder Str = new StringBuilder();
        for(XWPFParagraph paras : paras_mul) {

            //获取段落中的列表
            List<XWPFRun> runsLists = paras.getRuns();
            for (XWPFRun xL : runsLists) {
                Str.append(xL.text());
            }
        }

        //如果检查规则为指定值
        if (check_id.equals("yellow")) {
            if (!check_text.equals(Str.toString())) {
                writeTxtFile("第" + (Rol + 1) + "行" + "第" + (Col + 1) + "列：" + "填写值为”" + Str + "“，但指定值为”" + check_text + "“，内容不为指定值", "result");
            }
        }
        //如果检测规则为不为空
        else {
            //如果校验字段内包含方框对号
            if(regular != null){
                if(!Str.toString().contains("☑")){
                    writeTxtFile("第" + Rol + "行" + "第" + Col + "列：" + "必须进行选择", "result");
                }
            }
            //如果校验内容是纯文本
            else if(Str.toString().equals("")) {
                writeTxtFile("第" + Rol + "行" + "第" + Col + "列：" + "内容不允许为空", "result");
            }
        }
    }

    //检验纯文档
    private void check_word(List<Store> liststores, List<Regular> listregular, XWPFDocument xdoc) {

        Regular regular = listregular.get(0);

        //将纯文档转化为整段文字
        List<XWPFParagraph> paras = xdoc.getParagraphs();
        StringBuilder text = new StringBuilder();

        //对于，每一段内容
        for(int i = 0; i < paras.size(); i++) {
            text.append(paras.get(i).getText());
            //添加分隔符
            if(text.charAt(text.length() - 1) != 'o'){
                text.append("o");
            }
        }

        //按照正则表达式提取出需要比对的内容
        Pattern pat = Pattern.compile(regular.getRegular());
        Matcher mat = pat.matcher(text.toString());


        if(mat.matches()) {
            for(Store store : liststores){
                int num = store.getPara_id() + 1;
                if(store.getCheck_id().equals("yellow")){
                    if (!mat.group(num).equals(store.getText())) {
                        writeTxtFile("第" + store.getPara_id() + "处" + "存在不为指定值的内容", "result");
                    }
                }else{
                    if(mat.group(num).equals("")){
                        writeTxtFile("第" + store.getPara_id() + "处" + "存在未填选项", "result");
                    }else if(mat.group(num).equals("☑")){
                        if(!mat.group(num).contains("☑")){
                            writeTxtFile("第" + store.getPara_id() + "处" + "存在未填选项", "result");
                        }
                    }
                }
            }
            /*for(int k = 1; k <= mat.groupCount(); k++){
                if(liststores.get(k - 1).getCheck_id().equals("yellow")){
                    if (!mat.group(k).equals(liststores.get(k - 1).getText())) {
                        writeTxtFile("第" + liststores.get(k - 1).getPara_id() + "段" + "存在不为指定值的内容", "result");
                    }
                }
                else{
                    if(mat.group(k).equals("")){
                        writeTxtFile("第" + liststores.get(k - 1).getPara_id() + "段" + "存在未填选项", "result");
                    }
                }
            }*/
        }
        //
        /*for (Store x : list){

            //获取
            List<XWPFRun> runsLists = paras.get(x.getRol()).getRuns();
            XWPFRun xL = runsLists.get(x.getCol());
            String text_check = x.getText();
            String text_upload = xL.text();
            String color = x.getCheck_id();
            if (color.equals("yellow")) {
                if (!text_check.equals(text_upload)) {
                    writeTxtFile("第" + x.getRol() + "段； " + "原值为”" + text_check + "“，但指定值为”" + text_upload + "“，内容不为指定值", "result");
                }
            } else {
                if(text_check.equals("")) {
                    writeTxtFile("第" + x.getRol() + "段； " + "内容不允许为空", "result");
                }
            }
        }*/
    }
//    List<Store> list = checkDao.liststores(1);
//
//        //创建需要输出的TXT文档
////        creatTxtFile("result");
//
//        InputStream in = new FileInputStream(uploadFilePath + "4.docx");
//
//        XWPFDocument xdoc = new XWPFDocument(in);
//        //获取word文件中的表格
//        Iterator<XWPFTable> itTable = xdoc.getTablesIterator();
//        XWPFTable table;
//
//        if (itTable.hasNext()) {
//            int tableIndex = 0; //表格编号
//            int start=0;
//            while (itTable.hasNext()) {
//                XWPFTableRow row;
//                List<XWPFTableCell> cells;
//                table = itTable.next();
//
//                for (int i = start; i < list.size(); i++) {
//                    Store x = list.get(i);
//                    if (x.getTable_id() == tableIndex) {
//                        System.out.println(x);
//                        int Rol = x.getRol();
//                        int Col = x.getCol();
//                        int table_id = x.getTable_id();
//                        String check_id = x.getCheck_id();
//                        String check_text = x.getText();
////                    System.out.println(table.getNumberOfRows());
//                        //获取word表格对应的单元格
//                        row = table.getRow(Rol);
//                        cells = row.getTableCells();  //针对每一行的所有单元格
////                    System.out.println(cells.size());
//                        XWPFTableCell cell = cells.get(Col); //获取单个单元格
//                        System.out.println(cell.getText());
//                    } else {
//                        tableIndex++;
//                        start = i;
//                        break;
//                    }
//                }
//            }
//        }
//
//        //如果检测不到表格
//        else{
//            String testTxt = "投资期限：□短期（1年以内）□中期（3年以内）□中长期（5年以内）☑期限不限";
////            //  注意[\u4E00-\u9FA5]里面的斜杠字符，千万不可省略，不区分大小写
//            if(testTxt.contains("☑"))
//                System.out.println("yes");
//            Pattern pat = Pattern.compile("投资期限：(.*?)不限");
//            Matcher mat = pat.matcher(testTxt);
//            if(mat.matches()) {
//                for(int i = 1; i <= mat.groupCount(); i++){
//                    System.out.println(mat.group(i));
//                }
//
//            }
////            char[] utfBytes = "郑伟成".toCharArray();
////            String unicodeBytes = "";
////            for (int i = 0; i < utfBytes.length; i++) {
////                String hexB = Integer.toHexString(utfBytes[i]);
////                if (hexB.length() <= 2) {
////                    hexB = "00" + hexB;
////                }
////                unicodeBytes = unicodeBytes + "\\u" + hexB;
////            }
////            System.out.println(unicodeBytes);
//            /*List<XWPFParagraph> paras = xdoc.getParagraphs();
//            for (Store x : list){
//
//                //获取对应的段落
//                List<XWPFRun> runsLists = paras.get(x.getRol()).getRuns();
//                XWPFRun xL = runsLists.get(x.getCol());
//                String text_check = x.getText();
//                String text_upload = xL.text();
//                String color = x.getCheck_id();
//                if (color.equals("yellow")) {
//                    if (!text_check.equals(text_upload)) {
//                        writeTxtFile("第" + x.getRol() + "段； " + "原值为”" + text_check + "“，但指定值为”" + text_upload + "“，内容不为指定值", "result");
//                    }
//                } else {
//                    if(text_check.equals("")) {
//                        writeTxtFile("第" + x.getRol() + "段； " + "内容不允许为空", "result");
//                    }
//                }
//            }*/
//        }
        /*String[] colors = {
                "yellow", //黄色
                "cyan", //蓝色
        };

        //根据id读取到对应的模板
        InputStream in = new FileInputStream(checkFilePath + "checkfile.docx"); //docx文件
        @SuppressWarnings("resource")
        XWPFDocument xdoc = new XWPFDocument(in);
        Iterator<XWPFTable> itTable = xdoc.getTablesIterator();  //获取word文件中的表格
        //如果文档能检测到表格
        if(itTable.hasNext()){
             //表格编号
            for(Store x : list){
                XWPFTable table;
                int tableIndex = 0; //表格编号
                while (itTable.hasNext()) {  //循环word中的每个表格

                    table = itTable.next();
                    XWPFTableRow row;
                    List<XWPFTableCell> cells;
                    for (int i = 0; i < table.getNumberOfRows(); i++) {
                    if(i == 0)  //是否略过表头
                    {
                        continue;
                    }
                        row = table.getRow(i);  //获取word表格的每一行
                        cells = row.getTableCells();  //针对每一行的所有单元格
                        for (int j = 0; j < cells.size(); j++) {
                            XWPFTableCell cell = cells.get(j); //获取单个单元格
                            //获取单元格相同字体颜色+文字
                            List<XWPFParagraph> paras_mul = cell.getParagraphs(); //获取包含段落的列表
                            for(XWPFParagraph paras : paras_mul){
//                                System.out.println(paras.getText());
                                List<XWPFRun> runsLists = paras.getRuns();//获取段落中的列表
                                String temp = "";
                                for(XWPFRun xL:runsLists){
                                    if(xL.getCTR().getRPr().getHighlight() != null){
                                        temp += xL.text();
                                    }
//                                    if(xL.getCTR().getRPr().getHighlight() != null){
//                                        String c = xL.getCTR().getRPr().getHighlight().getVal().toString();
////                                        System.out.println(c);
//                                        String text = xL.text();
//                                        System.out.println(text);
//                                    }
                                }
                                System.out.println(temp);
                            }
                        }
                    }
                    tableIndex++;
                }
            }
        }*/
        //如果能检测到表格
        /*if(itTable.hasNext()){

            for (Store x : list) {

                int i = 0;
                for (i = 0; i < x.getTable_id(); i++) {
                    itTable.hasNext();
                    itTable.next();
                }

                System.out.println(i);

                table = itTable.next();
                List<XWPFTableCell> cells;

                //获取word表格的对应行
                XWPFTableRow row = table.getRow(x.getRol());

                //获取对应的列
//                XWPFTableCell cell = row.getTableCells().get(x.getCol());

                //获取包含段落的列表--不止有一段的情况
                List<XWPFParagraph> paras = cell.getParagraphs();

                //假设只有一个段落
                String text_check = paras.get(0).getText();

                String color = x.getCheck_id();
                String text_upload = x.getText();
                if (color.equals("yellow")) {
                    if (!text_check.equals(text_upload)) {
                        writeTxtFile("第" + x.getRol() + "行" + "第" + x.getCol() + "列； " + "原值为”" + text_check + "“，但指定值为”" + text_upload + "“，内容不为指定值", "result");
                    }
                } else {
                    if(text_check.equals("")) {
                        writeTxtFile("第" + x.getRol() + "行" + "第" + x.getCol() + "列" + "内容不允许为空", "result");
                    }
                }
            }
        }
        //如果检测不到表格
        else{
            List<XWPFParagraph> paras = xdoc.getParagraphs();
            for (Store x : list){

                //获取对应的段落
                List<XWPFRun> runsLists = paras.get(x.getRol()).getRuns();
                XWPFRun xL = runsLists.get(x.getCol());
                String text_check = x.getText();
                String text_upload = xL.text();
                String color = x.getCheck_id();
                if (color.equals("yellow")) {
                    if (!text_check.equals(text_upload)) {
                        writeTxtFile("第" + x.getRol() + "段； " + "原值为”" + text_check + "“，但指定值为”" + text_upload + "“，内容不为指定值", "result");
                    }
                } else {
                    if(text_check.equals("")) {
                        writeTxtFile("第" + x.getRol() + "段； " + "内容不允许为空", "result");
                    }
                }
            }
        }*/
        //从数据库中提取出所有的数据准备进行校验
//        List<Store> list = checkDao.liststores(1);
//        System.out.println(list);

//        //读取下载到data的文件
//        InputStream in = new FileInputStream(checkFilePath + filename + ".docx"); //docx文件
//        @SuppressWarnings("resource")
//        XWPFDocument xdoc = new XWPFDocument(in);
//        Iterator<XWPFTable> itTable = xdoc.getTablesIterator();  //获取word文件中的表格
//        XWPFTable table;
//        table = itTable.next();
//
//        //创建需要输出的TXT文档
//        creatTxtFile(filename + "-校验模板id" + id + ".txt");
//
//        for (Store x : list) {
//            XWPFTableRow row;
//            List<XWPFTableCell> cells;
//            row = table.getRow(x.getRol());  //获取word表格的对应行
//            cells = row.getTableCells();  //针对这一行的所有单元格
//            XWPFTableCell cell = cells.get(x.getCol()); //获取对应的单元格
//            XWPFParagraph paras = cell.getParagraphs().get(0); //获取包含段落的列表--只有一段
//            String text_check = paras.getText();
//            String color = x.getCheck_id();
//            String text_upload = x.getText();
//            if (color.equals("FFFF00")) {
//                if (!text_check.equals(text_upload)) {
//                    writeTxtFile("第" + x.getRol() + "行" + "第" + x.getCol() + "列； " + "原值为" + text_check + "，但指定值为" + text_upload + "，内容不为指定值", "result");
//                }
//            } else {
//                if (text_check.equals("")) {
//                    writeTxtFile("第" + x.getRol() + "行" + "第" + x.getCol() + "列" + "内容不允许为空", "result");
//                }
//            }
//
//        }


    //创建需要输出的TXT文件
    public boolean creatTxtFile(String name) throws IOException {
        boolean flag = false;
        File filename = new File(downloadFilePath + name);

        //首先判断之前是否已经存在校验结果，如果存在则清空文件内容
        if (!filename.exists()) {
            filename.createNewFile();
            flag = true;
        } else {
            File fileTempObj = new File(downloadFilePath + name);
            clearTxtFile(name);
            flag = true;
        }
        return flag;
    }

    //清空文件中的内容
    public void clearTxtFile(String fileName) {
        File file = new File(downloadFilePath + fileName);
        try {
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write("");
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //写已有的TXT输出文件
    public void writeTxtFile(String info, String fileName) {
        File file = new File(downloadFilePath + fileName);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fileWriter = new FileWriter(file, true);
            info = info + System.getProperty("line.separator");
            fileWriter.write(info);
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


        /*List<Store> list = this.checkDao.liststores(id);

        *//*for(Store x : list){
            System.out.println(x.getColor());
        }*//*
        InputStream in = new FileInputStream(checkFilePath + "checkfile.docx"); //docx文件
        @SuppressWarnings("resource")
        XWPFDocument xdoc = new XWPFDocument(in);
        Iterator<XWPFTable> itTable = xdoc.getTablesIterator();  //获取word文件中的表格
        XWPFTable table;
        table = itTable.next();

        for (Store x : list) {

            XWPFTableRow row;
            List<XWPFTableCell> cells;
            row = table.getRow(x.getRol());  //获取word表格的对应行
            cells = row.getTableCells();  //针对这一行的所有单元格
            XWPFTableCell cell = cells.get(x.getCol()); //获取对应的单元格
            XWPFRun run = cell.addParagraph().getRuns().get(0);
            XWPFParagraph paras = cell.getParagraphs().get(0); //获取包含段落的列表--只有一段
            String text_check = paras.getText();
//            System.out.println(text_check);
            String color = x.getCheck_id();
            String text_upload = x.getText();
            CTRPr pr = run.getCTR().getRPr();
            System.out.println(pr);
//            System.out.println(text_upload);
            if (color.equals("FFFF00")) {
                if (!text_check.equals(text_upload)) {
                    System.out.println("第" + x.getRol() + "行" + "第" + x.getCol() + "列" + "内容不为指定值/n");
                }
            } else {
                if (text_check.equals("")) {
                    System.out.println("第" + x.getRol() + "行" + "第" + x.getCol() + "列" + "内容不允许为空/n");
                }
            }*/
            /*for (XWPFRun xL : runsLists) {
                String color = x.getColor();
                System.out.println(color);
                String text_check = xL.text();
                String text_upload = x.getText();
                System.out.println(color.equals("FFFF00"));
                if (color.equals("FFFF00")) {
                    if (text_check != text_upload) {
                        System.out.println("第" + x.getRol() + "行" + "第" + x.getCol() + "列" + "内容不为指定值");
                    }
                } else {
                    if (text_check == null) {
                        System.out.println("第" + x.getRol() + "行" + "第" + x.getCol() + "列" + "内容不允许为空");
                    }
                }
            }*/


            /*XWPFTableRow row;
            List<XWPFTableCell> cells;
            row = table.getRow(x.getRol());  //获取word表格的对应行
            cells = row.getTableCells();  //针对这一行的所有单元格
            XWPFTableCell cell = cells.get(x.getCol()); //获取对应的单元格
            XWPFParagraph paras = cell.getParagraphs().get(0); //获取包含段落的列表--只有一段
            List<XWPFRun> runsLists = paras.getRuns();//获取段落中的列表

            for (XWPFRun xL : runsLists) {
                String color = x.getColor();
                String text_check = xL.text();
                String text_upload = x.getText();
                if (color.equals("FFFF00")) {
                    if (!text_check.equals(text_upload)) {
                        writeTxtFile("第" + x.getRol() + "行" + "第" + x.getCol() + "列" + "内容不为指定值", "result");
                    }
                } else {
                    if(text_check.equals("")) {
                        writeTxtFile("第" + x.getRol() + "行" + "第" + x.getCol() + "列" + "内容不允许为空", "result");
                    }
                }
            }*/


