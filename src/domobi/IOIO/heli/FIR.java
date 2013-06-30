package domobi.IOIO.heli;

public class FIR {
	private float[] h;
	private float[] x;
	private float y;

	public FIR(float[] h) {
		this.h = h;
		this.x = new float[h.length];
	}

	public float getNextOutput(float x_new) {
		// update time delays, drop off last one
		for (int i = x.length - 1; i > 0; i--) {
			x[i] = x[i - 1];
		}
		x[0] = x_new;

		// Convolution
		y = 0;
		for (int i = 0; i < h.length; i++) {
			y += x[i] * h[i];
		}

		return y;
	}

}
