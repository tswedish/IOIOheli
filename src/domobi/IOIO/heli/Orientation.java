package domobi.IOIO.heli;

public class Orientation {

  /*
   * Eventually want to go to a quaternion representation.
   * 
   * For now, using a 2D vector for "yaw" and angle for pitch... with absolute
   * value for mainPwr. Eventually extend Orientation + localization for 4D
   * quaternion, 3D (lat, long, alt) global location.
   * 
   * Pitch is the angle from level.
   * 
   * Yaw is location along the unit circle (x,y), kept as an angle, with methods
   * for getting deltas...(to avoid gimbal lock).
   * 
   * Roll will be an issue, for now, ignoring.
   */

  public float    pitch;
  // radians
  private float   yaw;
  private float[] qYaw;
  // Replace with height for barometer
  public float    mainPwr;

  public Orientation() {
    this.pitch = 0.0f;
    this.mainPwr = 0.0f;
    this.yaw = 0.0f;
    this.qYaw = new float[2];
    setYaw(this.yaw);
  }

  public Orientation(float pitch, float yaw) {
    this.pitch = pitch;
    this.setYaw(yaw);
    this.mainPwr = 0.0f;
  }

  public void setQuaternionYaw(float[] qYaw) {
    this.yaw = (float) Math.atan2((double) qYaw[1], (double) qYaw[0]);
    this.qYaw[0] = (float) Math.cos((double) yaw);
    this.qYaw[1] = (float) Math.sin((double) yaw);
  }

  public void setQuaternionYaw(float x, float y) {
    this.yaw = (float) Math.atan2((double) x, (double) y);
    this.qYaw[0] = (float) Math.cos((double) yaw);
    this.qYaw[1] = (float) Math.sin((double) yaw);
  }

  public void setYaw(float yaw) {
    this.qYaw[0] = (float) Math.cos((double) yaw);
    this.qYaw[1] = (float) Math.sin((double) yaw);
    setQuaternionYaw(this.qYaw);
  }

  public float[] getQuaternionYaw() {
    return this.qYaw;
  }

  public float getYawX() {
    return this.qYaw[0];
  }

  public float getYawY() {
    return this.qYaw[1];
  }

  public float getYaw() {
    return this.yaw;
  }

  public Orientation getDifference(Orientation orientation) {
    Orientation diff = new Orientation();
    diff.setYaw(perpDotProduct(orientation.getQuaternionYaw(), this.qYaw));
    diff.pitch = this.pitch - orientation.pitch;
    // mainPwr no error
    return diff;
  }

  static float perpDotProduct(float[] a, float[] b) {
    return (float) Math.atan2(a[0] * b[1] - a[1] * b[0], a[0] * b[0] + a[1]
        * b[1]);
  }
}
