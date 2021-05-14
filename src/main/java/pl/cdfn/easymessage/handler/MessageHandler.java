package pl.cdfn.easymessage.handler;

import pl.cdfn.easymessage.serialization.MessagePackSerializable;

public interface MessageHandler<T extends MessagePackSerializable> {

  void accept(T message);

  default boolean isOneTime() {
    return false;
  }
}
