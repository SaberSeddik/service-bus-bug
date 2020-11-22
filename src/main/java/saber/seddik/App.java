package saber.seddik;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.models.ReceiveMode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
public class App {
    private static final String TOPIC = "service-bus-bug-topic";
    private static final String SUBSCRIPTION = "service-bus-bug-subscription";
    private static final int MAX_CONCURRENT_SESSIONS = 1;
    private static final int MAX_CONCURRENT_CALL_PER_SESSIONS = 1;

    @SneakyThrows
    public static void main(String[] args) {
        String connectionString = args[0];
        LOGGER.info("Start Sending messages");
        var sendClient = new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .sender()
                .topicName(TOPIC)
                .buildClient();

        // Send test Messages
        for (int i = 0; i < 10; i++) {
            sendClient.sendMessage(buildAMessage());
        }
        sendClient.close();
        LOGGER.info("End Sending messages");
        LOGGER.info("Registering async Consumer messages");
        var client = new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .sessionReceiver()
                .receiveMode(ReceiveMode.PEEK_LOCK)
                .topicName(TOPIC)
                .subscriptionName(SUBSCRIPTION)
                .disableAutoComplete()
                .buildAsyncClient();
        client.acceptNextSession()
                .flatMapMany(receiver -> receiver.receiveMessages().flatMap(message -> {
                    LOGGER.info(new String(message.getBody().toBytes(), UTF_8));
                    // Lock duration is set 1 minutes on the subscription.
                    LOGGER.info("Wait for 2 minutes to cause lock lost from the server side.");
                    try {
                        Thread.sleep(2 * 60 * 1_000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    LOGGER.info("Thread released after a wait of 2 minutes.");
                    return receiver.complete(message);
                }))
                .subscribe();
        //Let's wait this thread
        Thread.sleep(5*60*1_000);
    }

    private static ServiceBusMessage buildAMessage() {
        var message = new ServiceBusMessage("This is the message body " + UUID.randomUUID().toString());
        message.setSessionId("session_123");
        return message;
    }
}
