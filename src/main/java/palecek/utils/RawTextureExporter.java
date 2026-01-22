package palecek.utils;

import org.lwjgl.BufferUtils;
import palecek.core.utils.Texture;
import palecek.core.utils.Texture3D;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL12C.GL_TEXTURE_3D;

public class RawTextureExporter {

    public static void saveTexture2DFloat(
            Texture texture,
            int width,
            int height,
            String path
    ) throws IOException {

        glBindTexture(GL_TEXTURE_2D, texture.getId());
        glPixelStorei(GL_PACK_ALIGNMENT, 1);

        int channels = 4; // RGBA
        FloatBuffer buffer = BufferUtils.createFloatBuffer(width * height * channels);

        glGetTexImage(GL_TEXTURE_2D, 0, GL_RGBA, GL_FLOAT, buffer);

        writeFloatBuffer(buffer, path);
    }

    public static void saveTexture3DFloat(
            Texture3D texture,
            int width,
            int height,
            int depth,
            String path
    ) throws IOException {

        glBindTexture(GL_TEXTURE_3D, texture.getId());
        glPixelStorei(GL_PACK_ALIGNMENT, 1);

        int channels = 4; // RGBA
        FloatBuffer buffer = BufferUtils.createFloatBuffer(width * height * depth * channels);

        glGetTexImage(GL_TEXTURE_3D, 0, GL_RGBA, GL_FLOAT, buffer);

        writeFloatBuffer(buffer, path);
    }

    private static void writeFloatBuffer(FloatBuffer buffer, String path) throws IOException {
        buffer.rewind();

        ByteBuffer byteBuffer = ByteBuffer
                .allocateDirect(buffer.remaining() * Float.BYTES)
                .order(java.nio.ByteOrder.LITTLE_ENDIAN);

        byteBuffer.asFloatBuffer().put(buffer);

        try (FileOutputStream out = new FileOutputStream(path)) {
            byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);
            out.write(bytes);
        }
    }

    public static FloatBuffer loadRawTexture(String path, int width, int height, int depth, int channels) throws Exception {
        File file = new File(path);
        byte[] bytes = new byte[(int) file.length()];

        try (FileInputStream fis = new FileInputStream(file)) {
            int read = fis.read(bytes);
            if (read != bytes.length) {
                throw new RuntimeException("Failed to read full file");
            }
        }

        FloatBuffer buffer = ByteBuffer
                .allocateDirect(bytes.length)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();

        // Convert byte[] to float[] (assuming little endian float32)
        for (int i = 0; i < bytes.length; i += 4) {
            int bits = (bytes[i] & 0xFF) |
                    ((bytes[i + 1] & 0xFF) << 8) |
                    ((bytes[i + 2] & 0xFF) << 16) |
                    ((bytes[i + 3] & 0xFF) << 24);
            buffer.put(Float.intBitsToFloat(bits));
        }

        buffer.flip();
        return buffer;
    }
}
