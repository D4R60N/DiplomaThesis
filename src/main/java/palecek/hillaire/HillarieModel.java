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
    private Vector3f whitePoint;

    private float sunZenith;
    private float sunAzimuth;

    public HillarieModel() {
        this.bottomRadius = 6360.0f;
        this.topRadius = 6420.0f;
        this.rayleighDensityExpScale = -1.0f / 8.0f;
        this.rayleighScattering = new Vector3f(0.005802f, 0.013558f, 0.033100f);
        this.mieDensityExpScale = -1.0f / 1.2f;
        this.mieScattering = new Vector3f(0.003996f, 0.003996f, 0.003996f);
        this.mieExtinction = new Vector3f(0.004440f, 0.004440f, 0.004440f);
        this.mieAbsorption = new Vector3f(0.000444f, 0.000444f, 0.000444f); // Example value
        this.miePhaseG = 0.8f;
        this.absorptionDensity0LayerWidth = 25.0f;
        this.absorptionDensity0ConstantTerm = -2.0f / 3.0f;
        this.absorptionDensity0LinearTerm = 1.0f / 15.0f;
        this.absorptionDensity1ConstantTerm = 8.0f / 3.0f;
        this.absorptionDensity1LinearTerm = -1.0f / 15.0f;
        this.absorptionExtinction = new Vector3f(0.000650f, 0.001881f, 0.000085f);
        this.groundAlbedo = new Vector3f(0.1f, 0.1f, 0.1f);

        this.exposure = 10.0f;
        this.whitePoint = new Vector3f(1.0f, 1.0f, 1.0f);
        this.sunZenith = (float)Math.PI / 2.0f;
        this.sunAzimuth = 0.0f;
    }

    public Vector3f calculateSunPosition(float azimuth, float zenith) {
        float x = (float)(Math.sin(zenith) * Math.sin(azimuth));
        float y = (float)(Math.cos(zenith));
        float z = (float)(Math.sin(zenith) * Math.cos(azimuth));
        return new Vector3f(x, y, z).normalize();
    }

    public void rotateSun(float incZenith, float incAzimuth) {
        sunZenith += incZenith;
        sunAzimuth += incAzimuth;
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
}