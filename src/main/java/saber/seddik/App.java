package saber.seddik;

import com.microsoft.azure.servicebus.*;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
public class App {
    private static final String TOPIC = "service-bus-bug-topic";
    private static final String SUBSCRIPTION = "service-bus-bug-subscription";
    private static final String SUBSCRIPTION_PATH = String.format("%s/subscriptions/%s", TOPIC, SUBSCRIPTION);
    private static final int MAX_CONCURRENT_SESSIONS = 1;
    private static final int MAX_CONCURRENT_CALL_PER_SESSIONS = 1;
    private static final ScheduledExecutorService SCHEDULER =
            Executors.newScheduledThreadPool(5);

    @SneakyThrows
    public static void main(String[] args) {
        String connectionString = args[0];
        LOGGER.info("Start Sending messages");
        TopicClient sendClient = new TopicClient(new ConnectionStringBuilder(connectionString, TOPIC));
        scheduleSend(sendClient, () -> buildAMessage("session_1"));
        scheduleSend(sendClient, () -> buildAMessage("session_2"));
        LOGGER.info("End Sending messages");
        LOGGER.info("Registering async Consumer messages");
        var sessionHandlerOptions = new SessionHandlerOptions(MAX_CONCURRENT_SESSIONS, MAX_CONCURRENT_CALL_PER_SESSIONS, false, Duration.ofSeconds(30));
        var client = new SubscriptionClient(
                new ConnectionStringBuilder(connectionString, SUBSCRIPTION_PATH), ReceiveMode.PEEKLOCK);
        client.registerSessionHandler(new SessionHandler(), sessionHandlerOptions, Executors.newCachedThreadPool());

        //Let's wait this thread
        Thread.onSpinWait();
    }

    @SneakyThrows
    private static void scheduleSend(TopicClient sendClient, Supplier<IMessage> supplier) {
        Runnable task = () -> {
            try {
                LOGGER.info("Sending message");
                sendClient.send(supplier.get());
            } catch (InterruptedException | ServiceBusException e) {
                e.printStackTrace();
            }
        };
        SCHEDULER.scheduleAtFixedRate(task, 0, 10, TimeUnit.SECONDS);
    }

    private static Message buildAMessage(String sessionId) {
        Message message = new Message("Session: '" + sessionId + "' .This is the message body " + UUID.randomUUID().toString());
        message.setSessionId(sessionId);
        return message;
    }

    private static class SessionHandler implements ISessionHandler {

        @Override
        @SneakyThrows
        public CompletableFuture<Void> onMessageAsync(IMessageSession session, IMessage message) {
            var payload = new String(message.getBody(), UTF_8);
            LOGGER.info("Messages= '{}'", payload);
            // Lock duration is set 1 minutes on the subscription.
//            LOGGER.info("Wait for 2 minutes to cause lock lost from the server side.");
//            Thread.sleep(2 * 60 * 1_000);
//            LOGGER.info("Thread released after a wait of 2 minutes.");
            return session.completeAsync(message.getLockToken());
        }

        @Override
        public CompletableFuture<Void> OnCloseSessionAsync(IMessageSession session) {
            return null;
        }

        @Override
        public void notifyException(Throwable exception, ExceptionPhase phase) {

        }
    }
}
