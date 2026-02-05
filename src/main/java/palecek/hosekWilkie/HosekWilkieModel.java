package palecek.hosekWilkie;

import org.joml.Vector3f;
import palecek.data.HosekWilkieData;
import palecek.data.HosekWilkieDataRGB;
import palecek.utils.SunVector;

public class HosekWilkieModel {
    private SunVector sunDir;
    private Vector3f A, B, C, D, E, F, G, H, I;
    private Vector3f sunColor;
    private float sunAngularRadius;
    private float glowRadius;
    private float turbidity;
    private int albedo;
    private Vector3f Z;
    private HosekWilkieData data = new HosekWilkieDataRGB();
    private float exposure;

    public HosekWilkieModel(SunVector sunDir, Vector3f sunColor, float sunAngularRadius, float glowRadius, float turbidity, int albedo, float exposure) {
        this.glowRadius = glowRadius;
        this.sunAngularRadius = sunAngularRadius;
        this.sunColor = sunColor;
        this.sunDir = sunDir;
        this.turbidity = turbidity;
        this.albedo = albedo;
        this.exposure = exposure;
        float elevation = sunDir.getElevation();
        lookUpCoefficients(turbidity, albedo, elevation);
    }

    private void lookUpCoefficients(float turbidity, int albedo, float elevation) {
        A = getCoefficient(0, turbidity, albedo, elevation);
        B = getCoefficient(1, turbidity, albedo, elevation);
        C = getCoefficient(2, turbidity, albedo, elevation);
        D = getCoefficient(3, turbidity, albedo, elevation);
        E = getCoefficient(4, turbidity, albedo, elevation);
        F = getCoefficient(5, turbidity, albedo, elevation);
        G = getCoefficient(6, turbidity, albedo, elevation);
        H = getCoefficient(8, turbidity, albedo, elevation);
        I = getCoefficient(7, turbidity, albedo, elevation);
        Z = getZenith(turbidity, albedo, elevation);
    }

    private Vector3f getCoefficient(int coeffIndex, float turbidity, int albedo, float elevation) {
        float r = (float) evaluate(data.getDataset1(), coeffIndex, 9, turbidity, albedo, elevation);
        float g = (float) evaluate(data.getDataset2(), coeffIndex, 9, turbidity, albedo, elevation);
        float b = (float) evaluate(data.getDataset3(), coeffIndex, 9, turbidity, albedo, elevation);

        return new Vector3f(r, g, b);
    }

    private Vector3f getZenith(float turbidity, int albedo, float elevation) {
        float r = (float) evaluate(data.getDatasetRad1(), 0, 1, turbidity, albedo, elevation);
        float g = (float) evaluate(data.getDatasetRad2(), 0, 1, turbidity, albedo, elevation);
        float b = (float) evaluate(data.getDatasetRad3(), 0, 1, turbidity, albedo, elevation);

        return new Vector3f(r, g, b);
    }

    public void recalculate() {
        float elevation = sunDir.getElevation();
        lookUpCoefficients(turbidity, albedo, elevation);
    }

    private double evaluateSpline(double[] dataset, int baseIndex, int stride, double value) {
        return  Math.pow(1 - value, 5) * dataset[baseIndex + 0] +
                5 * Math.pow(1 - value, 4) * Math.pow(value, 1)
                        * dataset[baseIndex + stride] +
                10 * Math.pow(1 - value, 3) * Math.pow(value, 2)
                        * dataset[baseIndex + 2 * stride] +
                10 * Math.pow(1 - value, 2) * Math.pow(value, 3)
                        * dataset[baseIndex + 3 * stride] +
                5 * Math.pow(1 - value, 1) * Math.pow(value, 4)
                        * dataset[baseIndex + 4 * stride] +
                Math.pow(value, 5) * dataset[baseIndex + 5 * stride];
    }

    private double evaluate(double[] datasetRGB, int coeffIndex, int stride, float turbidity, float albedo, float sunTheta) {
        double elevationK = 1-Math.pow(Math.max(0f, 1f - sunTheta / (Math.PI / 2f)), 1.0/3.0);
        int turbidity0 = Math.min(Math.max((int)turbidity, 1), 10);
        int turbidity1 = Math.min(turbidity0 + 1, 10);
        float turbidityK = Math.min(Math.max(turbidity - turbidity0, 0f), 1f);

        int baseIndex0 = coeffIndex + stride * 6 * (turbidity0 - 1);
        int baseIndex1 = coeffIndex + stride * 6 * (turbidity1 - 1);

        double a0t0 = evaluateSpline(datasetRGB, baseIndex0, stride, elevationK);
        double a1t0 = evaluateSpline(datasetRGB, baseIndex0 + stride * 6 * 10, stride, elevationK);
        double a0t1 = evaluateSpline(datasetRGB, baseIndex1, stride, elevationK);
        double a1t1 = evaluateSpline(datasetRGB, baseIndex1 + stride * 6 * 10, stride, elevationK);

        return a0t0 * (1 - albedo) * (1 - turbidityK) +
                a1t0 * albedo * (1 - turbidityK) +
                a0t1 * (1 - albedo) * turbidityK +
                a1t1 * albedo * turbidityK;
    }


    public void rotateSun(float rotateBy) {
        float angleRad = (float) Math.toRadians(rotateBy);
        sunDir.rotate(new Vector3f(1, 0, 0), angleRad);
        float elevation = sunDir.getElevation();
        lookUpCoefficients(turbidity, albedo, elevation);
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

    public float getTurbidity() {
        return turbidity;
    }


    public void setTurbidity(float turbidity) {
        this.turbidity = turbidity;
    }

    public int getAlbedo() {
        return albedo;
    }

    public void setAlbedo(int albedo) {
        this.albedo = albedo;
    }

    public float getExposure() {
        return exposure;
    }

    public void setExposure(float exposure) {
        this.exposure = exposure;
    }
}
