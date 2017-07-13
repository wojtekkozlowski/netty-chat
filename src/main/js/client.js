var net = require('net');
var fs = require('fs');
var tlsOptions = {
  key: fs.readFileSync('../resources/keystore.pem'),
  requestCert: true,
  rejectUnauthorized: true
};

var clients = [];

for (var i=0;i<4000;i++){
	setTimeout(function(){	
		clients.push(createAndStartClient())
	},(Math.random() * 4000) + 1000);
}

function createAndStartClient(){
	var client = new net.Socket();
	client.connect(8000, '127.0.0.1', function() {
		console.log('Connected');
			client.write('<hello>0800</hello>');
		});
		client.on('data', function(data) {
			console.log('Received: ' + String(data).trim());
			// client.destroy(); // kill client after server's response
			setTimeout(function(){
			client.write('<hello>0800</hello>');
		},Math.random()*1000);
	});
	client.on('close', function() {
		console.log('Connection closed');
	});
}
