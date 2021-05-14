package pl.cdfn.easymessage.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import pl.cdfn.easymessage.serialization.MessagePackSerializable;

public class MessageHandlerRegistry {

    public static final MessageHandlerRegistry INSTANCE = new MessageHandlerRegistry();

    @SuppressWarnings("rawtypes")
    private final Map<Class<?>, Set<MessageHandler>> consumerMap;

    public MessageHandlerRegistry() {
      this.consumerMap = new HashMap<>();
    }

    public <T extends MessagePackSerializable> void addHandler(Class<T> clazz, MessageHandler<T> consumer) {
      var set = this.consumerMap.get(clazz);
      if(set == null) {
        consumerMap.put(clazz, Set.of(consumer));
        return;
      }
      set.add(consumer);
    }

    @SuppressWarnings("unchecked")
    public void callAll(Class<?> clazz, MessagePackSerializable message) {
      var iterator = this.consumerMap.get(clazz).iterator();
      while (iterator.hasNext()) {
        var next = iterator.next();
        next.accept(message);
        if (next.isOneTime()) {
          iterator.remove();
        }
      }
    }
}
