# Overview
This library helps in communication across multiple JVMs.
It works by serializing messages using MessagePack and sending them to specific channels.
Deserialization happens automatically when a message is received on channel. After deserialization, all handlers for message's class are triggered. If there's no such class in classloader, message is getting skipped.
In order to get this library working properly, you need to have exactly the same path to class in both classloaders of JVMs you want to communicate between.
<br><br>
This library utilizes lettuce as Redis client.
In order to use the library, you have to create your own RedisClient, so you can pass it to `MessagePublisher` and   

# Messages
Example message using MessagePackSerializable interface:

```java
import java.io.IOException;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;
import pl.cdfn.easymessage.serialization.MessagePackSerializable;

public class MyMessage implements MessagePackSerializable {

  private Integer someInt;
  private String someString;
  private byte[] someBytes;

  public MyMessage(int someInt, String someString, byte[] someBytes) {
    this.someInt = someInt;
    this.someString = someString;
    this.someBytes = someBytes;
  }
  
  @Override
  public void deserialize(byte[] bytes) throws IOException {
    try (MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(bytes)){
      this.someInt = unpacker.unpackInt();
      this.someString = unpacker.unpackString();
      this.someBytes = unpacker.readPayload(unpacker.unpackArrayHeader());
    }
  }

  @Override
  public byte[] serialize() throws IOException {
    try (MessagePacker packer = MessagePack.newDefaultBufferPacker()) {
      packer.packInt(this.someInt);
      packer.packString(this.someString);
      packer.packArrayHeader(this.someBytes.length);
      packer.writePayload(this.someBytes);
    }
  }
}
```

Example message using AnnotationMessageSerializer:

```java
import java.io.IOException;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;
import pl.cdfn.easymessage.serialization.annotation.AnnotationMessageSerializer;
import pl.cdfn.easymessage.serialization.annotation.MessagePackField;

public class MyMessage implements AnnotationMessageSerializer {

  @MessagePackField
  private Integer someInt;
  @MessagePackField
  private String someString;
  @MessagePackField
  private byte[] someBytes;

  public MyMessage(int someInt, String someString, byte[] someBytes) {
    this.someInt = someInt;
    this.someString = someString;
    this.someBytes = someBytes;
  }
}
```

Example message handler:

```java
import pl.cdfn.easymessage.handler.MessageHandler;
import pl.cdfn.easymessage.example.MyMessage;

public class MyMessageHandler extends MessageHandler<MyMessage> {

  @Override
  public void accept(MyMessage message) {
    System.out.println(message);
  }

  // If that returns true, handler will get deleted after handling request
  // Useful for e.g. downloading configuration from master server
  @Override
  public boolean isOneTime() {
    return false;
  }
}
```

Fully working example:

```java
import io.lettuce.core.RedisClient;
import pl.cdfn.easymessage.MessagePublisher;
import pl.cdfn.easymessage.MessagePubsubListener;
import pl.cdfn.easymessage.example.MyMessage;
import pl.cdfn.easymessage.handler.MessageHandler;
import pl.cdfn.easymessage.handler.MessageHandlerRegistry;

public class EasyMessageExample implements Runnable {

  private final RedisClient redisClient;
  private final MessagePublisher publisher;
  private final MessagePubsubListener pubsubListener;

  public MessagePublisherExample(RedisClient client, String prefix) {
    this.redisClient = client;
    this.publisher = new MessagePublisher(client, preifx);
    this.pubsubListener = new MessagePubsubListener(prefix, MessageHandlerRegistry.INSTANCE);
  }

  @Override
  public void run() {
    // This registers pub-sub listener which triggers message handlers.
    // You want to do that only on initialization, once.
    this.pubsubListener.register(this.redisClient);

    // Creating handler, it'll trigger after any MyMessage is sent
    MessageHandlerRegistry.INSTANCE.addHandler(MyMessage.class, new MessageHandler<>() {
      @Override
      public void accept(MyMessage message) {
        System.out.println(message);
      }

      @Override
      public boolean isOneTime() {
        return true;
      }
    });
    // Creating MyMessage instance which implements MessagePackSerializable
    MyMessage message = new MyMessage(1, "someString", new byte[]{1, 2, 3});

    // Publishing message
    this.publisher.publish(message);
    
    // At this point, handler we created previous should trigger.
    // After executing MessageHandler#accept, it should get deleted as it is one-time handler.
  }
}
```