package zwc.dao;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;
import zwc.pojo.Regular;
import zwc.pojo.Store;

@Mapper
@Repository
public interface UploadDao {

    //在更新操作中只需要将所有元素重新插入表格中即可
    public int addStore(Store store);

    public int addRegular(Regular regular);
}
