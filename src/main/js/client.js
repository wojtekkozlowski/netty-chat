var net = require('net');
var fs = require('fs');
var tls = require('tls');

process.env.NODE_TLS_REJECT_UNAUTHORIZED = "0";

var args = process.argv.slice(2);

var clients = [];

var host = args[0] || 'localhost';
var useSSL = args[1] == 'true';
var port = args[2] || 8000;
var totalConnections = args[3];
var cps = args[4];

console.log(useSSL)

var connsSpread = totalConnections / cps;

var tlsOptions = {
    host: host,
    key: fs.readFileSync('../resources/keystore.pem'),
    requestCert: true,
    rejectUnauthorized: false,
    passphrase: "qwerty"
};
console.log(tlsOptions)

console.log("total connections: " + totalConnections);
console.log("rate: "+ cps + "/s")
console.log("duration: " + connsSpread + "s");

for (var i = 0; i < totalConnections; i++) {
    setTimeout(function () {
        if(useSSL){
            clients.push(createAndStartSSLClient())
        } else {
            clients.push(createAndStartClient())    
        }
    }, (Math.random() * 1000 * connsSpread) + 1000);
}

function createAndStartSSLClient() {
    var client = tls.connect(port, tlsOptions, function () {
        // console.log("new client connected")
        client.write('<hello>0800</hello>');
    });

    client.on('data', function (data) {
        setTimeout(function () {
            client.write('<hello>0800</hello>');
        }, Math.random() * 1000);
    });

    // client.on('end', function () {
        // console.log('end')
    // });
    // client.on('close', function () {
        // console.log('close');
    // });

    client.setTimeout(10000, function () {
        console.log('timeout and reconnect')
        client.destroy();
        clients.push(createAndStartSSLClient())
    });

}

function createAndStartClient() {
    var client = new net.Socket();
    client.connect(port, tlsOptions.host, function () {
        // console.log('Connected');
        client.write('<hello>0800</hello>');
    });
    client.on('data', function (data) {
        // console.log('Received: ' + String(data).trim());
        // client.destroy(); // kill client after server's response
        setTimeout(function () {
            client.write('<hello>0800</hello>');
        }, Math.random() * 1000 * connsSpread);
    });
    client.on('close', function () {
        console.log('Connection closed');
    });
}
