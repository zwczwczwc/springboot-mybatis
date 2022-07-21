package zwc.service.Producer;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author zwc
 * 2022-07-20
 * 15:51
 */

@Service
@Slf4j
public class UploadProducer {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    public void send(String id){
        rocketMQTemplate.convertAndSend("broker-a", id);
    }
}
