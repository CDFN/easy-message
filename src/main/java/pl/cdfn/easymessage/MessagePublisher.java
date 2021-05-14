package pl.cdfn.easymessage;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.sync.RedisCommands;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.cdfn.easymessage.serialization.MessagePackSerializable;
import pl.cdfn.easymessage.util.StringByteCodec;

public class MessagePublisher {
  private static final Logger LOGGER = LoggerFactory.getLogger(MessagePublisher.class);

  private final RedisCommands<String, byte[]> commands;
  private final String prefix;

  public MessagePublisher(RedisClient client, String prefix) {
    this.commands = client.connect(StringByteCodec.INSTANCE).sync();
    this.prefix = prefix;
  }

  public void publish(MessagePackSerializable message) {
    var className = message.getClass().getName();
    try {
      commands.publish(
          prefix + className,
          message.serialize()
      );
    } catch (IOException e) {
      LOGGER.error("error while publishing message {}", className, e);
    }
  }

}