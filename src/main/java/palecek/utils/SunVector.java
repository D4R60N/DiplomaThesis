package palecek.utils;

import org.joml.Vector3f;

public class SunVector {

    private Vector3f direction;

    /**
     * Constructs a SunVector from sun position angles in radians.
     *
     * @param azimuth   The horizontal angle of the sun (radians, 0 = north, increases eastward)
     * @param elevation The vertical angle of the sun above the horizon (radians)
     */
    public SunVector(float azimuth, float elevation) {
        float clampedElevation = Math.max(0, Math.min(elevation, (float) Math.PI / 2));
        float x = (float) (Math.cos(clampedElevation) * Math.sin(azimuth));
        float y = (float) Math.sin(clampedElevation);
        float z = (float) (Math.cos(clampedElevation) * Math.cos(azimuth));
        this.direction = new Vector3f(x, y, z).normalize();
    }

    public void rotate(Vector3f axis, float angle) {
        Vector3f temp = new Vector3f(direction);
        temp.rotateAxis(angle, axis.x, axis.y, axis.z);
        temp.normalize();

        float elevation = (float) Math.asin(temp.y / temp.length());

        if (elevation < 0) {
            elevation = 0;
            float azimuth = (float) Math.atan2(temp.x, temp.z);
            float x = (float) (Math.cos(elevation) * Math.sin(azimuth));
            float y = (float) Math.sin(elevation);
            float z = (float) (Math.cos(elevation) * Math.cos(azimuth));
            this.direction.set(x, y, z).normalize();
        } else {
            this.direction.set(temp);
        }
    }


    public Vector3f getDirection() {
        return new Vector3f(direction);
    }

    public void setDirection(float azimuth, float elevation) {
        float x = (float) (Math.cos(elevation) * Math.sin(azimuth));
        float y = (float) Math.sin(elevation);
        float z = (float) (Math.cos(elevation) * Math.cos(azimuth));
        this.direction.set(x, y, z).normalize();
    }

    public float getElevation() {
        return (float) Math.asin(direction.y / direction.length());
    }

    public float getAzimuth() {
        return (float) Math.atan2(direction.x, direction.z);
    }

    @Override
    public String toString() {
        return "SunVector{" +
                "direction=" + direction +
                '}';
    }
}
