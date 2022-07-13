package zwc.service;

import java.io.IOException;

/**
 * @author zwc
 * 2022-07-04
 * 14:00
 */
public interface DeleteService {
    public void delete(int id) throws IOException;

    public void deleteRegular(int id) throws IOException;
}
