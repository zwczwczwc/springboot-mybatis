package zwc.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zwc
 * 2022-07-11
 * 16:18
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Regular {

    private Integer Rol;
    private Integer Col;
    //文档类型
    private Integer file_id;
    //正则表达式
    private String regular;

    private Integer table_id;

}
