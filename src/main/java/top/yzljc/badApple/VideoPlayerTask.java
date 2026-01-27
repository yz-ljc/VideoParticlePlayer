package top.yzljc.badApple;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class VideoPlayerTask extends BukkitRunnable {

    private final VideoCache cache;
    private final Location origin;
    private final double spacing;

    private static final double SPEED_MULTIPLIER = 1;
    private int currentTick = 0;

    private final Particle.DustOptions blackDust = new Particle.DustOptions(Color.BLACK, 1.0f); // 大小可以调大一点让它更明显
    private final Particle.DustOptions whiteDust = new Particle.DustOptions(Color.WHITE, 1.0f);

    public VideoPlayerTask(VideoCache cache, Location origin, double spacing) {
        this.cache = cache;
        this.origin = origin;
        this.spacing = spacing;
    }

    @Override
    public void run() {
        int frameIndex = (int) (currentTick * SPEED_MULTIPLIER);

        if (frameIndex >= cache.getTotalFrames()) {
            this.cancel();
            return;
        }

        boolean[] frameData = cache.getFrame(frameIndex);
        if (frameData == null) return;

        int width = cache.getWidth();
        int height = cache.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boolean isWhite = frameData[y * width + x];

                double offsetX = (x - width / 2.0) * spacing;
                double offsetY = (height - y) * spacing;

                Location particleLoc = origin.clone().add(offsetX, offsetY, 0);

                origin.getWorld().spawnParticle(
                        Particle.DUST,
                        particleLoc,
                        1,
                        isWhite ? whiteDust : blackDust
                );
            }
        }

        currentTick++;
    }
}