package palecek.hillaire;

import org.joml.Vector3f;
import palecek.core.ComputeShaderManager;
import palecek.core.ShaderManager;


public class HillarieModel {
    private float bottomRadius;
    private float topRadius;
    private float rayleighDensityExpScale;
    private Vector3f rayleighScattering;
    private float mieDensityExpScale;
    private Vector3f mieScattering;
    private Vector3f mieExtinction;
    private Vector3f mieAbsorption;
    private float miePhaseG;
    private float absorptionDensity0LayerWidth;
    private float absorptionDensity0ConstantTerm;
    private float absorptionDensity0LinearTerm;
    private float absorptionDensity1ConstantTerm;
    private float absorptionDensity1LinearTerm;
    private Vector3f absorptionExtinction;
    private Vector3f groundAlbedo;
    private float exposure;
    private Vector3f sunIlluminance;

    private float sunZenith;
    private float sunAzimuth;
    private Vector3f sunDirection;
    private boolean isSmall = false;

    public HillarieModel() {
        this.bottomRadius = 6360.0f;
        this.topRadius = 6520.0f;
        this.rayleighDensityExpScale = -1.0f / 8.0f;
        this.rayleighScattering = new Vector3f(0.005802f, 0.013558f, 0.033100f);
        this.mieDensityExpScale = -1.0f / 1.2f;
        this.mieScattering = new Vector3f(0.003996f, 0.003996f, 0.003996f).mul(4.0f);
        this.mieAbsorption = new Vector3f(0.004440f).mul(0.5f);
        this.mieExtinction = new Vector3f(mieScattering).add(mieAbsorption);
        this.miePhaseG = 0.8f;
        this.absorptionDensity0LayerWidth = 25.0f;
        this.absorptionDensity0ConstantTerm = -2.0f / 3.0f;
        this.absorptionDensity0LinearTerm = 1.0f / 15.0f;
        this.absorptionDensity1ConstantTerm = 8.0f / 3.0f;
        this.absorptionDensity1LinearTerm = -1.0f / 15.0f;
        this.absorptionExtinction = new Vector3f(0.000650f, 0.001881f, 0.000085f);
        this.groundAlbedo = new Vector3f(0.1f, 0.1f, 0.1f);
        this.sunIlluminance = new Vector3f(10.0f, 10.0f, 10.0f);

        this.exposure = 0.2f;
        this.sunZenith = (float)Math.toRadians(90.0f);
        this.sunAzimuth = 0.0f;
        this.sunDirection = calculateSunPosition();
    }
    public static HillarieModel getSmallPlanet() {
        HillarieModel model = new HillarieModel();
        model.isSmall = true;
        return model;
    }

    public Vector3f calculateSunPosition() {
        return calculateSunPosition(sunAzimuth, sunZenith);
    }

    public static Vector3f calculateSunPosition(float azimuth, float zenith) {
        float x = (float)(Math.sin(zenith) * Math.sin(azimuth));
        float y = (float)(Math.cos(zenith));
        float z = (float)(Math.sin(zenith) * Math.cos(azimuth));
        return new Vector3f(x, y, z).normalize();
    }

    public void rotateSun(float incZenith, float incAzimuth) {
        sunZenith += incZenith;
        sunAzimuth += incAzimuth;
        sunDirection = calculateSunPosition();
    }

    public void createUniforms(ComputeShaderManager manager, String uniformName) {
        manager.createUniform(uniformName + ".BottomRadius");
        manager.createUniform(uniformName + ".TopRadius");
        manager.createUniform(uniformName + ".RayleighDensityExpScale");
        manager.createUniform(uniformName + ".RayleighScattering");
        manager.createUniform(uniformName + ".MieDensityExpScale");
        manager.createUniform(uniformName + ".MieScattering");
        manager.createUniform(uniformName + ".MieExtinction");
        manager.createUniform(uniformName + ".MieAbsorption");
        manager.createUniform(uniformName + ".MiePhaseG");
        manager.createUniform(uniformName + ".AbsorptionDensity0LayerWidth");
        manager.createUniform(uniformName + ".AbsorptionDensity0ConstantTerm");
        manager.createUniform(uniformName + ".AbsorptionDensity0LinearTerm");
        manager.createUniform(uniformName + ".AbsorptionDensity1ConstantTerm");
        manager.createUniform(uniformName + ".AbsorptionDensity1LinearTerm");
        manager.createUniform(uniformName + ".AbsorptionExtinction");
        manager.createUniform(uniformName + ".GroundAlbedo");
    }

    public void setUniforms(ComputeShaderManager manager, String uniformName) {
        manager.setUniform(uniformName + ".BottomRadius", bottomRadius);
        manager.setUniform(uniformName + ".TopRadius", topRadius);
        manager.setUniform(uniformName + ".RayleighDensityExpScale", rayleighDensityExpScale);
        manager.setUniform(uniformName + ".RayleighScattering", rayleighScattering);
        manager.setUniform(uniformName + ".MieDensityExpScale", mieDensityExpScale);
        manager.setUniform(uniformName + ".MieScattering", mieScattering);
        manager.setUniform(uniformName + ".MieExtinction", mieExtinction);
        manager.setUniform(uniformName + ".MieAbsorption", mieAbsorption);
        manager.setUniform(uniformName + ".MiePhaseG", miePhaseG);
        manager.setUniform(uniformName + ".AbsorptionDensity0LayerWidth", absorptionDensity0LayerWidth);
        manager.setUniform(uniformName + ".AbsorptionDensity0ConstantTerm", absorptionDensity0ConstantTerm);
        manager.setUniform(uniformName + ".AbsorptionDensity0LinearTerm", absorptionDensity0LinearTerm);
        manager.setUniform(uniformName + ".AbsorptionDensity1ConstantTerm", absorptionDensity1ConstantTerm);
        manager.setUniform(uniformName + ".AbsorptionDensity1LinearTerm", absorptionDensity1LinearTerm);
        manager.setUniform(uniformName + ".AbsorptionExtinction", absorptionExtinction);
        manager.setUniform(uniformName + ".GroundAlbedo", groundAlbedo);
    }

    public void createUniforms(ShaderManager manager, String uniformName) throws Exception {
        manager.createUniform(uniformName + ".BottomRadius");
        manager.createUniform(uniformName + ".TopRadius");
        manager.createUniform(uniformName + ".RayleighDensityExpScale");
        manager.createUniform(uniformName + ".RayleighScattering");
        manager.createUniform(uniformName + ".MieDensityExpScale");
        manager.createUniform(uniformName + ".MieScattering");
        manager.createUniform(uniformName + ".MieExtinction");
        manager.createUniform(uniformName + ".MieAbsorption");
        manager.createUniform(uniformName + ".MiePhaseG");
        manager.createUniform(uniformName + ".AbsorptionDensity0LayerWidth");
        manager.createUniform(uniformName + ".AbsorptionDensity0ConstantTerm");
        manager.createUniform(uniformName + ".AbsorptionDensity0LinearTerm");
        manager.createUniform(uniformName + ".AbsorptionDensity1ConstantTerm");
        manager.createUniform(uniformName + ".AbsorptionDensity1LinearTerm");
        manager.createUniform(uniformName + ".AbsorptionExtinction");
        manager.createUniform(uniformName + ".GroundAlbedo");
    }

    public void setUniforms(ShaderManager manager, String uniformName) {
        manager.setUniform(uniformName + ".BottomRadius", bottomRadius);
        manager.setUniform(uniformName + ".TopRadius", topRadius);
        manager.setUniform(uniformName + ".RayleighDensityExpScale", rayleighDensityExpScale);
        manager.setUniform(uniformName + ".RayleighScattering", rayleighScattering);
        manager.setUniform(uniformName + ".MieDensityExpScale", mieDensityExpScale);
        manager.setUniform(uniformName + ".MieScattering", mieScattering);
        manager.setUniform(uniformName + ".MieExtinction", mieExtinction);
        manager.setUniform(uniformName + ".MieAbsorption", mieAbsorption);
        manager.setUniform(uniformName + ".MiePhaseG", miePhaseG);
        manager.setUniform(uniformName + ".AbsorptionDensity0LayerWidth", absorptionDensity0LayerWidth);
        manager.setUniform(uniformName + ".AbsorptionDensity0ConstantTerm", absorptionDensity0ConstantTerm);
        manager.setUniform(uniformName + ".AbsorptionDensity0LinearTerm", absorptionDensity0LinearTerm);
        manager.setUniform(uniformName + ".AbsorptionDensity1ConstantTerm", absorptionDensity1ConstantTerm);
        manager.setUniform(uniformName + ".AbsorptionDensity1LinearTerm", absorptionDensity1LinearTerm);
        manager.setUniform(uniformName + ".AbsorptionExtinction", absorptionExtinction);
        manager.setUniform(uniformName + ".GroundAlbedo", groundAlbedo);
    }

    public Vector3f getSunDirection() {
        return sunDirection;
    }

    public float getSunAzimuth() {
        return sunAzimuth;
    }

    public float getSunZenith() {
        return sunZenith;
    }

    public float getExposure() {
        return exposure;
    }
    public Vector3f getSunIlluminance() {
        return sunIlluminance;
    }
    public boolean isSmall() {
        return isSmall;
    }

    public float getBottomRadius() {
        return bottomRadius;
    }

    public void setBottomRadius(float bottomRadius) {
        this.bottomRadius = bottomRadius;
    }

    public float getTopRadius() {
        return topRadius;
    }

    public void setTopRadius(float topRadius) {
        this.topRadius = topRadius;
    }

    public float getRayleighDensityExpScale() {
        return rayleighDensityExpScale;
    }

    public void setRayleighDensityExpScale(float rayleighDensityExpScale) {
        this.rayleighDensityExpScale = rayleighDensityExpScale;
    }

    public Vector3f getRayleighScattering() {
        return rayleighScattering;
    }

    public void setRayleighScattering(Vector3f rayleighScattering) {
        this.rayleighScattering = rayleighScattering;
    }

    public float getMieDensityExpScale() {
        return mieDensityExpScale;
    }

    public void setMieDensityExpScale(float mieDensityExpScale) {
        this.mieDensityExpScale = mieDensityExpScale;
    }

    public Vector3f getMieScattering() {
        return mieScattering;
    }

    public void setMieScattering(Vector3f mieScattering) {
        this.mieScattering = mieScattering;
    }

    public Vector3f getMieExtinction() {
        return mieExtinction;
    }

    public void setMieExtinction(Vector3f mieExtinction) {
        this.mieExtinction = mieExtinction;
    }

    public Vector3f getMieAbsorption() {
        return mieAbsorption;
    }

    public void setMieAbsorption(Vector3f mieAbsorption) {
        this.mieAbsorption = mieAbsorption;
    }

    public float getMiePhaseG() {
        return miePhaseG;
    }

    public void setMiePhaseG(float miePhaseG) {
        this.miePhaseG = miePhaseG;
    }

    public float getAbsorptionDensity0LayerWidth() {
        return absorptionDensity0LayerWidth;
    }

    public void setAbsorptionDensity0LayerWidth(float absorptionDensity0LayerWidth) {
        this.absorptionDensity0LayerWidth = absorptionDensity0LayerWidth;
    }

    public float getAbsorptionDensity0ConstantTerm() {
        return absorptionDensity0ConstantTerm;
    }

    public void setAbsorptionDensity0ConstantTerm(float absorptionDensity0ConstantTerm) {
        this.absorptionDensity0ConstantTerm = absorptionDensity0ConstantTerm;
    }

    public float getAbsorptionDensity0LinearTerm() {
        return absorptionDensity0LinearTerm;
    }

    public void setAbsorptionDensity0LinearTerm(float absorptionDensity0LinearTerm) {
        this.absorptionDensity0LinearTerm = absorptionDensity0LinearTerm;
    }

    public float getAbsorptionDensity1ConstantTerm() {
        return absorptionDensity1ConstantTerm;
    }

    public void setAbsorptionDensity1ConstantTerm(float absorptionDensity1ConstantTerm) {
        this.absorptionDensity1ConstantTerm = absorptionDensity1ConstantTerm;
    }

    public float getAbsorptionDensity1LinearTerm() {
        return absorptionDensity1LinearTerm;
    }

    public void setAbsorptionDensity1LinearTerm(float absorptionDensity1LinearTerm) {
        this.absorptionDensity1LinearTerm = absorptionDensity1LinearTerm;
    }

    public Vector3f getAbsorptionExtinction() {
        return absorptionExtinction;
    }

    public void setAbsorptionExtinction(Vector3f absorptionExtinction) {
        this.absorptionExtinction = absorptionExtinction;
    }

    public Vector3f getGroundAlbedo() {
        return groundAlbedo;
    }

    public void setGroundAlbedo(Vector3f groundAlbedo) {
        this.groundAlbedo = groundAlbedo;
    }

    public void setExposure(float exposure) {
        this.exposure = exposure;
    }

    public void setSunIlluminance(Vector3f sunIlluminance) {
        this.sunIlluminance = sunIlluminance;
    }

    public void setSunDirection(Vector3f sunDirection) {
        this.sunDirection = sunDirection;
    }
}