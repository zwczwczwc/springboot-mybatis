package zwc.pojo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zwc
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Store {
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
