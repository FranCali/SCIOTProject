var amqp = require('amqplib');

exports.handler = function(context, event) {
    var request = JSON.parse(event.body);
    
    var message = request.msg;

    amqp.connect('amqp://guest:guest@10.0.2.15:5672').then(function(conn) {
    return conn.createChannel().then(function(ch) {
        var ex = 'iot/chat';
        var ok = ch.assertExchange(ex, 'fanout', {durable: false});
        return ok.then(function() {
            ch.publish(ex, '', Buffer.from(message.toString()));
            return ch.close();
        });
    }).finally(function() { conn.close();  })
    }).catch(console.log);
    
    context.callback(message.toString());
};
