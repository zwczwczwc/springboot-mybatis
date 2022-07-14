package zwc.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.Table;
import java.io.Serializable;

/**
 * @author zwc
 * 2022-07-11
 * 16:18
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Table(name = "regular")
public class Regular implements Serializable {

    private Integer Rol;
    private Integer Col;
    //文档类型
    private Integer file_id;
    //正则表达式
    private String regular;

    private Integer table_id;

}
