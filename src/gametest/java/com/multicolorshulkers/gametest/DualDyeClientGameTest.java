package com.multicolorshulkers.gametest;

import com.multicolorshulkers.MultiColorShulkers;
import com.multicolorshulkers.MultiColorShulkers.ShulkerColors;
import com.multicolorshulkers.client.MultiColorShulkersClient;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.TestInput;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Predicate;

/**
 * End-to-end client test: real client + integrated server, simulated input.
 * Covers RULES.md section 1 (in-world dyeing) and the dual-color block and item renderers.
 */
public class DualDyeClientGameTest implements FabricClientGameTest {

	private static final int RED = DyeColor.RED.getIndex();
	private static final int BLUE = DyeColor.BLUE.getIndex();

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			singleplayer.getClientWorld().waitForChunksRender();
			TestServerContext server = singleplayer.getServer();
			TestInput input = context.getInput();

			server.runCommand("gamemode creative @a");
			BlockPos playerPos = server.computeOnServer(s -> s.getPlayerManager().getPlayerList().get(0).getBlockPos());
			BlockPos box = playerPos.add(0, 0, 2);
			server.runCommand("setblock %d %d %d minecraft:shulker_box".formatted(box.getX(), box.getY(), box.getZ()));
			lookAt(context, box);
			context.waitTicks(2);

			// Shift + right-click with red dye → lid
			holdDye(context, server, "red_dye");
			input.holdShift();
			rightClick(context);
			input.releaseShift();
			waitForServer(context, server, s -> serverColors(s, box).topColor() == RED, "server lid red");
			context.waitFor(client -> clientColors(box).topColor() == RED);

			// Ctrl + right-click with blue dye → base
			holdDye(context, server, "blue_dye");
			input.holdControl();
			rightClick(context);
			input.releaseControl();
			waitForServer(context, server, s -> serverColors(s, box).bottomColor() == BLUE, "server base blue");
			context.waitFor(client -> clientColors(box).bottomColor() == BLUE);
			assertEquals(new ShulkerColors(RED, BLUE), serverColors(server, box), "server colors after dyeing");

			// Render check: the placed box must show red (lid) and blue (base) pixels.
			singleplayer.getClientWorld().waitForChunksRender();
			context.waitTicks(5);
			Path screenshot = context.takeScreenshot("dual_dye_shulker_placed");
			assertRedAndBlue(screenshot, 35, 65, 35, 65, 200);

			// Plain right-click (no binding) opens the box normally.
			rightClick(context);
			context.waitForScreen(ShulkerBoxScreen.class);
			context.setScreen(() -> null);
			assertEquals(new ShulkerColors(RED, BLUE), serverColors(server, box), "colors unchanged after plain right-click");

			// Item render check: the dual-colored shulker item icon in the first hotbar slot (selected).
			server.runOnServer(s -> s.getPlayerManager().getPlayerList().get(0)
					.setStackInHand(Hand.MAIN_HAND, DualDyeServerGameTests.coloredShulker(RED, BLUE)));
			context.waitFor(client -> new ShulkerColors(RED, BLUE).equals(MultiColorShulkers.getColorsFromItemStack(client.player.getMainHandStack())));
			context.waitTicks(10);
			Path itemScreenshot = context.takeScreenshot("dual_dye_shulker_item");
			assertRedAndBlue(itemScreenshot, 28, 35, 89, 100, 20);
		}
	}

	/** Client GameTest 4.x has no lookAt; aim the camera at the block center ourselves. */
	private static void lookAt(ClientGameTestContext context, BlockPos pos) {
		context.runOnClient(client -> {
			Vec3d eye = client.player.getEyePos();
			Vec3d d = Vec3d.ofCenter(pos).subtract(eye);
			double horizontal = Math.sqrt(d.x * d.x + d.z * d.z);
			client.player.setYaw((float) (MathHelper.atan2(d.z, d.x) * MathHelper.DEGREES_PER_RADIAN) - 90.0F);
			client.player.setPitch((float) -(MathHelper.atan2(d.y, horizontal) * MathHelper.DEGREES_PER_RADIAN));
		});
	}

	private static void rightClick(ClientGameTestContext context) {
		// Hold briefly: the mod reads live button state when the use action fires.
		context.getInput().holdMouse(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
		context.waitTicks(2);
		context.getInput().releaseMouse(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
		context.waitTicks(2);
	}

	private static void holdDye(ClientGameTestContext context, TestServerContext server, String dye) {
		server.runCommand("item replace entity @a weapon.mainhand with minecraft:" + dye);
		// The client must see the dye before clicking, or the use action fires with the old item.
		Identifier id = Identifier.ofVanilla(dye);
		context.waitFor(client -> Registries.ITEM.getId(client.player.getMainHandStack().getItem()).equals(id));
	}

	/** Polls from the test thread (TestServerContext has no waitFor in this API version). */
	private static void waitForServer(ClientGameTestContext context, TestServerContext server,
									  Predicate<MinecraftServer> predicate, String what) {
		for (int tick = 0; tick < ClientGameTestContext.DEFAULT_TIMEOUT; tick++) {
			if (server.computeOnServer(predicate::test)) return;
			context.waitTick();
		}
		throw new AssertionError("Timed out waiting for " + what);
	}

	private static ShulkerColors serverColors(MinecraftServer server, BlockPos pos) {
		if (server.getOverworld().getBlockEntity(pos) instanceof ShulkerBoxBlockEntity be) {
			ShulkerColors colors = be.getAttached(MultiColorShulkers.SHULKER_COLORS);
			if (colors != null) return colors;
		}
		return ShulkerColors.DEFAULT;
	}

	private static ShulkerColors serverColors(TestServerContext server, BlockPos pos) {
		return server.computeOnServer(s -> serverColors(s, pos));
	}

	private static ShulkerColors clientColors(BlockPos pos) {
		ShulkerColors colors = MultiColorShulkersClient.getColors(pos);
		return colors == null ? ShulkerColors.DEFAULT : colors;
	}

	/**
	 * Samples a region of the screenshot (in percent of width/height) and requires both clearly red
	 * and clearly blue pixels. A vanilla (purple) shulker passes neither check.
	 */
	private static void assertRedAndBlue(Path screenshot, int x0, int x1, int y0, int y1, int minPixels) {
		BufferedImage image;
		try {
			image = ImageIO.read(screenshot.toFile());
		} catch (IOException e) {
			throw new AssertionError("Could not read screenshot " + screenshot, e);
		}
		int w = image.getWidth(), h = image.getHeight();
		int red = 0, blue = 0;
		for (int y = h * y0 / 100; y < h * y1 / 100; y++) {
			for (int x = w * x0 / 100; x < w * x1 / 100; x++) {
				int rgb = image.getRGB(x, y);
				int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
				if (r > 60 && r > 2 * b && r > 2 * g) red++;
				if (b > 60 && b > 1.5 * r && b > 1.2 * g) blue++;
			}
		}
		if (red < minPixels || blue < minPixels) {
			throw new AssertionError("Expected red lid and blue base in " + screenshot
					+ " (red pixels=" + red + ", blue pixels=" + blue + ")");
		}
	}

	private static void assertEquals(Object expected, Object actual, String what) {
		if (!expected.equals(actual)) {
			throw new AssertionError(what + ": expected " + expected + " but was " + actual);
		}
	}
}
