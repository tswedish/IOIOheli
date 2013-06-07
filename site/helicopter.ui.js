(function(){

var pubnub = null;
var channel = 'test_channel_AQei6';
var debug_channel = 'error_debug'
var pubkey = 'demo';
var subkey = 'demo';
var debugcount = 0;

var box, input, codes;

//Helicopter State (Autopilot)
var set_pitch = 0.01;
var set_yaw = 0.0;
var set_mainpwr = 0.0;
var fwdon = false;
var bckon = false;
var orientationErrors = {"PitchErr":0,"YawErr":0};

document.body.addEventListener('touchmove', function(event) {
  event.preventDefault();
  var touch = event.touches[0];
  if (touch.target == document.getElementById("progressbar") ||
      touch.target == document.getElementById("indicator_right") ||
      touch.target == document.getElementById("indicator_left")) {
    prog(touch.pageX);
  } else if (touch.target == document.getElementById("pwrprogressbar") ||
            touch.target == document.getElementById("pwrindicator")) {
    pwrprog(touch.pageY);
  }
}, false);

// Rotation Progress Bar Test
var actualprogress = 0;  // current value
var maxprogress = 300;
var minprogress = 150;

function prog(x)
{
  var indicator_right = document.getElementById("indicator_right");
  var indicator_left = document.getElementById("indicator_left");

  actualprogress = x-5;
  if(actualprogress >= maxprogress) {actualprogress = maxprogress;}
  if(actualprogress >= minprogress) {
    indicator_right.style.width=actualprogress + 'px';
    indicator_left.style.width=minprogress+'px';
  } else  {
    indicator_right.style.width=minprogress+'px';
    indicator_left.style.width=actualprogress + 'px';
  }
  set_yaw = (2*(actualprogress/maxprogress)-1)/2;
}

// Main Power Progress Bar Test
var pwractualprogress = 0;  // current value
var pwrmaxprogress = 303;
var pwrminprogress = 0;

function pwrprog(y)
{
  var indicator = document.getElementById("pwrindicator");

  pwractualprogress = y-100;
  if(pwractualprogress >= pwrmaxprogress) {pwractualprogress = pwrmaxprogress;}
  if(pwractualprogress <= pwrminprogress) {pwractualprogress = pwrminprogress;}
  indicator.style.height=pwractualprogress+'px';
  set_mainpwr = 1-pwractualprogress/pwrmaxprogress;

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
}

function debug_received(text) {
  console.log(text);
  orientationErrors = eval('(' + text + ')');
}

// Reconfigure these buttons
// Fast Button Increase Power
new FastButton(document.getElementById('increasepwr'), function() {
  var intext = document.getElementById('increasepwr');
  var detext = document.getElementById('decreasepwr');
  if (!fwdon)  {
    set_pitch = 0.1;
    intext.style.color = '#993300';
    detext.style.color = '#999999';
    fwdon = true;
    bckon = false;
  } else  {
    intext.style.color = '#999999';
    fwdon = false;
    set_pitch = 0.01;
  }
});

// Fast Button Decrease Power
new FastButton(document.getElementById('decreasepwr'), function() {
  var intext = document.getElementById('increasepwr');
  var detext = document.getElementById('decreasepwr');

  if (!bckon)  {
    set_pitch = -0.1;
    detext.style.color = '#993300';
    intext.style.color = '#999999';
    bckon = true;
    fwdon = false;
  } else  {
    detext.style.color = '#999999';
    bckon = false;
    set_pitch = 0.01;
  }
});

// Fast Button Abort
new FastButton(document.getElementById('abort'), function() {
  set_mainpwr = 0.0;
  set_pitch = 0;
  set_yaw = 0;

  var intext = document.getElementById('increasepwr');
  var detext = document.getElementById('decreasepwr');
  var indicator_right = document.getElementById("indicator_right");
  var indicator_left = document.getElementById("indicator_left");
  var indicator = document.getElementById("pwrindicator");

  indicator.style.height = '303px';
  indicator_left.style.width = '150px';
  indicator_right.style.width = '150px';

  intext.style.color = '#999999';
  detext.style.color = '#999999';

  bckon = false;
  fwdon = false;

  sendOrientation();
});

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
  document.getElementById('control_surface').style.display = 'inline';
});


})();
