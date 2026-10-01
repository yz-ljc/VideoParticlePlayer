package top.yzljc.playVideo.video;

import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Cushion;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jcodec.api.FrameGrab;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/** Plays native video frames on a horizontal grid of temporary cushions. */
public class CushionVideoPlayerTask extends BukkitRunnable {

    // This controls work per server tick, not the total video resolution.
    private static final int SPAWN_PER_TICK = 256;
    private static final DyeColor[] PALETTE = DyeColor.values();
    private static final DyeColor[] GRAYSCALE_PALETTE = {
            DyeColor.BLACK, DyeColor.GRAY, DyeColor.LIGHT_GRAY, DyeColor.WHITE
    };
    private static final byte[] GRAYSCALE_LOOKUP = buildGrayscaleLookup();

    private final VideoCache cache;
    private final Player player;
    private final World world;
    private final double fps;
    private final int width;
    private final int height;
    private final int forwardX;
    private final int forwardZ;
    private final int rightX;
    private final int rightZ;
    private final int startX;
    private final int startZ;
    private final int groundY;
    private final Cushion[] cushions;
    private final byte[] previousColors;
    private final ArrayBlockingQueue<DecodedFrame> frames = new ArrayBlockingQueue<>(4);

    private volatile boolean stopped;
    private volatile boolean decodingComplete;
    private volatile Throwable decodeFailure;
    private volatile int decodedFrames;
    private Thread decoderThread;
    private int spawnedCount;
    private int lastFrame = -1;
    private long playbackTick;

    public CushionVideoPlayerTask(VideoCache cache, Player player, double fps, int width) {
        if (width < 1) {
            throw new IllegalArgumentException("画面宽度必须大于 0");
        }
        this.cache = cache;
        this.player = player;
        this.world = player.getWorld();
        this.fps = fps;
        this.width = width;
        this.height = Math.max(1, (int) Math.round((double) cache.getSourceHeight() * width / cache.getSourceWidth()));
        try {
            this.cushions = new Cushion[Math.multiplyExact(width, height)];
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("画面尺寸超出 Java 数组可表示范围", e);
        }
        this.previousColors = new byte[cushions.length];

        Location location = player.getLocation();
        double lookX = location.getDirection().getX();
        double lookZ = location.getDirection().getZ();
        if (Math.abs(lookX) > Math.abs(lookZ)) {
            this.forwardX = lookX >= 0 ? 1 : -1;
            this.forwardZ = 0;
        } else {
            this.forwardX = 0;
            this.forwardZ = lookZ >= 0 ? 1 : -1;
        }
        this.rightX = -forwardZ;
        this.rightZ = forwardX;
        this.groundY = location.getBlockY() - 1;
        this.startX = location.getBlockX() + forwardX * 2 - rightX * (width / 2);
        this.startZ = location.getBlockZ() + forwardZ * 2 - rightZ * (width / 2);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void prepare() {
        if (!cache.getSourceFile().isFile()) {
            throw new IllegalArgumentException("视频文件已不存在");
        }
    }

    @Override
    public void run() {
        try {
            if (spawnedCount < cushions.length) {
                spawnNextBatch();
                return;
            }
            if (decodeFailure != null) {
                throw new IllegalStateException("读取原视频失败: " + decodeFailure.getMessage(), decodeFailure);
            }

            long targetFrame = (long) (playbackTick * fps / 20.0);
            DecodedFrame latest = null;
            DecodedFrame head;
            while ((head = frames.peek()) != null && head.index() <= targetFrame) {
                latest = frames.poll();
            }
            if (latest != null) {
                applyFrame(latest.colors());
                lastFrame = latest.index();
            }
            if (decodingComplete && frames.isEmpty() && targetFrame >= decodedFrames) {
                cleanup();
                cancel();
                return;
            }
            if (lastFrame >= 0) {
                playbackTick++;
            }
        } catch (RuntimeException e) {
            player.sendMessage("§c坐垫播放中断: " + e.getMessage());
            cleanup();
            cancel();
            throw e;
        }
    }

    private void spawnNextBatch() {
        int end = Math.min(cushions.length, spawnedCount + SPAWN_PER_TICK);
        for (; spawnedCount < end; spawnedCount++) {
            int x = spawnedCount % width;
            int y = spawnedCount / width;
            // Put the image's top row at the far edge so it reads correctly from above.
            int forwardOffset = height - 1 - y;
            Block floor = world.getBlockAt(startX + x * rightX + forwardOffset * forwardX,
                    groundY, startZ + x * rightZ + forwardOffset * forwardZ);
            if (!floor.getType().isOccluding() || !floor.getRelative(BlockFace.UP).isEmpty()) {
                throw new IllegalArgumentException("画面需要 " + width + "x" + height + " 的平坦完整方块地面；在第 " + (spawnedCount + 1) + " 格停止");
            }
            Location position = floor.getLocation().add(0.5, 1.0, 0.5);
            cushions[spawnedCount] = world.spawn(position, Cushion.class, cushion -> {
                cushion.setColor(DyeColor.BLACK);
                cushion.setPersistent(false);
                cushion.setInvulnerable(true);
            });
            previousColors[spawnedCount] = (byte) DyeColor.BLACK.ordinal();
        }
        if (spawnedCount == cushions.length) {
            startDecoder();
            player.sendMessage("§a坐垫画面已生成，开始读取原视频帧。");
        }
    }

    private void startDecoder() {
        decoderThread = new Thread(this::decodeVideo, "BadApple-CushionDecoder");
        decoderThread.setDaemon(true);
        decoderThread.start();
    }

    private void decodeVideo() {
        try (SeekableByteChannel channel = NIOUtils.readableChannel(cache.getSourceFile())) {
            FrameGrab grab = FrameGrab.createFrameGrab(channel);
            Picture picture;
            int index = 0;
            while (!stopped && (picture = grab.getNativeFrame()) != null) {
                DecodedFrame decoded = new DecodedFrame(index, quantizeFrame(picture));
                while (!stopped && !frames.offer(decoded, 100, TimeUnit.MILLISECONDS)) {
                    // Wait for the main thread to consume the bounded frame buffer.
                }
                if (stopped) {
                    break;
                }
                decodedFrames = ++index;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            if (!stopped) {
                decodeFailure = e;
            }
        } finally {
            decodingComplete = true;
        }
    }

    private byte[] quantizeFrame(Picture picture) {
        BufferedImage image = AWTUtil.toBufferedImage(picture);
        if (image.getWidth() != width || image.getHeight() != height) {
            BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = resized.createGraphics();
            try {
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                graphics.drawImage(image, 0, 0, width, height, null);
            } finally {
                graphics.dispose();
            }
            image = resized;
        }
        int[] rgb = image.getRGB(0, 0, width, height, null, 0, width);
        byte[] colors = new byte[rgb.length];
        for (int i = 0; i < rgb.length; i++) {
            int red = rgb[i] >> 16 & 0xff;
            int green = rgb[i] >> 8 & 0xff;
            int blue = rgb[i] & 0xff;
            int max = Math.max(red, Math.max(green, blue));
            int min = Math.min(red, Math.min(green, blue));
            colors[i] = max - min <= 24
                    ? GRAYSCALE_LOOKUP[(red + green + blue) / 3]
                    : nearestColor(red, green, blue, PALETTE);
        }
        return colors;
    }

    private void applyFrame(byte[] colors) {
        for (int i = 0; i < cushions.length; i++) {
            if (!cushions[i].isValid()) {
                throw new IllegalStateException("播放区域的坐垫被移除");
            }
            if (colors[i] != previousColors[i]) {
                cushions[i].setColor(PALETTE[colors[i] & 0xff]);
                previousColors[i] = colors[i];
            }
        }
    }

    /** Removes every entity created so far and stops decoding. */
    public void cleanup() {
        stopped = true;
        if (decoderThread != null) {
            decoderThread.interrupt();
        }
        frames.clear();
        for (int i = 0; i < spawnedCount; i++) {
            if (cushions[i] != null) {
                cushions[i].remove();
                cushions[i] = null;
            }
        }
    }

    private static byte[] buildGrayscaleLookup() {
        byte[] lookup = new byte[256];
        for (int gray = 0; gray < lookup.length; gray++) {
            lookup[gray] = nearestColor(gray, gray, gray, GRAYSCALE_PALETTE);
        }
        return lookup;
    }

    private static byte nearestColor(int red, int green, int blue, DyeColor[] palette) {
        DyeColor result = palette[0];
        int bestDistance = Integer.MAX_VALUE;
        for (DyeColor candidate : palette) {
            Color color = candidate.getColor();
            int dr = red - color.getRed();
            int dg = green - color.getGreen();
            int db = blue - color.getBlue();
            int distance = dr * dr + dg * dg + db * db;
            if (distance < bestDistance) {
                bestDistance = distance;
                result = candidate;
            }
        }
        return (byte) result.ordinal();
    }

    private record DecodedFrame(int index, byte[] colors) {}
}
