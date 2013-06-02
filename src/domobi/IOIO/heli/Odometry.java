package domobi.IOIO.heli;

import java.util.Timer;
import java.util.TimerTask;

import domobi.IOIO.heli.MainActivity.CheckForHeartbeat;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class Odometry implements SensorEventListener {
	
	private Sensor accel;
	private Sensor mag;
	private Orientation calculatedOrientation;
	private float[] magBin;
	private float[] accelBin;
	private FIR orientationPitchFilter;
	private FIR orientationYawFilterX;
	private FIR orientationYawFilterY;
	
	public Odometry(SensorManager sM)	{
		/* From Matlab, do I ever want to calculate these on the fly?
		 * Assuming cutoff of 10Hz (for 20Hz Autopilot), with Fs = 50Hz
		 * (from SENSOR_DELAY_GAME)...but going to log at equal intervals...
		 */
		float[] h = {0.0092f,0.3194f,0.3321f,0.3321f,0.3194f,0.0092f};

		
		accel = sM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mag = sM.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		sM.registerListener(this,accel,SensorManager.SENSOR_DELAY_GAME);
		sM.registerListener(this,mag,SensorManager.SENSOR_DELAY_GAME);
		
		orientationPitchFilter = new FIR(h);
		orientationYawFilterX = new FIR(h);
		orientationYawFilterY = new FIR(h);
		
		
		calculatedOrientation = new Orientation();
		
		Timer timer = new Timer();
		TimerTask checkOrientation = new filterSensor();
		timer.scheduleAtFixedRate(checkOrientation,0,20);
		
		magBin = new float[3];
		accelBin = new float[3];
	}
	
	class filterSensor extends TimerTask	{
		@Override
		public void run()	{
			updateOrientation();
		}
	}
	
	public Orientation getOrientation()	{
		return calculatedOrientation;
	}
	
	
	public void updateOrientation()	{
		
		
		float[] m_rotationMatrix = new float[9];
		float[] m_orientation = new float[3];
		
		
		if (SensorManager.getRotationMatrix(m_rotationMatrix, null,
                accelBin, magBin)) {
				SensorManager.getOrientation(m_rotationMatrix, m_orientation);
		}
		
		
		calculatedOrientation.pitch = orientationPitchFilter.getNextOutput(m_orientation[1]);
		calculatedOrientation.setQuaternionYaw(
				orientationYawFilterX.getNextOutput((float)Math.cos(m_orientation[0])),
				orientationYawFilterY.getNextOutput((float)Math.sin(m_orientation[0]))
				);
		
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)	{
			accelBin[0] = event.values[0];	
			accelBin[1] = event.values[1];
			accelBin[2] = event.values[2];
		} else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)	{
			magBin[0] = event.values[0];
			magBin[1] = event.values[1];
			magBin[2] = event.values[2];
		}
	}

}
