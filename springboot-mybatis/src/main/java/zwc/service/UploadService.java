package zwc.service;

import java.io.IOException;

public interface UploadService {

    //在更新操作中只需要将所有元素重新插入表格中即可
    boolean addStore(int id) throws IOException;
}
