package zwc.service.Impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import zwc.dao.DeleteDao;
import zwc.service.DeleteService;

import java.io.IOException;

/**
 * @author zwc
 * 2022-07-04
 * 14:01
 */
@Service
public class DeleteServiceImpl implements DeleteService {

    @Autowired
    private DeleteDao deleteDao;

    @Override
    public void delete(int id) throws IOException{
        deleteDao.delete(id);
    }

    @Override
    public void deleteRegular(int id) throws  IOException{
        deleteDao.deleteRegular(id);
    }

}
