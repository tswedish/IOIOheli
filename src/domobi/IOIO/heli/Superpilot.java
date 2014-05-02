package domobi.IOIO.heli;

import org.json.JSONException;
import org.json.JSONObject;

public class Superpilot {
	
	private Helicopter heliModel;
	
	public Superpilot() {
		heliModel = new Helicopter();
	}

	public Helicopter getHeli(JSONObject setpoint) throws JSONException {
		double pitch = setpoint.getDouble("pitch");
		double yaw = setpoint.getDouble("yaw");
		double mainPwr = 0.5 * setpoint.getDouble("mainPwr");

		heliModel.setTailPwr((float) pitch);
		heliModel.setRotationPwr((float) yaw);
		heliModel.setMainPwr((float) mainPwr);
		
		return heliModel;
	}
	
}
