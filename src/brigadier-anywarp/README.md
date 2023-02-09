# Brigadier AnyWarp

----

> This module requires Java 11+

Brigadier AnyWarp is designed for redirect interface calls to other objects.

With AnyWarp, it is very easy to do:

```java
interface CommandSender {
    void sendMessage(String message);
}

interface MyCommandSender extends CommandSender {
    void myMethod();
}


/*synthetic*/ final class RedirectCommandSender implements MyCommandSender, CommandSender { // Generated in runtime by AnyWarp
    final CommandSender agent;
    final MyCommandSender proxy;

    public void sendMessage(String message) { agent.sendMessage(message); }

    public void myMethod() { proxy.myMethod(); }
}
```
