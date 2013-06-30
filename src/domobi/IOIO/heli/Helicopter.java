package domobi.IOIO.heli;

import org.json.JSONException;
import org.json.JSONObject;

class Helicopter {

	private float rotationTrim;
	private float tailTrim;
	private float mainPwr;
	private float tailPwr;
	private float rotationPwr;
	private Boolean isUserConnected;

	public Helicopter() {
		rotationTrim = 0.0f;
		rotationPwr = 0.0f;
		tailTrim = 0.0f;
		tailPwr = 0.0f;
		mainPwr = 0.0f;
		isUserConnected = false;

	}

	public void setHelicopterState(JSONObject heliState) throws JSONException {
		double MainPwr = heliState.getDouble("mainPower");
		double RotPwr = heliState.getDouble("rotationPower");
		double RotTrim = heliState.getDouble("rotTrim");
		double TailPwr = heliState.getDouble("tailPower");
		double TailTrim = heliState.getDouble("tailTrim");

		setMainPwr((float) MainPwr);
		setRotationPwr((float) RotPwr);
		setRotationTrim((float) RotTrim);
		setTailPwr((float) TailPwr);
		setTailTrim((float) TailTrim);

		// Helicopter Settings Updated
		isUserConnected = true;

	}

	public float getRotationTrim() {
		return rotationTrim;
	}

	public float getTailTrim() {
		return tailTrim;
	}

	public float getMainPwr() {
		return mainPwr;
	}

	public float getTailPwr() {
		return tailPwr;
	}

	public float getRotationPwr() {
		return rotationPwr;
	}

	public Boolean connectionBeat() {
		return isUserConnected;
	}

	public void setRotationTrim(float rot) {
		rotationTrim = rot;
	}

	public void setTailTrim(float tail) {
		tailTrim = tail;
	}

	public void setMainPwr(float mainPwr) {
		this.mainPwr = mainPwr;

		if (this.mainPwr > 1) {
			this.mainPwr = 1;
		}
		if (this.mainPwr < 0) {
			this.mainPwr = 0;
		}
	}

	public void setTailPwr(float tailPwr) {
		this.tailPwr = tailPwr;

		if (this.tailPwr > 1) {
			this.tailPwr = 1;
		}
		if (this.tailPwr < -1) {
			this.tailPwr = -1;
		}
	}

	public void setRotationPwr(float rotPwr) {
		this.rotationPwr = rotPwr;

		if (this.rotationPwr > 1) {
			this.rotationPwr = 1;
		}
		if (this.rotationPwr < -1) {
			this.rotationPwr = -1;
		}
	}

	public void setConnection(Boolean connection) {
		this.isUserConnected = connection;
	}

}