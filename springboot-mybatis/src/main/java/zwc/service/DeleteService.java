package zwc.service;

import java.io.IOException;

/**
 * @author zwc
 * 2022-07-04
 * 14:00
 */
public interface DeleteService {
    void delete(int id) throws IOException;

    void deleteRegular(int id) throws IOException;

    void deleteCache(int id) throws  IOException;

    void deleteCacheRegular(int id) throws IOException;
}
