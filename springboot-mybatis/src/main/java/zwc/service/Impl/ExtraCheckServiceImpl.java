package zwc.service.Impl;

import org.apache.poi.xwpf.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import zwc.service.ExtraCheckService;

import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author zwc
 * 2022-07-26
 * 10:19
 */
@Service
public class ExtraCheckServiceImpl implements ExtraCheckService {

    @Value("${download_dir}")
    private String downloadFilePath;

    @Value("${check_dir}")
    private String checkFilePath;

    @Override
    public void extraCheck(String filename, int id) throws IOException {

        //读取下载到data的文件
        InputStream in = new FileInputStream(checkFilePath + filename);

        //用于处理特殊情况
        if(id == 1){
            extra_check_1(filename);
        }
        if(id == 2){
            extra_check_2(filename);
        }
        if(id == 3){
            //可能对表结构有修改的情况还没有考虑
            extra_check_3(filename);
        }
        if(id == 4){
            extra_check_4(filename);
        }
    }

    private void extra_check_1(String filename) throws IOException {

        InputStream in = new FileInputStream(checkFilePath + filename);

        @SuppressWarnings("resource")
        XWPFDocument xdoc = new XWPFDocument(in);

        //获取word文件中的表格
        Iterator<XWPFTable> itTable = xdoc.getTablesIterator();

        //按照表格顺序一一进行检查
        XWPFTable table = itTable.next();
        table = itTable.next();

        XWPFTableCell cell_1 = getCell(4,1,table);
        if(cell_1.getText().equals("是☑否□")){
            writeTxtFile("第5行" + "第2列未填写姓名", "result");
        }
        XWPFTableCell cell_2 = getCell(21,1,table);
        if(cell_2.getText().equals("是☑否□")){
            writeTxtFile("第22行" + "第2列未填写时长", "result");
        }
        XWPFTableCell cell_3 = getCell(30,1,table);
        if(cell_3.getText().equals("是☑否□")){
            writeTxtFile("第30行" + "第2列未填方式", "result");
        }
        XWPFTableCell cell_4 = getCell(34,1,table);
        if(cell_4.getText().equals("是☑否□")){
            writeTxtFile("第34行" + "第2列未填写方式", "result");
        }
        XWPFTableCell cell_5 = getCell(40,1,table);
        if(cell_5.getText().equals("是☑否□")){
            writeTxtFile("第41行" + "第2列未填写方式", "result");
        }
        XWPFTableCell cell_6 = getCell(50,0,table);
        if(cell_6.getText().trim().equals("姓名:")){
            writeTxtFile("第51行" + "第2列未填写姓名", "result");
        }
        XWPFTableCell cell_7 = getCell(51,0,table);
        if(cell_7.getText().trim().equals("所在部门:")){
            writeTxtFile("第51行" + "第2列未填写所在部门", "result");
        }
        XWPFTableCell cell_8 = getCell(52,0,table);
        if(cell_8.getText().trim().equals("职务:")){
            writeTxtFile("第51行" + "第2列未填写职务", "result");
        }
        XWPFTableCell cell_9 = getCell(53,0,table);
        if(cell_9.getText().trim().equals("邮箱地址:")){
            writeTxtFile("第51行" + "第2列未填写邮箱地址", "result");
        }
        XWPFTableCell cell_10 = getCell(54,0,table);
        if(cell_10.getText().trim().equals("联系电话:")){
            writeTxtFile("第51行" + "第2列未填写联系电话", "result");
        }
    }

    private void extra_check_2(String filename) throws IOException {

        InputStream in = new FileInputStream(checkFilePath + filename);

        @SuppressWarnings("resource")
        XWPFDocument xdoc = new XWPFDocument(in);

        //获取word文件中的表格
        Iterator<XWPFTable> itTable = xdoc.getTablesIterator();

        //按照表格顺序一一进行检查
        XWPFTable table = itTable.next();

        XWPFTableCell cell_1 = getCell(6,1,table);
        //将cell中的内容转化为整段的文字
        List<XWPFParagraph> paras_1 = cell_1.getParagraphs();
        StringBuilder text_1 = new StringBuilder();
        for(int i = 0; i < paras_1.size(); i++) {
            text_1.append(paras_1.get(i).getText());
            //添加分隔符
            if(text_1.charAt(text_1.length() - 1) != 'o'){
                text_1.append("o");
            }
        }

        Pattern pat_1 = Pattern.compile("截至2022年06月17日o1.产品净值：【(.*?)】o2.产品规模：【(.*?)】万元o3.托管机构：【(.*?)】o4.是否结构化产品：(.*?)o5.产品成立日：(.*?)o6.产品到期日：(.*?)o7.产品代码：【(.*?)】o8.产品类别：【(.*?)】o9.投资范围是否包括“收益互换、场外衍生品”？o(.*?)是，见【华夏资本鸿鹄鑫享20号集合资产管理计划资产管理合同】（合同名称）第【29】页  □否o10.是否已在产品协议中揭示了投资“收益互换、场外衍生品”的风险？o(.*?)是，见【华夏资本鸿鹄鑫享20号集合资产管理计划资产管理合同】（合同名称）第【62】页  □否o11.是否存在单一委托人占比超过20%的情况：(.*?)o如果选择是，则填写：o本机构承诺，以上委托人符合《证券期货投资者适当性管理办法》规定的专业投资者标准。o12.交易目的及性质：o（1）资金来源：是否为募集资金投资(.*?)是□否o（2）交易目的：(.*?)o（3）投资期限：(.*?)o（4）拟挂钩投资标的（交易用途）：(.*?)o");
        Matcher mat_1 = pat_1.matcher(text_1.toString());

        if(mat_1.matches()) {

            String check_text_1 = mat_1.group(11);

            if(check_text_1.contains("☑是")){
                XWPFTable table_temp = cell_1.getTableArray(0);
                if(getCell(0,0,table_temp).getText().trim().equals("投资者姓名或机构名称")){
                    writeTxtFile("第7行" + "第2列未填写表格内投资者姓名或机构名称", "result");
                }
                if(getCell(0,1,table_temp).getText().trim().equals("身份证号或统一信用代码")){
                    writeTxtFile("第7行" + "第2列未填写表格内身份证号或统一信用代码", "result");
                }
                if(getCell(0,2,table_temp).getText().trim().equals("持有份额比例（%）")){
                    writeTxtFile("第7行" + "第2列未填写表格内持有份额比例", "result");
                }
                if(getCell(0,3,table_temp).getText().trim().equals("是否符合《证券期货投资者适当性管理办法》专业投资者标准")){
                    writeTxtFile("第7行" + "第2列未填写表格内是否符合《证券期货投资者适当性管理办法》专业投资者标准", "result");
                }
            }

            String check_text_2 = mat_1.group(15);

            if(check_text_2.charAt(37) != '□' && check_text_2.substring(38, check_text_2.length()).equals("其他____________")){
                writeTxtFile("第7行" + "第2列未填写其他", "result");
            }
        }

        XWPFTableCell cell_2 = getCell(7,1,table);
        List<XWPFParagraph> paras_2 = cell_2.getParagraphs();
        StringBuilder text_2 = new StringBuilder();
        for(int i = 0; i < paras_2.size(); i++) {
            text_2.append(paras_2.get(i).getText());
            //添加分隔符
            if(text_2.charAt(text_2.length() - 1) != 'o'){
                text_2.append("o");
            }
        }
        Pattern pat_2 = Pattern.compile("是否经认可取得开展本金融衍生产品交易资格？o(.*?)o");
        Matcher mat_2 = pat_2.matcher(text_2.toString());
        if(mat_2.matches()){
            String check_text = mat_2.group(1);
            if(check_text.trim().equals("☑是，请具体列明： o□否")){
                writeTxtFile("第8行" + "第2列未填写具体情况", "result");
            }
        }

        XWPFTableCell cell_3 = getCell(8,1,table);
        //将cell中的内容转化为整段的文字
        List<XWPFParagraph> paras_3 = cell_3.getParagraphs();
        StringBuilder text_3 = new StringBuilder();
        for(int i = 0; i < paras_3.size(); i++) {
            text_3.append(paras_3.get(i).getText());
            //添加分隔符
            if(text_3.charAt(text_3.length() - 1) != 'o'){
                text_3.append("o");
            }
        }

        Pattern pat_3 = Pattern.compile("1、是否有来源于以下机构的不良诚信记录？o(.*?)o2、机构自身及其控股股东、实际控制人、董监高有无下列负面记录：o(.*?)o3、是否有证券异常交易相关记录：o(.*?)o");
        Matcher mat_3 = pat_3.matcher(text_3.toString());

        if(mat_3.matches()) {

            String check_text_1 = mat_3.group(1);

            if(check_text_1.substring(0, 90).contains("☑")){
                if("o□无".equals(check_text_1.substring(91).trim())){
                    writeTxtFile("第9行" + "第2列第1处未填写具体情况", "result");
                }
                if(check_text_1.substring(91).trim().contains("☑")){
                    writeTxtFile("第9行" + "第2列第1处非法选择，请修改选择", "result");
                }
            }

            String check_text_2 = mat_3.group(2);
            if(check_text_2.substring(0, 86).contains("☑")){
                if("o□无".equals(check_text_2.substring(87).trim())){
                    writeTxtFile("第9行" + "第2列第2处未填写具体情况", "result");
                }
                if(check_text_2.substring(87).contains("☑")){
                    writeTxtFile("第9行" + "第2列第2处非法选择，请修改选择", "result");
                }
            }

            String check_text_3 = mat_3.group(3);
            if(check_text_3.substring(0, 158).contains("☑")){
                if("o□无".equals(check_text_3.substring(159).trim())){
                    writeTxtFile("第9行" + "第2列第3处未填写具体情况", "result");
                }
                if(check_text_3.substring(159).contains("☑")){
                    writeTxtFile("第9行" + "第2列第3处非法选择，请修改选择", "result");
                }
            }
        }
    }

    private void extra_check_3(String filename) throws  IOException{

    }

    private void extra_check_4(String filename) throws IOException{
        writeTxtFile("请人工检测是否盖章", "result");
    }

    public XWPFTableCell getCell(int Rol, int Col, XWPFTable table){

        XWPFTableRow row = null;
        List<XWPFTableCell> cells = null;

        //获取word表格对应的单元格
        row = table.getRow(Rol);

        //针对每一行的所有单元格
        cells = row.getTableCells();

        //获取单个单元格
        return cells.get(Col);
    }

    //创建需要输出的TXT文件
    private boolean creatTxtFile(String name) throws IOException {
        boolean flag = false;
        File filename = new File(downloadFilePath + name);

        //首先判断之前是否已经存在校验结果，如果存在则清空文件内容
        if (!filename.exists()) {
            filename.createNewFile();
            flag = true;
        }else{
            clearTxtFile(name);
            flag = true;
        }
        return flag;
    }

    //清空文件中的内容
    private void clearTxtFile(String fileName) {
        File file =new File(downloadFilePath + fileName);
        try {
            FileWriter fileWriter =new FileWriter(file);
            fileWriter.write("");
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //写已有的TXT输出文件
    private void writeTxtFile(String info, String fileName) {
        File file =new File(downloadFilePath + fileName);
        try {
            if(!file.exists()){
                file.createNewFile();
            }
            FileWriter fileWriter =new FileWriter(file, true);
            info =info +System.getProperty("line.separator");
            fileWriter.write(info);
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
