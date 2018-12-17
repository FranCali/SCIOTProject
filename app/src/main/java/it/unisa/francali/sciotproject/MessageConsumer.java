package it.unisa.francali.sciotproject;

import android.util.Log;

import com.rabbitmq.client.*;

public class MessageConsumer {
    private static final String EXCHANGE_NAME = "iot/rooms";

    public static void consume()throws Exception{
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("192.168.1.115");
        factory.setPort(5672);
        factory.setUsername("guest");
        factory.setPassword("guest");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, EXCHANGE_NAME, "");

        Log.d("Connection established", " [*] Waiting for messages. To exit press CTRL+C");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            Log.d("Success", " [x] Received '" + message + "'");
        };
        channel.basicConsume(queueName, true, deliverCallback, consumerTag -> { });
    }
}
