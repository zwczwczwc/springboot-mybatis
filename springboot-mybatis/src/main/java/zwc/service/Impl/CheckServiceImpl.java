package zwc.service.Impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import zwc.dao.CheckDao;
import zwc.pojo.Regular;
import zwc.pojo.Store;
import zwc.service.CheckService;

import java.io.*;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author zwc
 */

@Service
@Slf4j
public class CheckServiceImpl implements CheckService {

    @Value("${check_dir}")
    private String checkFilePath;

    @Value("${upload_dir}")
    private String uploadFilePath;

    @Value("${download_dir}")
    private String downloadFilePath;

    @Autowired
    private CheckDao checkDao;

    @Autowired
    private RedisTemplate redisTemplate;

    //查询表中的所有元组并进行检查
    @Override
    public void liststores(String filename, int id) throws IOException {

        //从数据库中提取出所有的数据准备进行校验
        List<Store> liststores = listStore(id);

        List<Regular> listRegular = listRegular(id);

//        listRegular.forEach(System.out::println);

        //读取下载到data的文件
        InputStream in = new FileInputStream(checkFilePath + filename);

        //创建需要输出的TXT文档
        creatTxtFile("result");

        @SuppressWarnings("resource")
        XWPFDocument xdoc = new XWPFDocument(in);
        //获取word文件中的表格
        Iterator<XWPFTable> itTable = xdoc.getTablesIterator();

        //如果能检测到表格
        if (itTable.hasNext()) {
            check_table(itTable, liststores, listRegular);
        }
        //如果检测不到表格
        else{
            check_word(liststores, listRegular, xdoc);
        }
        /*Iterator<XWPFTable> itTable = xdoc.getTablesIterator();
        XWPFTable table;
        XWPFTableRow row;
        List<XWPFTableCell> cells;

        //如果能检测到表格
        if(itTable.hasNext()){
            for (Store x : list) {

                int Rol = x.getRol();
                int Col = x.getCol();
                int table_id = x.getTable_id();
                String check_id = x.getCheck_id();
                String check_text = x.getText();

                // 过滤前面不需要的表格
                for (int i = 0; i < table_id; i++) {
                    itTable.hasNext();
                    itTable.next();
                }

                table = itTable.next();

                //获取word表格对应的单元格
                row = table.getRow(Rol);
                cells = row.getTableCells();  //针对每一行的所有单元格
                XWPFTableCell cell = cells.get(Col); //获取单个单元格

                StringBuilder Str = new StringBuilder();
                //获取包含段落的列表--不止有一段的情况
                List<XWPFParagraph> paras = cell.getParagraphs();
                for(XWPFParagraph para : paras){
                    List<XWPFRun> runsLists = para.getRuns();//获取段落中的列表
                    for(XWPFRun xL : runsLists){
                        Str.append(xL.text());
                    }
                }
                //假设只有一个段落
                String text_check = paras.get(0).getText();
                String color = x.getCheck_id();
                String text_upload = x.getText();
                if (check_id.equals("yellow")) {
                    if (!check_text.equals(Str.toString())) {
                        writeTxtFile("第" + Rol + "行" + "第" + Col + "列； " + "原值为”" + Str.toString() + "“，但指定值为”" + check_text + "“，内容不为指定值", "result");
                    }
                }
                else {
                    if(Str.toString().equals("")) {
                        writeTxtFile("第" + Rol + "行" + "第" + Col + "列" + "内容不允许为空", "result");
                    }
                }
            }
        }*/
    }

    //检验表格
    private void check_table(Iterator<XWPFTable> itTable, List<Store> liststore, List<Regular> listRegular){
        //word中表格编号
        int tableIndex = 0;

        //按照表格顺序一一进行检查
        while (itTable.hasNext()) {
            XWPFTableRow row = null;
            List<XWPFTableCell> cells = null;
            XWPFTable table = itTable.next();

            //检验普通单元格
            for (int i = 0; i < liststore.size(); i++) {
                //按照得到的store一一进行校验
                Store x = liststore.get(i);
                //按照表格顺序一一进行处理，处理正常的单元格
                if(x.getTable_id() == tableIndex && x.getPara_id() == -1){
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
                Regular regular = listRegular.get(i);
                if(regular.getTable_id() == tableIndex){
                    int Rol = regular.getRol();
                    int Col = regular.getCol();

                    //获取word表格对应的单元格
                    row = table.getRow(Rol);

                    //针对每一行的所有单元格
                    cells = row.getTableCells();

                    //获取单个单元格
                    XWPFTableCell cell = cells.get(Col);

                    //按照需要进行校验的单元格进行校验
                    check_table_word(cell, regular, liststore);
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

                //如果检测规则是必须为指定的值
                if(store.getCheck_id().equals("specific-context")){
                    if (!mat.group(num).equals(store.getText())) {
                        writeTxtFile("第" + (store.getRol() + 1) + "行" + "第" + (store.getCol() + 1) + "列" + "第" + (store.getPara_id() + 1) + "处" + "所填内容不为指定内容", "result");
                    }
                }else{
//                    System.out.println(mat.group(num));
                    if(mat.group(num).trim().equals("")){
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
        if (check_id.equals("specific-context")) {
            if (!check_text.equals(Str.toString())) {

                /*XWPFRun run = paras_mul.get(0).createRun();

                CTShd shd = run.getCTR().addNewRPr().addNewShd();
                shd.setFill("FF0000");
                run.setText("存在填写内容不为指定值，需要修改");*/

                writeTxtFile("第" + (Rol + 1) + "行" + "第" + (Col + 1) + "列：" + "填写值为”" + Str + "“，但指定值为”" + check_text + "“，内容不为指定值", "result");
            }
        }
        //如果检测规则为不为空
        else {
            //如果校验字段内包含方框对号
            if(regular != null){
                if(!Str.toString().contains("☑")){

                    /*XWPFRun run = paras_mul.get(0).createRun();

                    CTShd shd = run.getCTR().addNewRPr().addNewShd();
                    shd.setFill("FF0000");
                    run.setText("存在需填写的内容为空，需要修改");*/

                    writeTxtFile("第" + Rol + "行" + "第" + Col + "列：" + "必须进行选择", "result");
                }
            }
            //如果校验内容是纯文本
            else if(Str.toString().equals("")) {

                /*XWPFRun run = paras_mul.get(0).createRun();

                CTShd shd = run.getCTR().addNewRPr().addNewShd();
                shd.setFill("FF0000");
                run.setText("存在需填写的内容为空，需要修改");*/

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
                if(store.getCheck_id().equals("specific-context")){
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

    private List<Store> listStore(int id){
        List listStore = redisTemplate.opsForList().range("Store" + id, 0, -1);
        //使用双重校验锁从redis中提取数据
        if(listStore == null){
            synchronized (this.getClass()){
                log.debug("-----从Redis中查询数据-----");
                listStore = redisTemplate.opsForList().range("Store" + id, 0, -1);
                if(listStore == null){
                    log.debug("-----从数据库中提取数据-----");
                    listStore = checkDao.liststores(id);
                    for(int i = 0; i < listStore.size(); i++){
                        redisTemplate.opsForList().rightPush("Store" + id, listStore.get(i));
                    }
                }else{
                    log.debug("-----从Redis中提取数据(同步代码块)-----");
                    return listStore;
                }
            }
        }
        return listStore;
    }

    private List<Regular> listRegular(int id){
        List listRegular = redisTemplate.opsForList().range("Regular" + id, 0, -1);
        //使用双重校验锁从redis中提取数据
        if(listRegular == null){
            synchronized (this.getClass()){
                log.debug("-----从Redis中查询数据-----");
                listRegular = redisTemplate.opsForList().range("Store" + id, 0, -1);
                if(listRegular == null){
                    log.debug("-----从数据库中提取数据-----");
                    listRegular = checkDao.listregulars(id);
                    for(int i = 0; i < listRegular.size(); i++){
                        redisTemplate.opsForList().rightPush("Store" + id, listRegular.get(i));
                    }
                }else{
                    log.debug("-----从Redis中提取数据(同步代码块)-----");
                    return listRegular;
                }
            }
        }
        return listRegular;
    }
}
