package palecek.preetham;

import org.joml.Vector3f;

public class PreethamModel {
    private Vector3f sunDir;
    private Vector3f A, B, C, D, E;
    private Vector3f Z;
    private Vector3f sunColor;
    private float sunAngularRadius;
    private float glowRadius;
    private float turbidity;
    private float sunAngle;

    public PreethamModel(Vector3f sunDir, Vector3f A, Vector3f B, Vector3f C, Vector3f D, Vector3f E, Vector3f Z, Vector3f sunColor, float sunAngularRadius, float glowRadius, float turbidity, float sunAngle) {
        this.glowRadius = glowRadius;
        this.sunAngularRadius = sunAngularRadius;
        this.sunColor = sunColor;
        this.A = A;
        this.B = B;
        this.C = C;
        this.D = D;
        this.E = E;
        this.Z = Z;
        this.sunDir = sunDir;
        this.turbidity = turbidity;
        this.sunAngle = sunAngle;
    }

    public void rotateSun(float rotateBy) {
        float angleRad = (float) Math.toRadians(rotateBy);
        sunDir.rotateZ(angleRad);

        //axValue * Math.abs(1 - 2 * ((t % (2 * maxValue)) / (float)(2 * maxValue)))
        sunAngle = (float) Math.toDegrees(Math.acos(sunDir.y))+90;
        sunAngle = 90 * Math.abs(1-2*((sunAngle % 180)/180.0f));
        Z = computeZenith(turbidity, (float) Math.toRadians(sunAngle));
    }


    public Vector3f getSunDir() {
        return sunDir;
    }

    public void setSunDir(Vector3f sunDir) {
        this.sunDir = sunDir;
    }

    public Vector3f getA() {
        return A;
    }

    public void setA(Vector3f a) {
        A = a;
    }

    public Vector3f getB() {
        return B;
    }

    public void setB(Vector3f b) {
        B = b;
    }

    public Vector3f getC() {
        return C;
    }

    public void setC(Vector3f c) {
        C = c;
    }

    public Vector3f getD() {
        return D;
    }

    public void setD(Vector3f d) {
        D = d;
    }

    public Vector3f getE() {
        return E;
    }

    public void setE(Vector3f e) {
        E = e;
    }

    public Vector3f getZ() {
        return Z;
    }

    public void setZ(Vector3f z) {
        Z = z;
    }

    public Vector3f getSunColor() {
        return sunColor;
    }

    public void setSunColor(Vector3f sunColor) {
        this.sunColor = sunColor;
    }

    public float getSunAngularRadius() {
        return sunAngularRadius;
    }

    public void setSunAngularRadius(float sunAngularRadius) {
        this.sunAngularRadius = sunAngularRadius;
    }

    public float getGlowRadius() {
        return glowRadius;
    }

    public void setGlowRadius(float glowRadius) {
        this.glowRadius = glowRadius;
    }

    public float getTurbidity() {
        return turbidity;
    }

    public void setTurbidity(float turbidity) {
        this.turbidity = turbidity;
    }
    public float getSunAngle() {
        return sunAngle;
    }
    public void setSunAngle(float sunAngle) {
        this.sunAngle = sunAngle;
    }

    public static Vector3f computeZenith(float turbidity, float thetaSun) {
        double chi = (4.0/9.0 - turbidity/120.0) * (Math.PI - 2.0 * thetaSun);

        double YzRaw = (4.0453 * turbidity - 4.9710) * Math.tan(chi) - 0.2155 * turbidity + 2.4192;

        double xz =
                (0.00165 * Math.pow(thetaSun, 3) - 0.00375 * Math.pow(thetaSun, 2) + 0.00209 * thetaSun) * turbidity * turbidity +
                        (-0.02903 * Math.pow(thetaSun, 3) + 0.06377 * Math.pow(thetaSun, 2) - 0.03202 * thetaSun + 0.00394) * turbidity +
                        (0.11693 * Math.pow(thetaSun, 3) - 0.21196 * Math.pow(thetaSun, 2) + 0.06052 * thetaSun + 0.25885);


        double yz =
                (0.00275 * Math.pow(thetaSun, 3) - 0.00610 * Math.pow(thetaSun, 2) + 0.00317 * thetaSun) * turbidity * turbidity +
                        (-0.04214 * Math.pow(thetaSun, 3) + 0.08970 * Math.pow(thetaSun, 2) - 0.04153 * thetaSun + 0.00516) * turbidity +
                        (0.15346 * Math.pow(thetaSun, 3) - 0.26756 * Math.pow(thetaSun, 2) + 0.06669 * thetaSun + 0.26688);

        double chiNoon = (4.0/9.0 - turbidity/120.0) * (Math.PI - 0.0); // thetaSun=0 at noon
        double YzNoon = (4.0453 * turbidity - 4.9710) * Math.tan(chiNoon) - 0.2155 * turbidity + 2.4192;

        double YzNormalized = YzRaw / YzNoon;

        return new Vector3f((float)YzNormalized, (float)xz, (float)yz);
    }

}
