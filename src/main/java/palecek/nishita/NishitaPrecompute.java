package palecek.nishita;

import org.joml.Vector3f;
import palecek.core.ComputeShaderManager;
import palecek.core.Utils;
import palecek.core.utils.Texture;
import palecek.core.utils.TextureExporter;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengles.GLES31.GL_READ_ONLY;
import static org.lwjgl.opengl.GL30C.GL_RGBA32F;
import static org.lwjgl.opengl.GL42C.GL_ALL_BARRIER_BITS;
import static org.lwjgl.opengl.GL42C.glBindImageTexture;
import static org.lwjgl.opengles.GLES31.GL_WRITE_ONLY;


public class NishitaPrecompute {

    private static final float R_E = 6360.0f;
    private static final float R_A = 6420.0f;
    private static final float H_R = 8.0f;
    private static final float H_M = 1.2f;
    private static final float g = 0.75f;

    private static final Vector3f BETA_R = new Vector3f(0.0058f, 0.0135f, 0.0331f);

    private static final Vector3f BETA_M = new Vector3f(0.0021f);

    public Texture[] precompute(ComputeShaderManager computeShaderManager, int width, int height) throws Exception {
        Texture transmittanceMap = new Texture(width, height, GL_RGBA32F, GL_RGBA, GL_FLOAT, null);
        Texture singleScatteringMap = new Texture(width, height, GL_RGBA32F, GL_RGBA, GL_FLOAT, null);

        int groupsX = (int) Math.ceil((double) width / 8);
        int groupsY = (int) Math.ceil((double) height / 4);

        computeShaderManager.createComputeShader(Utils.loadResource("/shaders/precompute/nishita/transmittance.glsl"));
        computeShaderManager.link();
        computeShaderManager.bind();

        computeShaderManager.createUniform("Re");
        computeShaderManager.createUniform("Ra");
        computeShaderManager.createUniform("Hr");
        computeShaderManager.createUniform("Hm");
        computeShaderManager.createUniform("BetaR");
        computeShaderManager.createUniform("BetaM");

        computeShaderManager.setUniform("Re", R_E);
        computeShaderManager.setUniform("Ra", R_A);
        computeShaderManager.setUniform("Hr", H_R);
        computeShaderManager.setUniform("Hm", H_M);
        computeShaderManager.setUniform("BetaR", BETA_R);
        computeShaderManager.setUniform("BetaM", BETA_M);


        glBindImageTexture(0, transmittanceMap.getId(), 0, false, 0, GL_WRITE_ONLY, GL_RGBA32F);
        computeShaderManager.dispatchCompute(groupsX, groupsY, 1);

        computeShaderManager.memoryBarrier(GL_ALL_BARRIER_BITS);
        computeShaderManager.unbind();


        computeShaderManager.createComputeShader(Utils.loadResource("/shaders/precompute/nishita/single_scattering.glsl"));
        computeShaderManager.link();
        computeShaderManager.bind();

        computeShaderManager.createUniform("Re");
        computeShaderManager.createUniform("Ra");
        computeShaderManager.createUniform("Hr");
        computeShaderManager.createUniform("Hm");
        computeShaderManager.createUniform("g");
        computeShaderManager.createUniform("BetaR");
        computeShaderManager.createUniform("BetaM");

        computeShaderManager.setUniform("Re", R_E);
        computeShaderManager.setUniform("Ra", R_A);
        computeShaderManager.setUniform("Hr", H_R);
        computeShaderManager.setUniform("Hm", H_M);
        computeShaderManager.setUniform("g", g);
        computeShaderManager.setUniform("BetaR", BETA_R);
        computeShaderManager.setUniform("BetaM", BETA_M);

        glBindImageTexture(0, transmittanceMap.getId(), 0, false, 0, GL_READ_ONLY, GL_RGBA32F);
        glBindImageTexture(1, singleScatteringMap.getId(), 0, false, 0, GL_WRITE_ONLY, GL_RGBA32F);

        computeShaderManager.dispatchCompute(groupsX, groupsY, 1);

        computeShaderManager.memoryBarrier(GL_ALL_BARRIER_BITS);
        computeShaderManager.unbind();

        TextureExporter.saveHDRTextureToPNG(transmittanceMap, width, height, "images/transmittance_map.png", 1f);
        TextureExporter.saveHDRTextureToPNG(singleScatteringMap, width, height, "images/single_scattering_map.png", 1f);

        return new Texture[]{transmittanceMap, singleScatteringMap};
    }
}
