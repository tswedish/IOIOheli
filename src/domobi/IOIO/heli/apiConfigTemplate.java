package domobi.IOIO.heli;

import com.pubnub.api.Pubnub;

public class apiConfigTemplate {
	public Pubnub pubnub;

	public apiConfigTemplate() {
		pubnub = new Pubnub("demo", "demo", "", "", false);
	}

}
