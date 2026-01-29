package top.yzljc.playVideo.video;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.scheduler.BukkitRunnable;

public class VideoPlayerTask extends BukkitRunnable {

    private final VideoCache cache;
    private final Location origin;
    private final double spacing;
    private final double fps; // 目标视频的 FPS

    private int currentTick = 0;

    public VideoPlayerTask(VideoCache cache, Location origin, double spacing, double fps) {
        this.cache = cache;
        this.origin = origin;
        this.spacing = spacing;
        this.fps = fps;
    }

    @Override
    public void run() {
        int frameIndex = (int) ((currentTick / 20.0) * fps);

        if (frameIndex >= cache.getTotalFrames()) {
            this.cancel();
            return;
        }

        int[] frameData = cache.getFrame(frameIndex);
        if (frameData == null) return;

        int width = cache.getWidth();
        int height = cache.getHeight();

        // 遍历所有像素点
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = frameData[y * width + x];

                Color color = Color.fromRGB(
                        (pixel >> 16) & 0xFF,
                        (pixel >> 8) & 0xFF,
                        pixel & 0xFF
                );

                double offsetX = (x - width / 2.0) * spacing;
                double offsetY = (height - y) * spacing;

                Location particleLoc = origin.clone().add(offsetX, offsetY, 0);

                // 生成彩色粒子
                Particle.DustOptions dustOptions = new Particle.DustOptions(color, 1.0f); // 1.0f 是粒子大小
                origin.getWorld().spawnParticle(Particle.DUST, particleLoc, 1, dustOptions);
            }
        }

        currentTick++;
    }
}