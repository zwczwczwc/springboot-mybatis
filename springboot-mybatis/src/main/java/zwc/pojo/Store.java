package zwc.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.Table;
import java.io.Serializable;

/**
 * @author zwc
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Table(name = "store")

//需要把内存中的对象状态数据保存到一个文件或者数据库中的时候，这个场景是比较常见的，例如我们利用mybatis框架编写持久层insert对象数据到数据库中时;
//网络通信时需要用套接字在网络中传送对象时，如我们使用RPC协议进行网络通信时;
public class Store implements Serializable {
    //行列
    private Integer Col;
    private Integer Rol;
    //检查类型
    private String check_id;
    //文本内容
    private String text;
    //文档内表格id
    private Integer table_id;
    //文档类型
    private Integer file_id;
    //正则表达式
    private String regular;
    //纯文本或者表格内的段落编号
    private Integer para_id;
}
