package palecek.hillaire;

import org.joml.*;
import palecek.Main;
import palecek.core.Camera;
import palecek.core.utils.*;
import palecek.utils.RawTextureExporter;
import palecek.core.ComputeShaderManager;
import palecek.core.Utils;

import java.lang.Math;

import static org.lwjgl.opengl.ARBShadingLanguageInclude.GL_SHADER_INCLUDE_ARB;
import static org.lwjgl.opengl.ARBShadingLanguageInclude.glNamedStringARB;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL30C.GL_RGBA32F;
import static org.lwjgl.opengl.GL42.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT;
import static org.lwjgl.opengl.GL42C.GL_ALL_BARRIER_BITS;
import static org.lwjgl.opengl.GL42C.glBindImageTexture;
import static org.lwjgl.opengles.GLES31.GL_WRITE_ONLY;


public class HillariePrecompute {
    private final Texture transmittanceMap, multipleScatteringMap, skyViewMap;
    private final Texture3D aerialPerspectiveMap;
    private final Vector2i transmittanceSize,scatteringSize, skyViewSize;
    private final Vector3i aerialPerspectiveSize;
    private final int skyViewGroupsX, skyViewGroupsY, aerialPerspectiveGroupsX, aerialPerspectiveGroupsY, aerialPerspectiveGroupsZ;
    private final ComputeShaderManager transmittanceComputeShaderManager, multiScatteringComputeShaderManager, skyViewComputeShaderManager, aerialPerspectiveComputeShaderManager;

    public HillariePrecompute(Vector2i transmittanceSize, Vector2i scatteringSize, Vector2i skyViewSize, Vector3i aerialPerspectiveSize) {
        transmittanceMap = new Texture(transmittanceSize.x, transmittanceSize.y, GL_RGBA32F, GL_RGBA, GL_FLOAT, null);
        multipleScatteringMap = new Texture(scatteringSize.x, scatteringSize.y, GL_RGBA32F, GL_RGBA, GL_FLOAT, null);
        skyViewMap = new Texture(skyViewSize.x, skyViewSize.y, GL_RGBA32F, GL_RGBA, GL_FLOAT, null);
        aerialPerspectiveMap = new Texture3D(aerialPerspectiveSize.x, aerialPerspectiveSize.y, aerialPerspectiveSize.z, GL_RGBA32F, GL_RGBA, GL_FLOAT, null);
        setupLinearFiltering();
        this.transmittanceSize = transmittanceSize;
        this.scatteringSize = scatteringSize;
        this.skyViewSize = skyViewSize;
        this.aerialPerspectiveSize = aerialPerspectiveSize;
        this.skyViewGroupsX = (int) Math.ceil((double) skyViewSize.x / 8);
        this.skyViewGroupsY = (int) Math.ceil((double) skyViewSize.y / 8);
        this.aerialPerspectiveGroupsX = (int) Math.ceil((double) aerialPerspectiveSize.x / 8);
        this.aerialPerspectiveGroupsY = (int) Math.ceil((double) aerialPerspectiveSize.y / 8);
        this.aerialPerspectiveGroupsZ = aerialPerspectiveSize.z;
        try {
            transmittanceComputeShaderManager = new ComputeShaderManager();
            multiScatteringComputeShaderManager = new ComputeShaderManager();
            skyViewComputeShaderManager = new ComputeShaderManager();
            aerialPerspectiveComputeShaderManager = new ComputeShaderManager();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ITexture[] precompute(HillarieModel model, Camera camera) throws Exception {
        int groupsX = (int) Math.ceil((double) transmittanceSize.x / 8);
        int groupsY = (int) Math.ceil((double) transmittanceSize.y / 8);

        glNamedStringARB(
                GL_SHADER_INCLUDE_ARB,
                "/common.glsl",
                Utils.loadResource("/shaders/precompute/hillaire/common.glsl")
        );

        //----------------------- Transmittance -----------------------//

        transmittanceComputeShaderManager.createComputeShader(Utils.loadResource("/shaders/precompute/hillaire/transmittance/transmittance.glsl"));
        transmittanceComputeShaderManager.link();
        transmittanceComputeShaderManager.bind();

        transmittanceComputeShaderManager.createUniform("uTransmittanceTextureSize");
        transmittanceComputeShaderManager.createUniform("RayMarchMinMaxSPP");
        transmittanceComputeShaderManager.createUniform("sunIlluminance");
        transmittanceComputeShaderManager.createUniform("sunDirection");
        model.createUniforms(transmittanceComputeShaderManager, "uAtmosphere");

        transmittanceComputeShaderManager.setUniform("uTransmittanceTextureSize", transmittanceSize);
        transmittanceComputeShaderManager.setUniform("RayMarchMinMaxSPP", new Vector2f(4.0f, 14.0f));
        transmittanceComputeShaderManager.setUniform("sunIlluminance", model.getSunIlluminance());
        transmittanceComputeShaderManager.setUniform("sunDirection", model.getSunDirection());
        model.setUniforms(transmittanceComputeShaderManager, "uAtmosphere");


        glBindImageTexture(0, transmittanceMap.getId(), 0, false, 0, GL_WRITE_ONLY, GL_RGBA32F);
        transmittanceMap.bind(
                transmittanceComputeShaderManager.getShaderProgram(),
                "transmittanceTexture",
                0
        );
        transmittanceComputeShaderManager.dispatchCompute(groupsX, groupsY, 1);

        transmittanceComputeShaderManager.memoryBarrier(GL_ALL_BARRIER_BITS);
        transmittanceComputeShaderManager.unbind();

        //----------------------- Save Transmittance -----------------------//
        RawTextureExporter.saveTexture2DFloat(
                transmittanceMap,
                transmittanceSize.x,
                transmittanceSize.y,
                "images/hillaire/transmittance.dat"
        );

        TextureExporter.saveHDRTextureToPNG(
                transmittanceMap, transmittanceSize.x, transmittanceSize.y, "images/hillaire/transmittance.png", 1f
        );

        //----------------------- Multiple Scattering -----------------------//

        multiScatteringComputeShaderManager.createComputeShader(Utils.loadResource("/shaders/precompute/hillaire/multiple_scattering/multiple_scattering.glsl"));
        initMultiScattering(multiScatteringComputeShaderManager, model);
        precomputeMultiScattering(multiScatteringComputeShaderManager, model);

        //----------------------- Save Multiple Scattering -----------------------//
        RawTextureExporter.saveTexture2DFloat(
                multipleScatteringMap,
                scatteringSize.x,
                scatteringSize.y,
                "images/hillaire/multiple_scattering.dat"
        );

        TextureExporter.saveHDRTextureToPNG(
                multipleScatteringMap, scatteringSize.x, scatteringSize.y, "images/hillaire/multiple_scattering.png", 1f
        );

        //----------------------- Sky View -----------------------//

        skyViewComputeShaderManager.createComputeShader(Utils.loadResource("/shaders/precompute/hillaire/sky_view/sky_view.glsl"));
        initSkyView(skyViewComputeShaderManager, model);
        precomputeSkyView(skyViewComputeShaderManager, model, camera);

        //----------------------- Save Sky View -----------------------//
        RawTextureExporter.saveTexture2DFloat(
                skyViewMap,
                skyViewSize.x,
                skyViewSize.y,
                "images/hillaire/sky_view.dat"
        );

        TextureExporter.saveHDRTextureToPNG(
                skyViewMap, skyViewSize.x, skyViewSize.y, "images/hillaire/sky_view.png", 1f
        );

        //----------------------- Arial Perspective -----------------------//

        aerialPerspectiveComputeShaderManager.createComputeShader(Utils.loadResource("/shaders/precompute/hillaire/aerial_perspective/aerial_perspective.glsl"));
        initAerialPerspective(aerialPerspectiveComputeShaderManager, model);
        precomputeAerialPerspective(aerialPerspectiveComputeShaderManager, model, camera);

        //----------------------- Save Arial Perspective -----------------------//
        RawTextureExporter.saveTexture3DFloat(
                aerialPerspectiveMap,
                aerialPerspectiveSize.x,
                aerialPerspectiveSize.y,
                aerialPerspectiveSize.z,
                "images/hillaire/aerial_perspective.dat"
        );

        TextureExporter.saveHDRTexture3DToPNG(
                aerialPerspectiveMap, aerialPerspectiveSize.x, aerialPerspectiveSize.y, aerialPerspectiveSize.z, "images/hillaire/aerial_perspective/aerial_perspective", 1f, TextureExporter.SliceDimension.Z
        );

        return new ITexture[]{transmittanceMap, multipleScatteringMap, skyViewMap, aerialPerspectiveMap};
    }
    public void initMultiScattering(ComputeShaderManager computeShaderManager, HillarieModel model) throws Exception {
        computeShaderManager.link();
        computeShaderManager.bind();

        computeShaderManager.createUniform("uScatteringTextureSize");
        computeShaderManager.createUniform("uTransmittanceTextureSize");
        computeShaderManager.createUniform("sunDirection");
        computeShaderManager.createUniform("RayMarchMinMaxSPP");
        computeShaderManager.createUniform("sunIlluminance");
        model.createUniforms(computeShaderManager, "uAtmosphere");

        computeShaderManager.setUniform("uScatteringTextureSize", scatteringSize);
        computeShaderManager.setUniform("uTransmittanceTextureSize", transmittanceSize);
        computeShaderManager.setUniform("RayMarchMinMaxSPP", new Vector2f(4.0f, 14.0f));
        model.setUniforms(computeShaderManager, "uAtmosphere");
    }

    public void precomputeMultiScattering(HillarieModel model) {
        precomputeMultiScattering(multiScatteringComputeShaderManager, model);
    }
    public void precomputeMultiScattering(ComputeShaderManager computeShaderManager, HillarieModel model) {
        computeShaderManager.bind();

        computeShaderManager.setUniform("sunIlluminance", model.getSunIlluminance());
        computeShaderManager.setUniform("sunDirection", model.getSunDirection());

        glBindImageTexture(0, multipleScatteringMap.getId(), 0, false, 0, GL_WRITE_ONLY, GL_RGBA32F);
        transmittanceMap.bind(
                computeShaderManager.getShaderProgram(),
                "transmittanceTexture",
                1
        );

        computeShaderManager.dispatchCompute(scatteringSize.x, scatteringSize.y, 1);

        computeShaderManager.memoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        computeShaderManager.unbind();
    }

    public void initSkyView(ComputeShaderManager computeShaderManager, HillarieModel model) throws Exception {
        computeShaderManager.link();
        computeShaderManager.bind();
        computeShaderManager.createUniform("uSkyViewTextureSize");
        computeShaderManager.createUniform("uScatteringTextureSize");
        computeShaderManager.createUniform("uTransmittanceTextureSize");
        computeShaderManager.createUniform("RayMarchMinMaxSPP");
        computeShaderManager.createUniform("sunIlluminance");
        computeShaderManager.createUniform("sunDirection");
        computeShaderManager.createUniform("camera");
        computeShaderManager.createUniform("isSmall");
        model.createUniforms(computeShaderManager, "uAtmosphere");

        computeShaderManager.setUniform("uSkyViewTextureSize", skyViewSize);
        computeShaderManager.setUniform("uScatteringTextureSize", scatteringSize);
        computeShaderManager.setUniform("uTransmittanceTextureSize", transmittanceSize);
        computeShaderManager.setUniform("RayMarchMinMaxSPP", new Vector2f(4.0f, 14.0f));
        computeShaderManager.setUniform("isSmall", model.isSmall());
        model.setUniforms(computeShaderManager, "uAtmosphere");
    }

    public void precomputeSkyView(HillarieModel model, Camera camera) {
        precomputeSkyView(skyViewComputeShaderManager, model, camera);
    }
    public void precomputeSkyView(ComputeShaderManager computeShaderManager, HillarieModel model, Camera camera) {
        computeShaderManager.bind();

        computeShaderManager.setUniform("sunIlluminance", model.getSunIlluminance());
        computeShaderManager.setUniform("sunDirection", model.getSunDirection());
        computeShaderManager.setUniform("camera", camera.getPosition());

        glBindImageTexture(0, skyViewMap.getId(), 0, false, 0, GL_WRITE_ONLY, GL_RGBA32F);
        transmittanceMap.bind(
                computeShaderManager.getShaderProgram(),
                "transmittanceTexture",
                1
        );
        multipleScatteringMap.bind(
                computeShaderManager.getShaderProgram(),
                "multiScatteringTexture",
                2
        );

        computeShaderManager.dispatchCompute(skyViewGroupsX, skyViewGroupsY, 1);

        computeShaderManager.memoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        computeShaderManager.unbind();
    }

    public void initAerialPerspective(ComputeShaderManager computeShaderManager, HillarieModel model) throws Exception {
        computeShaderManager.link();
        computeShaderManager.bind();
        computeShaderManager.createUniform("uAerialPerspectiveTextureSize");
        computeShaderManager.createUniform("uScatteringTextureSize");
        computeShaderManager.createUniform("uTransmittanceTextureSize");
        computeShaderManager.createUniform("RayMarchMinMaxSPP");
        computeShaderManager.createUniform("sunIlluminance");
        computeShaderManager.createUniform("projMatInv");
        computeShaderManager.createUniform("viewMatInv");
        computeShaderManager.createUniform("invViewProj");
        computeShaderManager.createUniform("sunDirection");
        computeShaderManager.createUniform("camera");
        computeShaderManager.createUniform("isSmall");
        model.createUniforms(computeShaderManager, "uAtmosphere");

        computeShaderManager.setUniform("uAerialPerspectiveTextureSize", aerialPerspectiveSize);
        computeShaderManager.setUniform("uScatteringTextureSize", scatteringSize);
        computeShaderManager.setUniform("uTransmittanceTextureSize", transmittanceSize);
        computeShaderManager.setUniform("RayMarchMinMaxSPP", new Vector2f(4.0f, 14.0f));
        computeShaderManager.setUniform("isSmall", model.isSmall());
        model.setUniforms(computeShaderManager, "uAtmosphere");
    }

    public void precomputeAerialPerspective(HillarieModel model, Camera camera) {
        precomputeAerialPerspective(aerialPerspectiveComputeShaderManager, model, camera);
    }
    public void precomputeAerialPerspective(ComputeShaderManager computeShaderManager, HillarieModel model, Camera camera) {
        computeShaderManager.bind();

        Matrix4f viewMatrixRot = Transformation.getViewMatrixRotationOnly(camera);
        Matrix4f viewMatrix = Transformation.getViewMatrix(camera);
        Matrix4f projectionMatrix = Main.getWindowManager().getProjectionMatrix();
        Matrix4f invViewProj = new Matrix4f();

        computeShaderManager.setUniform("sunIlluminance", model.getSunIlluminance());
        computeShaderManager.setUniform("projMatInv", new Matrix4f(projectionMatrix).invert());
        computeShaderManager.setUniform("viewMatInv", new Matrix4f(viewMatrixRot).invert());
        computeShaderManager.setUniform("invViewProj", projectionMatrix.invertPerspectiveView(viewMatrix, invViewProj));
        computeShaderManager.setUniform("sunDirection", model.getSunDirection());
        computeShaderManager.setUniform("camera", camera.getPosition());

        glBindImageTexture(0, aerialPerspectiveMap.getId(), 0, true, 0, GL_WRITE_ONLY, GL_RGBA32F);
        transmittanceMap.bind(
                computeShaderManager.getShaderProgram(),
                "transmittanceTexture",
                1
        );
        multipleScatteringMap.bind(
                computeShaderManager.getShaderProgram(),
                "multiScatteringTexture",
                2
        );

        computeShaderManager.dispatchCompute(aerialPerspectiveGroupsX, aerialPerspectiveGroupsY, aerialPerspectiveGroupsZ);

        computeShaderManager.memoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        computeShaderManager.unbind();
    }

    public void updateMultiScattering(HillarieModel model) {
        precomputeMultiScattering(multiScatteringComputeShaderManager, model);
    }

    public void updateSkyAndFog(HillarieModel model, Camera camera) {
        precomputeSkyView(skyViewComputeShaderManager, model, camera);
        precomputeAerialPerspective(aerialPerspectiveComputeShaderManager, model, camera);
    }
    private void setupLinearFiltering() {
        glBindTexture(GL_TEXTURE_3D, aerialPerspectiveMap.getId());
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);

        glBindTexture(GL_TEXTURE_2D, skyViewMap.getId());
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        glBindTexture(GL_TEXTURE_2D, transmittanceMap.getId());
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        glBindTexture(GL_TEXTURE_2D, multipleScatteringMap.getId());
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        glBindTexture(GL_TEXTURE_2D, 0);
    }
}
