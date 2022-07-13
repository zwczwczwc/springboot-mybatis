package zwc.dao;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface DeleteDao {

    public void delete(int id);

    public void deleteRegular(int id);
}
