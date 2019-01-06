var amqp = require('amqplib');

exports.handler = function(context, event) {
    var request = JSON.parse(event.body);
    var key = "rooms";
    var message = request.roomNumber+"-"+request.isSitting;

    amqp.connect('amqp://guest:guest@10.0.2.15:5672').then(function(conn) {
    return conn.createChannel().then(function(ch) {
        var ex = 'iot/rooms';
        var ok = ch.assertExchange(ex, 'topic', {durable: false});
        return ok.then(function() {
            ch.publish(ex, key, Buffer.from(message));
            //console.log(" [x] Sent %s:'%s'", key, message);
            return ch.close();
        });
    }).finally(function() { conn.close();  })
    }).catch(console.log);
    
    context.callback(message);
};
