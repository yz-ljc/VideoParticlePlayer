package top.yzljc.playVideo.video;

import org.bukkit.plugin.java.JavaPlugin;
import org.jcodec.api.FrameGrab;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;
import top.yzljc.playVideo.PlayVideo;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class VideoCache {

    private final List<int[]> frames = new ArrayList<>();
    private final int width;
    private final int height;
    private final Logger logger;
    private final double frameRate;

    public VideoCache(JavaPlugin plugin, File videoFile, int targetWidth) throws Exception {
        this.width = targetWidth;
        this.logger = plugin.getLogger();
        FrameGrab grab = FrameGrab.createFrameGrab(NIOUtils.readableChannel(videoFile));
        Picture picture = grab.getNativeFrame();

        if (picture == null) {
            throw new RuntimeException("无法读取视频第一帧！");
        }

        this.frameRate = 30.0; // default FPS

        double aspectRatio = (double) picture.getHeight() / picture.getWidth();
        this.height = (int) (targetWidth * aspectRatio);

        grab = FrameGrab.createFrameGrab(NIOUtils.readableChannel(videoFile));

        int frameCount = 0;
        Picture frame;
        while ((frame = grab.getNativeFrame()) != null) {
            BufferedImage bufImg = AWTUtil.toBufferedImage(frame);
            int[] frameData = processImage(bufImg, width, height);
            frames.add(frameData);

            frameCount++;
            if (frameCount % 50 == 0) {
                logger.info("Processing frame: " + frameCount);
            }
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
    public int getTotalFrames() { return frames.size(); }

    public int[] getFrame(int index) {
        if (index < 0 || index >= frames.size()) return null;
        return frames.get(index);
    }
}