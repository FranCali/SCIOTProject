var amqp = require('amqplib');
var BACKUP_QUEUE = "seatsBackup";
var backup_msg;    
var iot_seats_msg;
var new_backup_msg;

exports.handler = function(context, event) {
    var _event = JSON.parse(JSON.stringify(event));//data coming from roomsTracking queue
    iot_seats_msg = bin2string(_event.body.data);//formatted string ex. 1-true
    
    context.callback("okTrigger");
    consume_backup_msg();
    
};

function consume_backup_msg(){
	amqp.connect('amqp://guest:guest@10.0.2.15:5672').then(function(conn) {
  
	return conn.createChannel().then(function(ch) {

	var ok = ch.assertQueue(BACKUP_QUEUE, {durable:false});

	ok = ok.then(function(_qok) {
	  return ch.consume(BACKUP_QUEUE, function(msg) {
		    backup_msg = msg.content.toString();
			updateSeats(iot_seats_msg, backup_msg); 
         
           ch.close();
           conn.close();
	  }, {noAck: true});
	});

	return ok.then(function(_consumeOk) {
		
	});
	});
}).catch(console.warn);
}
    
function updateSeats(iot_seats_msg, backup_msg){
	console.log(backup_msg);
	var seats_msg_array = iot_seats_msg.split("-");
	var room_number = seats_msg_array[0];
	var is_sitting = seats_msg_array[1];//true if seats, false if leaves
	
	var backup_msg_array = backup_msg.split("-"); 	
	var seats_room_1 = backup_msg_array[0];
	var seats_room_2 = backup_msg_array[1];
	var seats_room_3 = backup_msg_array[2];



	switch(room_number){
		case "1": if(is_sitting == "true") {seats_room_1--;} else if(is_sitting == "false") {seats_room_1++;} break;
		case "2": if(is_sitting == "true") {seats_room_2--;} else if(is_sitting == "false") {seats_room_2++;} break;
		case "3": if(is_sitting == "true") {seats_room_3--;} else if(is_sitting == "false") {seats_room_3++;} break;
		default: break;
	}
	

	new_backup_msg = seats_room_1+"-"+seats_room_2+"-"+seats_room_3;
	send_to_backup_queue(new_backup_msg);
}

function send_to_backup_queue(msg){
	amqp.connect('amqp://guest:guest@10.0.2.15:5672').then(function(conn) {
		return conn.createChannel().then(function(ch) {
		    var ok = ch.assertQueue(BACKUP_QUEUE, {durable: false});
		    return ok.then(function(_qok) {
		    ch.sendToQueue(BACKUP_QUEUE, Buffer.from(msg));
		    return ch.close();
		    });
		}).finally(function() { 
		        
		    });
	}).catch(console.warn);
}

function bin2string(array){
  var result = "";
  for(var i = 0; i < array.length; ++i){
    result+= (String.fromCharCode(array[i]));
  }
  return result;
}






