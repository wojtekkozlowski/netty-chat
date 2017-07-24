var net = require('net');
var fs = require('fs');
var tls = require('tls');

var tlsOptions = {
    host: '192.168.0.236',
    key: fs.readFileSync('../resources/keystore.pem'),
    requestCert: true,
    rejectUnauthorized: false,
    passphrase: "qwerty"
};

process.env.NODE_TLS_REJECT_UNAUTHORIZED = "0";

var clients = [];

for (var i = 0; i < 10000; i++) {
    setTimeout(function () {
        // clients.push(createAndStartClient())
        clients.push(createAndStartSSLClient())
    }, (Math.random() * 5000) + 1000);
}

function createAndStartSSLClient() {
    var client = tls.connect(8000, tlsOptions, function () {
        // console.log("new client connected")
        client.write('<hello>0800</hello>');
    });

    client.on('data', function (data) {
        // console.log('Received: ' + String(data).trim());
        setTimeout(function () {
            client.write('<hello>0800</hello>');
        }, Math.random() * 1000);
    });

    client.on('end', function () {
        // console.log('end')
    });
    client.on('close', function () {
        // console.log('close');
    });

    client.setTimeout(10000, function () {
        console.log('timeout and reconnect')
        client.destroy();
        clients.push(createAndStartSSLClient())
    });

}

function createAndStartClient() {
    var client = new net.Socket();
    client.connect(8000, '192.168.0.236', function () {
        console.log('Connected');
        client.write('<hello>0800</hello>');
    });
    client.on('data', function (data) {
        console.log('Received: ' + String(data).trim());
        // client.destroy(); // kill client after server's response
        setTimeout(function () {
            client.write('<hello>0800</hello>');
        }, Math.random() * 1000);
    });
    client.on('close', function () {
        console.log('Connection closed');
    });
}
