package palecek.bruneton;

import org.joml.Vector3f;
import palecek.core.ComputeShaderManager;

public class BrunetonModel {
    Vector3f solarIrradiance;
    float sunAngularRadius;
    float bottomRadius;
    float topRadius;
    DensityProfile[] rayleighDensity;
    Vector3f rayleighScattering;
    DensityProfile[] mieDensity;
    Vector3f mieScattering;
    Vector3f mieExtinction;
    float miePhaseFunctionG;
    DensityProfile[] absorptionDensity;
    Vector3f absorptionExtinction;
    Vector3f groundAlbedo;
    float muSMin;

    public BrunetonModel() {
        this.solarIrradiance = new Vector3f(1.474f, 1.8504f, 2.1229f);
        this.sunAngularRadius = 0.004675f;
        this.bottomRadius = 6360.0f;
        this.topRadius = 6420.0f;
        this.groundAlbedo = new Vector3f(0.1f, 0.1f, 0.1f);
        this.muSMin = -0.207912f;

        this.rayleighScattering = new Vector3f(0.005802f, 0.013558f, 0.033100f);
        this.rayleighDensity = new DensityProfile[] {
                new DensityProfile(0.0f, 0.0f, 0.0f, 0.0f, 0.0f),
                new DensityProfile(0.0f, 1.0f, -1.0f / 8.0f, 0.0f, 0.0f)
        };

        this.mieScattering = new Vector3f(0.003996f, 0.003996f, 0.003996f);
        this.mieExtinction = new Vector3f(0.004440f, 0.004440f, 0.004440f);
        this.miePhaseFunctionG = 0.8f;
        this.mieDensity = new DensityProfile[] {
                new DensityProfile(0.0f, 0.0f, 0.0f, 0.0f, 0.0f),
                new DensityProfile(0.0f, 1.0f, -1.0f / 1.2f, 0.0f, 0.0f)
        };

        this.absorptionExtinction = new Vector3f(0.000650f, 0.001881f, 0.000085f);
        this.absorptionDensity = new DensityProfile[] {
                new DensityProfile(25.0f, 0.0f, 0.0f, 1.0f / 15.0f, -2.0f / 3.0f),
                new DensityProfile(0.0f, 0.0f, 0.0f, -1.0f / 15.0f, 8.0f / 3.0f)
        };
    }

    public BrunetonModel(Vector3f solarIrradiance, float sunAngularRadius, float bottomRadius, float topRadius, DensityProfile[] rayleighDensity, Vector3f rayleighScattering, DensityProfile[] mieDensity, Vector3f mieScattering, Vector3f mieExtinction, float miePhaseFunctionG, DensityProfile[] absorptionDensity, Vector3f absorptionExtinction, Vector3f groundAlbedo, float muSMin) {
        this.solarIrradiance = solarIrradiance;
        this.sunAngularRadius = sunAngularRadius;
        this.bottomRadius = bottomRadius;
        this.topRadius = topRadius;
        this.rayleighDensity = rayleighDensity;
        this.rayleighScattering = rayleighScattering;
        this.mieDensity = mieDensity;
        this.mieScattering = mieScattering;
        this.mieExtinction = mieExtinction;
        this.miePhaseFunctionG = miePhaseFunctionG;
        this.absorptionDensity = absorptionDensity;
        this.absorptionExtinction = absorptionExtinction;
        this.groundAlbedo = groundAlbedo;
        this.muSMin = muSMin;
    }

    public Vector3f getSolarIrradiance() {
        return solarIrradiance;
    }

    public void setSolarIrradiance(Vector3f solarIrradiance) {
        this.solarIrradiance = solarIrradiance;
    }

    public float getSunAngularRadius() {
        return sunAngularRadius;
    }

    public void setSunAngularRadius(float sunAngularRadius) {
        this.sunAngularRadius = sunAngularRadius;
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

    public DensityProfile[] getRayleighDensity() {
        return rayleighDensity;
    }

    public void setRayleighDensity(DensityProfile[] rayleighDensity) {
        this.rayleighDensity = rayleighDensity;
    }

    public Vector3f getRayleighScattering() {
        return rayleighScattering;
    }

    public void setRayleighScattering(Vector3f rayleighScattering) {
        this.rayleighScattering = rayleighScattering;
    }

    public DensityProfile[] getMieDensity() {
        return mieDensity;
    }

    public void setMieDensity(DensityProfile[] mieDensity) {
        this.mieDensity = mieDensity;
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

    public float getMiePhaseFunctionG() {
        return miePhaseFunctionG;
    }

    public void setMiePhaseFunctionG(float miePhaseFunctionG) {
        this.miePhaseFunctionG = miePhaseFunctionG;
    }

    public DensityProfile[] getAbsorptionDensity() {
        return absorptionDensity;
    }

    public void setAbsorptionDensity(DensityProfile[] absorptionDensity) {
        this.absorptionDensity = absorptionDensity;
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

    public float getMuSMin() {
        return muSMin;
    }

    public void setMuSMin(float muSMin) {
        this.muSMin = muSMin;
    }

    public static class DensityProfile {
        public float width;
        public float expTerm;
        public float expScale;
        public float linearTerm;
        public float constantTerm;

        public DensityProfile(float width, float expTerm, float expScale, float linearTerm, float constantTerm) {
            this.width = width;
            this.expTerm = expTerm;
            this.expScale = expScale;
            this.linearTerm = linearTerm;
            this.constantTerm = constantTerm;
        }
    }

    public void createUniforms(ComputeShaderManager manager, String uniformName) {
        manager.createUniform(uniformName + ".solar_irradiance");
        manager.createUniform(uniformName + ".sun_angular_radius");
        manager.createUniform(uniformName + ".bottom_radius");
        manager.createUniform(uniformName + ".top_radius");

        createDensityUniforms(manager, uniformName + ".rayleigh_density", rayleighDensity.length);
        manager.createUniform(uniformName + ".rayleigh_scattering");

        createDensityUniforms(manager, uniformName + ".mie_density", mieDensity.length);
        manager.createUniform(uniformName + ".mie_scattering");
        manager.createUniform(uniformName + ".mie_extinction");
        manager.createUniform(uniformName + ".mie_phase_function_g");

        createDensityUniforms(manager, uniformName + ".absorption_density", absorptionDensity.length);
        manager.createUniform(uniformName + ".absorption_extinction");
        manager.createUniform(uniformName + ".ground_albedo");
        manager.createUniform(uniformName + ".mu_s_min");
    }

    // Helper to avoid repetitive code
    private void createDensityUniforms(ComputeShaderManager manager, String baseName, int length) {
        for (int i = 0; i < length; i++) {
            manager.createUniform(baseName + ".layers[" + i + "].width");
            manager.createUniform(baseName + ".layers[" + i + "].exp_term");
            manager.createUniform(baseName + ".layers[" + i + "].exp_scale");
            manager.createUniform(baseName + ".layers[" + i + "].linear_term");
            manager.createUniform(baseName + ".layers[" + i + "].constant_term");
        }
    }

    public void setUniforms(ComputeShaderManager manager, String uniformName) {
        manager.setUniform(uniformName + ".solar_irradiance", solarIrradiance);
        manager.setUniform(uniformName + ".sun_angular_radius", sunAngularRadius);
        manager.setUniform(uniformName + ".bottom_radius", bottomRadius);
        manager.setUniform(uniformName + ".top_radius", topRadius);

        setDensityUniforms(manager, uniformName + ".rayleigh_density", rayleighDensity);
        manager.setUniform(uniformName + ".rayleigh_scattering", rayleighScattering);

        setDensityUniforms(manager, uniformName + ".mie_density", mieDensity);
        manager.setUniform(uniformName + ".mie_scattering", mieScattering);
        manager.setUniform(uniformName + ".mie_extinction", mieExtinction);
        manager.setUniform(uniformName + ".mie_phase_function_g", miePhaseFunctionG);

        setDensityUniforms(manager, uniformName + ".absorption_density", absorptionDensity);
        manager.setUniform(uniformName + ".absorption_extinction", absorptionExtinction);
        manager.setUniform(uniformName + ".ground_albedo", groundAlbedo);
        manager.setUniform(uniformName + ".mu_s_min", muSMin);
    }

    private void setDensityUniforms(ComputeShaderManager manager, String baseName, DensityProfile[] profiles) {
        for (int i = 0; i < profiles.length; i++) {
            manager.setUniform(baseName + ".layers[" + i + "].width", profiles[i].width);
            manager.setUniform(baseName + ".layers[" + i + "].exp_term", profiles[i].expTerm);
            manager.setUniform(baseName + ".layers[" + i + "].exp_scale", profiles[i].expScale);
            manager.setUniform(baseName + ".layers[" + i + "].linear_term", profiles[i].linearTerm);
            manager.setUniform(baseName + ".layers[" + i + "].constant_term", profiles[i].constantTerm);
        }
    }
}
