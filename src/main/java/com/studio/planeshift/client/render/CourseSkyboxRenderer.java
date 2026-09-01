package com.studio.planeshift.client.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.studio.planeshift.PlaneShift;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.client.renderer.state.SkyRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.CustomSkyboxRenderer;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Camera-centred course panorama.
 *
 * <p>The cubemap has no world-space translation, so the distant scenery stays nearly static as
 * the player runs. The authored side-on camera still supplies rotation, making the panorama feel
 * like a classic painted platformer backdrop without turning the HUD into a fake background.
 */
public final class CourseSkyboxRenderer implements CustomSkyboxRenderer {

    private static final int VERTEX_COUNT = 24;
    private static final int INDEX_COUNT = 36;

    private static Identifier getTexture() {
        com.studio.planeshift.common.course.CourseTheme theme = com.studio.planeshift.client.ClientCourseState.get().theme();
        return PlaneShift.id("textures/environment/course_skybox_" + theme.getSerializedName() + ".png");
    }

    private GpuBuffer cubeBuffer;

    @Override
    public boolean renderSky(LevelRenderState levelRenderState, SkyRenderState skyRenderState,
                             Matrix4f modelViewMatrix, Runnable setupFog) {
        setupFog.run();
        if (cubeBuffer == null) {
            cubeBuffer = buildCube();
        }

        Minecraft minecraft = Minecraft.getInstance();
        AbstractTexture texture = minecraft.getTextureManager().getTexture(getTexture());
        RenderSystem.AutoStorageIndexBuffer indices =
                RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        GpuBuffer indexBuffer = indices.getBuffer(INDEX_COUNT);
        GpuTextureView color = minecraft.getMainRenderTarget().getColorTextureView();
        GpuTextureView depth = minecraft.getMainRenderTarget().getDepthTextureView();
        GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().writeTransform(
                modelViewMatrix, new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
                new Vector3f(), new Matrix4f());

        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "PlaneShift course skybox", color, OptionalInt.empty(),
                depth, OptionalDouble.empty())) {
            RenderPipeline pipeline = RenderPipelines.END_SKY;
            pass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", transforms);
            pass.bindTexture("Sampler0", texture.getTextureView(), texture.getSampler());
            pass.setVertexBuffer(0, cubeBuffer);
            pass.setIndexBuffer(indexBuffer, indices.type());
            pass.drawIndexed(0, 0, INDEX_COUNT, 1);
        }
        return true;
    }

    private static GpuBuffer buildCube() {
        VertexFormat format = DefaultVertexFormat.POSITION_TEX_COLOR;
        try (ByteBufferBuilder bytes = ByteBufferBuilder.exactlySized(
                VERTEX_COUNT * format.getVertexSize())) {
            BufferBuilder vertices = new BufferBuilder(bytes, VertexFormat.Mode.QUADS, format);
            for (int face = 0; face < 6; face++) {
                Matrix4f rotation = new Matrix4f();
                switch (face) {
                    case 1 -> rotation.rotationX((float) (Math.PI / 2.0D));
                    case 2 -> rotation.rotationX((float) (-Math.PI / 2.0D));
                    case 3 -> rotation.rotationX((float) Math.PI);
                    case 4 -> rotation.rotationZ((float) (Math.PI / 2.0D));
                    case 5 -> rotation.rotationZ((float) (-Math.PI / 2.0D));
                    default -> {
                        // Front face uses the identity transform.
                    }
                }
                addFace(vertices, rotation);
            }
            try (MeshData mesh = vertices.buildOrThrow()) {
                return RenderSystem.getDevice().createBuffer(
                        () -> "PlaneShift course skybox vertices", 40, mesh.vertexBuffer());
            }
        }
    }

    private static void addFace(BufferBuilder vertices, Matrix4f transform) {
        int white = 0xFFFFFFFF;
        vertices.addVertex(transform, -100.0F, -100.0F, -100.0F)
                .setUv(0.0F, 0.0F).setColor(white);
        vertices.addVertex(transform, -100.0F, -100.0F, 100.0F)
                .setUv(0.0F, 1.0F).setColor(white);
        vertices.addVertex(transform, 100.0F, -100.0F, 100.0F)
                .setUv(1.0F, 1.0F).setColor(white);
        vertices.addVertex(transform, 100.0F, -100.0F, -100.0F)
                .setUv(1.0F, 0.0F).setColor(white);
    }
}
