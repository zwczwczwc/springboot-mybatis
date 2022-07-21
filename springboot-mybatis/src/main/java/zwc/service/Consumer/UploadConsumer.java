package zwc.service.Consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * @author zwc
 * 2022-07-20
 * 15:39
 */

@Component
@Slf4j
@RocketMQMessageListener(topic = "broker-a", consumerGroup = "uploadconsumer")
public class UploadConsumer implements RocketMQListener<String> {

    @Override
    public void onMessage(String message) {
        log.info("message:{}", message);
    }

}
