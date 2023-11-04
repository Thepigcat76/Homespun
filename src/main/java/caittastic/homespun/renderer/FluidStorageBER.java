package caittastic.homespun.renderer;

import caittastic.homespun.blockentity.FluidStorageBE;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Random;

public class FluidStorageBER implements BlockEntityRenderer<FluidStorageBE>{
  public FluidStorageBER(BlockEntityRendererProvider.Context context){}

  @Override
  public void render(
          @NotNull FluidStorageBE entity,
          float partialTick,
          @NotNull PoseStack poseStack,
          @NotNull MultiBufferSource bufferSource,
          int packedLight,
          int packedOverlay){
    Minecraft minecraft = Minecraft.getInstance();
    float pixHeight = 0.0625f;
    int lightLevel = getLightLevel(Objects.requireNonNull(entity.getLevel()), entity.getBlockPos());
    Random rand = new Random();
    rand.setSeed((long)entity.getBlockPos().getX() * entity.getBlockPos().getZ() * entity.getBlockPos().getY());

    poseStack.pushPose();
    poseStack.popPose();

    // fluid rendering starts here //
    FluidStack fluidStack = entity.getRenderFluid();
    Fluid fluid = fluidStack.getFluid();

    if(fluidStack.isEmpty())
      return;

    int fluidBrightness = Math.max(lightLevel, fluidStack.getFluid().getFluidType().getLightLevel(fluidStack));
    int l2 = fluidBrightness >> 0x10 & 0xFFFF;
    int i3 = fluidBrightness & 0xFFFF;

    IClientFluidTypeExtensions renderProperties = IClientFluidTypeExtensions.of(fluid);
    int fluidTintColour = renderProperties.getTintColor(fluidStack);
    float red = (fluidTintColour >> 16 & 0xFF) / 255.0F;
    float green = (fluidTintColour >> 8 & 0xFF) / 255.0F;
    float blue = (fluidTintColour & 0xFF) / 255.0F;
    float alpha = ((fluidTintColour >> 24) & 0xFF) / 255F;

    TextureAtlasSprite stillFluidSprite =
            minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(renderProperties.getStillTexture(fluidStack));
    VertexConsumer vertexBuffer = bufferSource.getBuffer(RenderType.translucentNoCrumbling());

    int startPixel = 4;
    int endPixel = 12;
    int minFluidHeight = 2;
    int maxFluidHeight = 15;
    int fluidHeightDif = maxFluidHeight - minFluidHeight;
    float fluidAmount = fluidStack.getAmount();
    float fluidMax = FluidStorageBE.TANK_CAPACITY;

    Matrix4f lastPose = poseStack.last().pose();
    Matrix3f matrix = poseStack.last().normal();

    float fluidY = Float.MIN_VALUE + median(
            pixHeight * minFluidHeight,
            pixHeight * (minFluidHeight + ((fluidAmount / fluidMax) * fluidHeightDif)),
            pixHeight * maxFluidHeight);

    //todo scale fluid texture so it is pixel consistant
    float fluidStartDrawPixel = pixHeight * startPixel;
    float fluidEndDrawPixel = pixHeight * endPixel;
    float fluidSpriteU0 = stillFluidSprite.getU0() + (stillFluidSprite.getU1() - stillFluidSprite.getU0()) / 4;
    float fluidSpriteU1 = stillFluidSprite.getU1() - (stillFluidSprite.getU1() - stillFluidSprite.getU0()) / 4;
    float fluidSpriteV0 = stillFluidSprite.getV0() + (stillFluidSprite.getV1() - stillFluidSprite.getV0()) / 4;
    float fluidSpriteV1 = stillFluidSprite.getV1() - (stillFluidSprite.getV1() - stillFluidSprite.getV0()) / 4;
    //north-west
    vertexBuffer.vertex(lastPose, fluidStartDrawPixel, fluidY, fluidStartDrawPixel).color(red, green, blue, alpha).uv(fluidSpriteU0, fluidSpriteV1).uv2(l2, i3).normal(matrix, 0, 1, 0).endVertex();
    //south-west
    vertexBuffer.vertex(lastPose, fluidStartDrawPixel, fluidY, fluidEndDrawPixel).color(red, green, blue, alpha).uv(fluidSpriteU0, fluidSpriteV0).uv2(l2, i3).normal(matrix, 0, 1, 0).endVertex();
    //south-east
    vertexBuffer.vertex(lastPose, fluidEndDrawPixel, fluidY, fluidEndDrawPixel).color(red, green, blue, alpha).uv(fluidSpriteU1, fluidSpriteV0).uv2(l2, i3).normal(matrix, 0, 1, 0).endVertex();
    //north-east
    vertexBuffer.vertex(lastPose, fluidEndDrawPixel, fluidY, fluidStartDrawPixel).color(red, green, blue, alpha).uv(fluidSpriteU1, fluidSpriteV1).uv2(l2, i3).normal(matrix, 0, 1, 0).endVertex();
  }

  private int getLightLevel(Level level, BlockPos pos){
    int blockLight = level.getBrightness(LightLayer.BLOCK, pos);
    int skyLight = level.getBrightness(LightLayer.SKY, pos);
    return LightTexture.pack(blockLight, skyLight);
  }

  float median(float a, float b, float c){
    return Math.max(Math.min(a, b), Math.min(Math.max(a, b), c));
  }
}
