package zwc.dao;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;
import zwc.pojo.Regular;
import zwc.pojo.Store;
import java.util.List;

/**
 * @author zwc
 */
@Mapper
@Repository
public interface CheckDao {

    //查询所有已经存储的store并返回store列表
    public List<Store> liststores(int id);

    public List<Regular> listregulars(int id);

}
