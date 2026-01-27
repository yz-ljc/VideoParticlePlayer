package top.yzljc.badApple.video;

import org.jcodec.api.FrameGrab;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class VideoCache {

    private final List<boolean[]> frames = new ArrayList<>();
    private final int width;
    private final int height;

    private static final int THRESHOLD = 128;

    public VideoCache(File videoFile, int targetWidth) throws Exception {
        this.width = targetWidth;

        FrameGrab grab = FrameGrab.createFrameGrab(NIOUtils.readableChannel(videoFile));
        Picture picture = grab.getNativeFrame();

        double aspectRatio = (double) picture.getHeight() / picture.getWidth();
        this.height = (int) (targetWidth * aspectRatio);

        grab = FrameGrab.createFrameGrab(NIOUtils.readableChannel(videoFile));

        int frameCount = 0;
        Picture frame;
        while ((frame = grab.getNativeFrame()) != null) {
            BufferedImage bufImg = AWTUtil.toBufferedImage(frame);

            boolean[] frameData = processImage(bufImg, width, height);
            frames.add(frameData);

            frameCount++;
            if (frameCount % 100 == 0) {
                System.out.println("已加载帧数: " + frameCount);
            }
        }
    }

    private boolean[] processImage(BufferedImage original, int w, int h) {
        java.awt.Image tmp = original.getScaledInstance(w, h, java.awt.Image.SCALE_SMOOTH);
        BufferedImage resized = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        resized.getGraphics().drawImage(tmp, 0, 0, null);

        boolean[] data = new boolean[w * h];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = resized.getRGB(x, y) & 0xFF;
                data[y * w + x] = rgb > THRESHOLD;
            }
        }
        return data;
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getTotalFrames() { return frames.size(); }

    public boolean[] getFrame(int index) {
        if (index < 0 || index >= frames.size()) return null;
        return frames.get(index);
    }
}