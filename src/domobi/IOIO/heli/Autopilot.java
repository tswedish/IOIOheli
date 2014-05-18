package domobi.IOIO.heli;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.SystemClock;

//Need Gyro for rotation, now just manually select rotation power...
public class Autopilot {
	private Helicopter heliModel;
	private Orientation desiredOrientation;
	public float pitchError;
	public float yawError;
	public float rotPower;
	public float pitPower;
	private long lastTime;
	private long dt;
	private FIR diffFilterPitch;
	private FIR diffFilterYaw;

	private float velMax = 20f;
	
	public float kPt = 0.3f, kIt = 0.001f, kDt = 2.5f;
	// public float kPr = 0.3f, kIr = 0.0f, kDr = 0.0f;
	public float kPr = 0.0001f, kIr = 0.0f, kDr = 0.0f;

	public float uPt = 0.0f, uIt = 0.0f, uDt = 0.0f;
	public float uPr = 0.0f, uIr = 0.0f, uDr = 0.0f;

	public Autopilot() {
		// Savitzky-Golay Derivative
		float[] sg = { 3, 2, 1, 0, -1, -2, -3 };
		heliModel = new Helicopter();
		desiredOrientation = new Orientation();
		diffFilterPitch = new FIR(sg);
		diffFilterYaw = new FIR(sg);
		lastTime = 0;
		rotPower = 0.0f;
	}

	public void setOrientation(float pitch, float yaw, float mainPwr) {
		desiredOrientation.pitch = pitch;
		desiredOrientation.yawVel = yaw;
		desiredOrientation.mainPwr = mainPwr;
	}

	public void setOrientation(JSONObject setpoint) throws JSONException {
		double pitch = setpoint.getDouble("pitch");
		double yaw = setpoint.getDouble("yaw");
		double mainPwr = setpoint.getDouble("mainPwr");

		desiredOrientation.pitch = (float) pitch;
		desiredOrientation.yawVel = (float) yaw * velMax;
		desiredOrientation.mainPwr = (float) mainPwr;
		
	}

	public Orientation getSetPoint() {
		return desiredOrientation;
	}

	public float getRotPwr()	{
		return rotPower;
	}
	
	public float getYawErr()	{
		return yawError;
	}
	
	public Helicopter getNextState(Odometry odom) {
		// getControl should be called at some rate.
		// the values of odom should reflect this
		// rate if there's a difference (i.e. use an
		// anti-aliasing filter).
		dt = SystemClock.elapsedRealtime() - lastTime;
		if (dt > 1000) {
			// starting up or something went wrong
			dt = 0;
			lastTime = SystemClock.elapsedRealtime();
		} else {
			lastTime = lastTime + dt;
		}

		Orientation error = desiredOrientation.getDifference(odom
				.getOrientation());
		pitchError = error.pitch;
		yawError = error.yawVel;

		heliModel.setMainPwr(desiredOrientation.mainPwr);
		heliModel.setTailPwr(desiredOrientation.pitch);
		
		heliModel.setRotationPwr(desiredOrientation.getYaw());
		
		// Should be reversed?
		uPr = yawError;

		uIr = (uIr + yawError * dt);

		uDr = (diffFilterYaw.getNextOutput(yawError) / dt);

		rotPower = rotPower + (kPr * uPr);// + kIr * uIr + kDr * uDr);
		if (rotPower > 1)	{ rotPower = 1; }
		if (rotPower < -1)  { rotPower = -1; }
		if (heliModel.getMainPwr() > 0.2f) {
			heliModel.setRotationPwr(-rotPower);
		} else	{
			rotPower = 0.0f;
		}

		return heliModel;
	}

}
