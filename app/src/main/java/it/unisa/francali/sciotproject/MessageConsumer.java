package it.unisa.francali.sciotproject;

import com.rabbitmq.client.*;

public class MessageConsumer {
    private static final String EXCHANGE_NAME = "iot/rooms";

    public static void consume()throws Exception{
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("172.19.28.197");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, EXCHANGE_NAME, "");

        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [x] Received '" + message + "'");
        };
        channel.basicConsume(queueName, true, deliverCallback, consumerTag -> { });
    }
}
