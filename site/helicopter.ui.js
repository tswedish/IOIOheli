(function(){

var pubnub = null;
var channel = 'test_channel_AQei6';
var debug_channel = 'error_debug'
var pubkey = 'demo';
var subkey = 'demo';
var debugcount = 0;

var box, input, codes;
var circlePosX = 150;
var circlePosY = 200;
var circleWidth = 20;

//Helicopter State (Autopilot)
var set_pitch = 0.01;
var set_yaw = 0.0;
var set_mainpwr = 10;
var fwdon = false;
var bckon = false;
var orientationErrors = {"PitchErr":0,"YawErr":0};

document.body.addEventListener('touchmove', function(event) {
  event.preventDefault();
  var touch = event.touches[0];
  if (touch.target == document.getElementById("control_surface")) {
  if (event.touches.length > 1) {
  var touch2 = event.touches[1];
    circleWidth = 0.5*Math.sqrt(Math.pow(Math.abs(touch.pageX - touch2.pageX),2)
                          + Math.pow(Math.abs(touch.pageY - touch2.pageY),2));
    prog((touch.pageX+touch2.pageX)/2, (touch.pageY+touch2.pageY)/2);
  } else {
    prog(touch.pageX,touch.pageY);
  }
  }

}, false);

document.body.addEventListener('touchend', function(event) {
  returnCircle();
}, false);

function returnCircle() {
  circlePosX = 150;
  circlePosY = 200;
}

function prog(x,y)
{
  circlePosX = x;
  circlePosY = y;
}

function sendOrientation() {

  var command = '{"pitch":'+set_pitch+','
                +'"yaw":'+set_yaw+','
                +'"mainPwr":'+set_mainpwr
                +'}';

  try {
    pubnub.publish({
      channel : channel,
      message : command,
      x       : function(){}
    });
  }
  catch(err) {
    console.log(err+' ...Probably not subscribed yet!');
  }

  command_received(command);


}


// Decrypt PUBNUB keys
function pass_receive(key) {

  var pubkey_cipher = [12, 86, 91, 87,
                      11, 65, 67, 87,
                      15, 7, 91, 84];
  var subkey_cipher = [93, 5, 14, 82,
                      8, 76, 18, 13,
                      85, 85, 15, 81];
  var channel_cipher = [12, 20, 31, 10,
                      25, 29, 31,  0,
                      25, 62, 8, 13,
                      8, 26, 29, 10, 1];

  function decrypt(cipher, key)  {
    var plaintext = new Array;
    //cipher is int array, key is char array
    cipher.forEach(function(v,i,arr) {
      plaintext[i] =
        String.fromCharCode(key[i%key.length].charCodeAt(0) ^ v);
    });
    plaintext = plaintext.join('');
    console.log(plaintext);
    return plaintext;
  }

  // end of real keys encrypted
  pubkey = 'pub-929c305c-87e9-4aae-92c3-'+decrypt(pubkey_cipher,key);
  subkey = 'sub-3dfeb000-7925-11e1-9bd8-'+decrypt(subkey_cipher,key);
  channel = ''+decrypt(channel_cipher,key);
}

// update last received
function command_received(text) {
  var command_obj = eval('(' + text + ')');
  /*
  box.innerHTML = ('PitchError:   '
              +(set_pitch).toFixed(3)
              ).replace( /[<>]/g, '' ) +
            '<br>'+
             ('RotErr: '
              +(set_yaw).toFixed(3)
              ).replace( /[<>]/g, '' ) +
            '<br>'+
             ('Rotor Power: '
              +(100*command_obj.mainPwr).toFixed(3)
              ).replace( /[<>]/g, '' ) +
            '<br>'
            ;
            */
  console.log(text);
}

function debug_received(text) {
  console.log(text);
  orientationErrors = eval('(' + text + ')');
}



function pubnub_init()  {
    pubnub = PUBNUB.init({
       'publish_key'   : pubkey,
       'subscribe_key' : subkey,
       'ssl'           : false
    });

    box     = pubnub.$('box');
    input   = pubnub.$('input');
    codes   = pubnub.$('codes');

    pubnub.ready();
    //pubnub.subscribe({
    //    channel : debug_channel,
    //    message : debug_received
    //});

    console.log('Created Pubnub.');
    console.log('pubkey: '+pubkey);
    console.log('subkey: '+subkey);
    console.log('channel: '+channel);
}

new FastButton(document.getElementById('subbutton'), function() {
  var subinput = document.getElementById('input').value;
  if(!pubnub) {
    console.log('Pass Entered: '+subinput);
    if (subinput)  {
      pass_receive(subinput);
      pubnub_init();
    }
    else {
      pubnub_init();
    }
    document.getElementById('input').value = 'Subscribe Attempt Sent';
    setInterval(function(){
       sendOrientation();
    }, 1000);

  }
  else  {
    console.log('Already Subscribed');
  }
  document.getElementById('toptext').style.display = 'none';
  document.getElementById('control_frame').style.display = 'inline';


  window.requestAnimFrame = (function(callback) {
        return window.requestAnimationFrame ||
        window.webkitRequestAnimationFrame ||
        window.mozRequestAnimationFrame ||
        window.oRequestAnimationFrame ||
        window.msRequestAnimationFrame ||
        function(callback) {
          window.setTimeout(callback, 1000 / 60);
        };
      })();

      function animate() {
        var canvas = document.getElementById('control_surface');
        var context = canvas.getContext('2d');

        // update
        set_yaw = circlePosX-150;
        set_pitch   = circlePosY-200;
        set_mainpwr = circleWidth*0.01;

        // clear
        context.clearRect(0, 0, canvas.width, canvas.height);

        // draw stuff
        context.beginPath();
        context.rect(145, 0, 10, 400);
        context.fillStyle = "rgb(55,55,55)";
        context.fill();

        context.beginPath();
        context.rect(0, 195, 300, 10);
        context.fillStyle = "rgb(55,55,55)";
        context.fill();

        context.beginPath();
        context.arc(150,
                    200,
                    10, 0, Math.PI*2, false); // Draw a circle
        context.closePath();
        context.fillStyle = "rgb(50,50,50)";
        context.fill();

        context.beginPath(); // Start the path
        context.arc(circlePosX,
                    circlePosY,
                    circleWidth, 0, Math.PI*2, false); // Draw a circle
        context.closePath(); // Close the path
        context.fillStyle = "rgba(153, 51, 0, 0.7)";
        context.fill(); // Fill the path



        // request new frame
        requestAnimFrame(function() {
          animate();
        });
      }
      animate();

});


})();
