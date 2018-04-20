var express = require('express');
var app = express();
var path = require('path');
var http = require('http').Server(app);
var io = require('socket.io').listen(server);

app.use(express.static(path.join(__dirname, 'js')));

app.get('/', function(req, res){
	res.sendFile(__dirname + '/chatting/index.html');
});

io.on('connection', function(socket){
  socket.on('chat message', function(msg){
    io.emit('chat message', msg);
  });
});

app.get('/contact', function(req, res){
	res.send('this is the contact page');
});

// :id ??
app.get('/profile/:id', function(req, res) {
	res.send('You requested to see a profile with the id of ' + req.params.id);
})

app.listen(3000);
