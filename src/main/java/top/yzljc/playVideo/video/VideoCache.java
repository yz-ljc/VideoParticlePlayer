package top.yzljc.playVideo.video;

import org.bukkit.plugin.java.JavaPlugin;
import org.jcodec.api.FrameGrab;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class VideoCache {

    private final List<int[]> frames = new ArrayList<>();
    private final int width;
    private final int height;
    private final int sourceWidth;
    private final int sourceHeight;
    private final File sourceFile;
    private final Logger logger;

    public VideoCache(JavaPlugin plugin, File videoFile, int targetWidth) throws Exception {
        this.width = targetWidth;
        this.sourceFile = videoFile;
        this.logger = plugin.getLogger();
        try (SeekableByteChannel channel = NIOUtils.readableChannel(videoFile)) {
            FrameGrab grab = FrameGrab.createFrameGrab(channel);
            Picture first = grab.getNativeFrame();
            if (first == null) {
                throw new IllegalArgumentException("无法读取视频第一帧！");
            }
            this.sourceWidth = first.getWidth();
            this.sourceHeight = first.getHeight();
            this.height = Math.max(1, (int) Math.round((double) targetWidth * sourceHeight / sourceWidth));

            int frameCount = 0;
            Picture frame = first;
            do {
                BufferedImage bufImg = AWTUtil.toBufferedImage(frame);
                frames.add(processImage(bufImg, width, height));
                frameCount++;
                if (frameCount % 50 == 0) {
                    logger.info("Processing frame: " + frameCount);
                }
            } while ((frame = grab.getNativeFrame()) != null);
        }
    }

    private int[] processImage(BufferedImage original, int w, int h) {
        java.awt.Image tmp = original.getScaledInstance(w, h, java.awt.Image.SCALE_SMOOTH);
        BufferedImage resized = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        resized.getGraphics().drawImage(tmp, 0, 0, null);

        return resized.getRGB(0, 0, w, h, null, 0, w);
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getSourceWidth() { return sourceWidth; }
    public int getSourceHeight() { return sourceHeight; }
    public File getSourceFile() { return sourceFile; }
    public int getTotalFrames() { return frames.size(); }

    public int[] getFrame(int index) {
        if (index < 0 || index >= frames.size()) return null;
        return frames.get(index);
    }
}
