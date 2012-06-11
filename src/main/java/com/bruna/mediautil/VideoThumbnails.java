package com.bruna.mediautil;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.MediaListenerAdapter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.mediatool.event.IVideoPictureEvent;
import com.xuggle.xuggler.Global;

public class VideoThumbnails {

	public static final double SECONDS_BETWEEN_FRAMES = 1;

	private static final String INPUT_FILENAME = "B:/dev/Bunny_Video/Big Buck Bunny.mp4";
	private static final String MOVIE_NAME = "Big Buck Bunny";
	private static final String OUTPUT_FILE_PREFIX = "B:/dev/Bunny_Video/snapshots/";
	private static final int THUMBNAIL_SIZE = 120;

	// The video stream index, used to ensure we display frames from one and
	// only one video stream from the media container.
	private static int mVideoStreamIndex = -1;

	// Time of last frame write
	private static long mLastPtsWrite = Global.NO_PTS;

	public static final long MICRO_SECONDS_BETWEEN_FRAMES = (long) (Global.DEFAULT_PTS_PER_SECOND * SECONDS_BETWEEN_FRAMES);

	public static void main(String[] args) {

		IMediaReader mediaReader = ToolFactory.makeReader(INPUT_FILENAME);

		// stipulate that we want BufferedImages created in BGR 24bit color
		// space
		mediaReader.setBufferedImageTypeToGenerate(BufferedImage.TYPE_3BYTE_BGR);

		mediaReader.addListener(new ImageSnapListener());

		// read out the contents of the media file and
		// dispatch events to the attached listener
		while (mediaReader.readPacket() == null)
			;

	}

	private static class ImageSnapListener extends MediaListenerAdapter {

		public void onVideoPicture(IVideoPictureEvent event) {

			if (event.getStreamIndex() != mVideoStreamIndex) {
				// if the selected video stream id is not yet set, go ahead an
				// select this lucky video stream
				if (mVideoStreamIndex == -1)
					mVideoStreamIndex = event.getStreamIndex();
				// no need to show frames from this video stream
				else
					return;
			}

			// if uninitialized, back date mLastPtsWrite to get the very first
			// frame
			if (mLastPtsWrite == Global.NO_PTS)
				mLastPtsWrite = event.getTimeStamp()
						- MICRO_SECONDS_BETWEEN_FRAMES;

			// if it's time to write the next frame
			if (event.getTimeStamp() - mLastPtsWrite >= MICRO_SECONDS_BETWEEN_FRAMES) {

				// indicate file written
				double seconds = ((double) event.getTimeStamp())
						/ Global.DEFAULT_PTS_PER_SECOND;
				String outputFilename = dumpImageToFile(event.getImage(),
						(int) seconds);
				System.out.printf(
						"at elapsed time of %6.3f seconds wrote: %s\n",
						seconds, outputFilename);

				// update last write time
				mLastPtsWrite += MICRO_SECONDS_BETWEEN_FRAMES;
			}

		}

		private String dumpImageToFile(BufferedImage image, int seconds) {
			try {
				String outputFilename = OUTPUT_FILE_PREFIX + MOVIE_NAME
						+ String.valueOf(seconds) + ".jpg";
				BufferedImage resizedImage = scale(image, THUMBNAIL_SIZE, THUMBNAIL_SIZE);
				ImageIO.write(resizedImage, "jpg", new File(outputFilename));
				return outputFilename;
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}
	}

	/**
	 * Resize image.
	 * 
	 * @param image
	 * @param p_width
	 * @param p_height
	 * @return
	 */
	public static BufferedImage scale(BufferedImage image, int p_width,
			int p_height) {

		int type = image.getType();

		if (type == BufferedImage.TYPE_CUSTOM) {
			type = BufferedImage.TYPE_3BYTE_BGR;
		}

		// Aspect ratio
		int thumbWidth = p_width;
		int thumbHeight = p_height;
		int imageWidth = image.getWidth();
		int imageHeight = image.getHeight();
		double thumbRatio = (double) thumbWidth / (double) thumbHeight;
		double imageRatio = (double) imageWidth / (double) imageHeight;
		if (thumbRatio < imageRatio) {
			thumbHeight = (int) (thumbWidth / imageRatio);
		} else {
			thumbWidth = (int) (thumbHeight * imageRatio);
		}

		BufferedImage resizedImage = new BufferedImage(thumbWidth, thumbHeight,
				type);
		Graphics2D graphics = resizedImage.createGraphics();
		graphics.drawImage(image, 0, 0, thumbWidth, thumbHeight, null);
		graphics.dispose();

		return resizedImage;
	}

}
