package zwc.service.Impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
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

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public void delete(int id) throws IOException{
        deleteDao.delete(id);
    }

    @Override
    public void deleteRegular(int id) throws  IOException{
        deleteDao.deleteRegular(id);
    }

    @Override
    public void deleteCache(int id) throws IOException {
        if(redisTemplate.hasKey("Store" + id)){
            redisTemplate.delete("Store" + id);
        }
    }

    @Override
    public void deleteCacheRegular(int id) throws IOException{
        if(redisTemplate.hasKey("Regular" + id)){
            redisTemplate.delete("Regular" + id);
        }
    }
}
