var app = require('express')();
var http = require('http').Server(app);
var express = require('express');
var io = require('socket.io')(http);
var port = process.env.PORT || 3000;
var path = require('path');

app.use(express.static(path.join(__dirname, 'js')));

app.set('view engine', 'ejs');

app.get('/chat', function(req, res){
  res.sendFile(__dirname + '/index.html');
});
io.on('connection', function(socket){
  socket.on('chat message', function(msg){
    io.emit('chat message', msg);
  });
});


app.get('/', function(req, res) {
	res.render('index');
});

app.get('/contact', function(req, res) {
	res.render('contact');
});


app.get('/profile/:name', function(req, res) {
	var data = {age:29, job: 'ninja', hobbies: ['eating', 'fighting', 'fishing']};
	res.render('profile', {person: req.params.name, data: data});
});

http.listen(port, function(){
  console.log('listening on *:' + port);
});
