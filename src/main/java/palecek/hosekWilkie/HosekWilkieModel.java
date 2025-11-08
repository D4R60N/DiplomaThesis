package palecek.hosekWilkie;

import org.joml.Vector3f;
import palecek.data.HosekWilkieData;
import palecek.utils.SunVector;

import static java.lang.Math.pow;

public class HosekWilkieModel {
    private SunVector sunDir;
    private Vector3f A, B, C, D, E, F, G, H, I;
    private Vector3f sunColor;
    private float sunAngularRadius;
    private float glowRadius;
    private int turbidity;
    private int albedo;
    private Vector3f Z;

    public HosekWilkieModel(SunVector sunDir, Vector3f sunColor, float sunAngularRadius, float glowRadius, int turbidity, int albedo) {
        this.glowRadius = glowRadius;
        this.sunAngularRadius = sunAngularRadius;
        this.sunColor = sunColor;
        this.sunDir = sunDir;
        this.turbidity = turbidity;
        this.albedo = albedo;
        float elevation = sunDir.getElevation();
        float elevationNorm = elevation / (float) (Math.PI / 2);
        lookUpCoefficients(turbidity, albedo, elevationNorm);
        lookUpZenith(turbidity, albedo, elevationNorm);
    }

    private void lookUpCoefficients(int turbidity, int albedo, float x) {

        A = getCoefficient(0, turbidity, albedo, x);
        B = getCoefficient(1, turbidity, albedo, x);
        C = getCoefficient(2, turbidity, albedo, x);
        D = getCoefficient(3, turbidity, albedo, x);
        E = getCoefficient(4, turbidity, albedo, x);
        F = getCoefficient(5, turbidity, albedo, x);
        G = getCoefficient(6, turbidity, albedo, x);
        H = getCoefficient(8, turbidity, albedo, x);
        I = getCoefficient(7, turbidity, albedo, x);
    }

    private Vector3f getCoefficient(int coeffIndex, int turbidity, int albedo, float elevationNorm) {
        float[] ctrlR = getBezierControlCoefficients(HosekWilkieData.datasetRGB1, albedo, turbidity, coeffIndex);
        float[] ctrlG = getBezierControlCoefficients(HosekWilkieData.datasetRGB2, albedo, turbidity, coeffIndex);
        float[] ctrlB = getBezierControlCoefficients(HosekWilkieData.datasetRGB3, albedo, turbidity, coeffIndex);

        float r = evalBezier(ctrlR, elevationNorm);
        float g = evalBezier(ctrlG, elevationNorm);
        float b = evalBezier(ctrlB, elevationNorm);

        return new Vector3f(r, g, b);
    }

    float evalBezier(float[] ctrl, float x) {
        float inv = 1.0f - x;
        return (float) (ctrl[0] * pow(inv, 5)
                + ctrl[1] * 5 * pow(inv, 4) * x
                + ctrl[2] * 10 * pow(inv, 3) * pow(x, 2)
                + ctrl[3] * 10 * pow(inv, 2) * pow(x, 3)
                + ctrl[4] * 5 * inv * pow(x, 4)
                + ctrl[5] * pow(x, 5));
    }

    float[] getBezierControlCoefficients(double[] dataset, int albedo, int turbidity, int coeffIndex) {
        int base = ((albedo * 10 + (turbidity - 1)) * 9 + coeffIndex) * 6;
        float[] result = new float[6];
        for (int i = 0; i < 6; i++)
            result[i] = (float) dataset[base + i];
        return result;
    }

    private void lookUpZenith(int turbidity, int albedo, float x) {
        float Yz = getZenithComponent(HosekWilkieData.datasetRGBRad1, albedo, turbidity, x);
        float xz = getZenithComponent(HosekWilkieData.datasetRGBRad2, albedo, turbidity, x);
        float yz = getZenithComponent(HosekWilkieData.datasetRGBRad3, albedo, turbidity, x);
        Z = new Vector3f(Yz, xz, yz);
    }

    float[] getBezierControlZeniths(double[] dataset, int albedo, int turbidity) {
        int base = (albedo * 10 + (turbidity - 1)) * 6;
        float[] result = new float[6];
        for (int i = 0; i < 6; i++)
            result[i] = (float) dataset[base + i];
        return result;
    }

    private float getZenithComponent(double[] dataset, int albedo, int turbidity, float elevationNorm) {
        float[] ctrl = getBezierControlZeniths(dataset, albedo, turbidity);
        return evalBezier(ctrl, elevationNorm);
    }

    public void rotateSun(float rotateBy) {
        float angleRad = (float) Math.toRadians(rotateBy);
        sunDir.rotate(new Vector3f(1, 0, 0), angleRad);
        float elevation = sunDir.getElevation();
        float elevationNorm = elevation / (float) (Math.PI / 2);
        System.out.println("Elevation: " + elevationNorm);
        lookUpCoefficients(turbidity, albedo, elevationNorm);
        lookUpZenith(turbidity, albedo, elevationNorm);
    }


    public SunVector getSunDir() {
        return sunDir;
    }

    public void setSunDir(SunVector sunDir) {
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

    public Vector3f getF() {
        return F;
    }

    public void setF(Vector3f f) {
        F = f;
    }

    public Vector3f getG() {
        return G;
    }

    public void setG(Vector3f g) {
        G = g;
    }

    public Vector3f getH() {
        return H;
    }

    public void setH(Vector3f h) {
        H = h;
    }

    public Vector3f getI() {
        return I;
    }

    public void setI(Vector3f i) {
        I = i;
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

    public int getTurbidity() {
        return turbidity;
    }

    public void setTurbidity(int turbidity) {
        this.turbidity = turbidity;
    }

    public int getAlbedo() {
        return albedo;
    }

    public void setAlbedo(int albedo) {
        this.albedo = albedo;
    }

}
