package domobi.IOIO.heli;

//import ioio.lib.api.DigitalOutput;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;
import org.json.JSONObject;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.TwiMaster;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.SensorManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.SeekBar;
import android.widget.ToggleButton;
import android.widget.TextView;
import android.widget.Button;
import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;

/**
 * This is the main activity of the HelloIOIO example application.
 * 
 * It displays a toggle button on the screen, which enables control of the
 * on-board LED. This example shows a very simple usage of the IOIO, by using
 * the {@link IOIOActivity} class. For a more advanced use case, see the
 * HelloIOIOPower example.
 */
public class MainActivity extends IOIOActivity implements SurfaceHolder.Callback {
	private ToggleButton toggleEnableGui_;
	private ToggleButton toggleVideoCapture_;

	private TextView statusText_;
	private TextView errText_;
	
	private boolean videoRecording = false;
	private float cnt = 0;


	// Also pubnub updated to interact with Autopilot/AssistedFlyer
	// (different channels)
	// FlightPlanner(AssistedFlyer,Waypoint[])
	// waypoint is Location/State(Land...etc)
	// AssistedFlyer(Helicopter,Autopilot)

	private MediaRecorder mrec;
	SurfaceHolder holder;
	SurfaceView cameraView;
	File video;
    private Camera mCamera;

	private SensorManager sM;
	private Odometry odom;
	private Autopilot autopilot;
	private Superpilot superpilot;
	private TwiMaster twi;

	private Helicopter heli = new Helicopter();
	//private Helicopter test = new Helicopter();
	// API keys here, uncomment for Demo Keys
	// private apiConfigTemplate config = new apiConfigTemplate();

	// Comment out if using demo keys
	private apiConfig config = new apiConfig();

	private Pubnub pubnub = config.pubnub;
	Timer timer = new Timer();

	class PubnubCallback extends Callback {

		public void successCallback(String channel, Object message) {
			// call update method
			try {
				// setText(message.toString(), textMainLift_);
				JSONObject setpoint = new JSONObject(message.toString());

				JSONObject lastError = new JSONObject("{\"PitchErr\":"
						+ Float.toString(autopilot.pitchError)
						+ ",\"PitchCorr\":"
						+ Float.toString(autopilot.kPt * autopilot.uPt
								+ autopilot.kIt * autopilot.uIt + autopilot.kDt
								* autopilot.uDt) + "}");
				String Pubchannel = "error_debug";
				pubnub.publish(Pubchannel, lastError, new Callback() {
				});
				
				heli = superpilot.getHeli(setpoint);
				//autopilot.setOrientation(setpoint);
				heli.setConnection(true);

				// setNumber(autopilot.getSetPoint().mainPwr, textMainLift_);
			} catch (Exception e) {
				setText(e.toString(), errText_);
			}
		}

	}

	class CheckForHeartbeat extends TimerTask {
		@Override
		public void run() {
			if (heli.connectionBeat()) {
				heli.setConnection(false);
			} else {
				heli.setMainPwr((float) (heli.getMainPwr() * 0.8));
			}
		}
	}

	/**
	 * Called when the activity is first created. Here we normally initialize
	 * our GUI.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		/* GUI */
		
		// Camera
		mrec = new MediaRecorder();
	
		initMediaRecorder();
		
		setContentView(R.layout.main);
		
		cameraView = (SurfaceView) findViewById(R.id.surfaceView1);
        holder = cameraView.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		
		toggleEnableGui_ = (ToggleButton) findViewById(R.id.toggleEnableGui);
		toggleVideoCapture_ = (ToggleButton) findViewById(R.id.toggleVideoCapture);

		statusText_ = (TextView) findViewById(R.id.textView1);
		errText_ = (TextView) findViewById(R.id.textView3);

		/* Pubnub Subscription and Callback */
		String subChannel = "autopilot_channel";
		Hashtable args = new Hashtable(1);
		args.put("channel", subChannel);
		Callback pubnubCallback = new PubnubCallback();
		try {
			pubnub.subscribe(args, pubnubCallback);
		} catch (Exception e) {
			setText("ERROR on Subscribe", errText_);
		}

		// Pubnub Lost Connection Timer (connection dropped from pubnub
		// Handler?)
		TimerTask checkHeartbeat = new CheckForHeartbeat();
		timer.scheduleAtFixedRate(checkHeartbeat, 0, 2500);

		// Odometry
		sM = (SensorManager) getSystemService(getBaseContext().SENSOR_SERVICE);
		odom = new Odometry(sM);
		autopilot = new Autopilot();
		superpilot = new Superpilot();
	}

	/**
	 * This is the thread on which all the IOIO activity happens. It will be run
	 * every time the application is resumed and aborted when it is paused. The
	 * method setup() will be called right after a connection with the IOIO has
	 * been established (which might happen several times!). Then, loop() will
	 * be called repetitively until the IOIO gets disconnected.
	 */
	class Looper extends BaseIOIOLooper {

		/** The on-board LED. */
		// private DigitalOutput led_;
		private PwmOutput pwmTailUp;
		private PwmOutput pwmTailDown;
		private PwmOutput pwmRotor1;
		private PwmOutput pwmRotor2;

		private float tailUpPwr;
		private float tailDownPwr;
		private float rotor1Pwr;
		private float rotor2Pwr;

		private float tailMax = 0.6f;
		private float tailMin = 0.05f;
		private float mainMax = 0.8f;
		private float mainMin = 0.2f;

		/**
		 * Called every time a connection with IOIO has been established.
		 * Typically used to open pins.
		 * 
		 * @throws ConnectionLostException
		 *             When IOIO connection is lost.
		 * 
		 * @see ioio.lib.util.AbstractIOIOActivity.IOIOThread#setup()
		 */
		@Override
		protected void setup() throws ConnectionLostException {
			
			try {
                mrec.start();
                videoRecording = true;
            } catch (Exception e) { 
            	mrec.stop();
            	setText(e.getMessage(), statusText_);
                mrec.release();
            }
			
			pwmTailUp = ioio_.openPwmOutput(14, 100);
			pwmTailDown = ioio_.openPwmOutput(13, 100);
			pwmRotor1 = ioio_.openPwmOutput(12, 100);
			pwmRotor2 = ioio_.openPwmOutput(11, 100);

			twi = ioio_.openTwiMaster(0, TwiMaster.Rate.RATE_100KHz, false);
			odom.setTwi(twi);

			autopilot.setOrientation(odom.getOrientation().pitch, 0.0f, 0.0f);
		}

		/**
		 * Called repetitively while the IOIO is connected.
		 * 
		 * @throws ConnectionLostException
		 *             When IOIO connection is lost.
		 * @throws InterruptedException
		 * 
		 * @see ioio.lib.util.AbstractIOIOActivity.IOIOThread#loop()
		 */
		@Override
		public void loop() throws ConnectionLostException, InterruptedException {
			
			if (toggleVideoCapture_.isChecked())	{
				setText("Releasing Video...",statusText_);
				mrec.stop();
				mrec.release();
			}

			
			/* Use Gui to Set Helicopter State (if enabled) */
			if (toggleEnableGui_.isChecked()) {
				UpdateHelicopterStatefromGui();
				heli.setConnection(true);
			} else {
				/* Update Helicopter State on GUI */
				//heli = autopilot.getNextState(odom);

				// autopilot.kDt = (10f*(float)seekMainLift_.getProgress())/
				// ((float)seekMainLift_.getMax());
				/*
				 * autopilot.kIt = ((float)seekRotationTrim_.getProgress())/
				 * ((float)seekRotationTrim_.getMax()); autopilot.kDt =
				 * ((float)seekTailTrim_.getProgress())/
				 * ((float)seekTailTrim_.getMax());
				 */
			}

			/* Take the Helicopter State and Apply to IOIO */
			UpdateIOIOfromHelicopterState(heli);
			pwmTailUp.setDutyCycle(tailUpPwr);
			pwmTailDown.setDutyCycle(tailDownPwr);
			pwmRotor1.setDutyCycle(rotor1Pwr);
			pwmRotor2.setDutyCycle(rotor2Pwr);

			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				//mrec.release();
			}
		}

		private void UpdateHelicopterStatefromGui() {
			// Does nothing for now
		}

		private void UpdateIOIOfromHelicopterState(Helicopter helicopter) {

			float tailPwrTrimmed = helicopter.getTailPwr()
					+ helicopter.getTailTrim();
			float mainRotationTrimmed = helicopter.getRotationPwr()
					+ helicopter.getRotationTrim();

			/* Clip if Over Max Ranges */
			if (tailPwrTrimmed > tailMax) {
				tailPwrTrimmed = tailMax;
			}
			if (tailPwrTrimmed < -tailMax) {
				tailPwrTrimmed = -tailMax;
			}
			if (mainRotationTrimmed > mainMax) {
				mainRotationTrimmed = mainMax;
			}
			if (mainRotationTrimmed < -mainMax) {
				mainRotationTrimmed = -mainMax;
			}

			/* Tail Logic */
			if (tailPwrTrimmed >= 0) {
				tailUpPwr = tailPwrTrimmed;
				tailDownPwr = 0.0f;
			} else if (tailPwrTrimmed < 0) {
				tailUpPwr = 0.0f;
				tailDownPwr = -tailPwrTrimmed;
			}

			/* Main Rotors */
			if (mainRotationTrimmed >= 0) {
				rotor1Pwr = helicopter.getMainPwr();
				rotor2Pwr = helicopter.getMainPwr() * (1 - mainRotationTrimmed);
			}
			if (mainRotationTrimmed < 0) {
				rotor1Pwr = helicopter.getMainPwr() * (1 + mainRotationTrimmed);
				rotor2Pwr = helicopter.getMainPwr();
			}

			// Set pwr to zero if below Motor startup friction
			if (tailUpPwr < tailMin) {
				tailUpPwr = 0.0f;
			}
			if (tailDownPwr < tailMin) {
				tailDownPwr = 0.0f;
			}
			if (rotor1Pwr < mainMin) {
				rotor1Pwr = 0.0f;
			}
			if (rotor2Pwr < mainMin) {
				rotor2Pwr = 0.0f;
			}
		}

	}
	 
	private void initMediaRecorder(){
			mrec.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
			mrec.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
	        CamcorderProfile camcorderProfile_HQ = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
	        mrec.setProfile(camcorderProfile_HQ);
	        mrec.setOutputFile("/sdcard/myvideo.mp4");
	        //mrec.setMaxDuration(60000); // Set max duration 60 sec.
	        //mrec.setMaxFileSize(5000000); // Set max file size 5M
		}
		
	private void prepareMediaRecorder(){
			mrec.setPreviewDisplay(holder.getSurface());
			try {
				mrec.prepare();
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	
	/**
	 * A method to create our IOIO thread.
	 * 
	 * @see ioio.lib.util.AbstractIOIOActivity#createIOIOThread()
	 */
	@Override
	protected IOIOLooper createIOIOLooper() {
		return new Looper();
	}

	private void setNumber(float f, final TextView textBox) {
		final String str = String.format("%.2f", f);
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				textBox.setText(str);
			}
		});
	}

	private void setText(final String str, final TextView textBox) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				textBox.setText(str);
			}
		});
	}

	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void surfaceCreated(SurfaceHolder arg0) {
		prepareMediaRecorder();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
	//	mCamera.stopPreview();
    //    mCamera.release();	
	}

}