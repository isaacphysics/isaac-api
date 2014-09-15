/**
 * Copyright 2014 Stephen Cummins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.segue.dao.content;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.google.inject.Inject;

import uk.ac.cam.cl.dtg.segue.dao.IAppDataManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.util.LatexMathToPngAdapter;

/**
 * Maths content manager is responsible for generating latex maths as images and
 * doing some caching where appropriate.
 * 
 * @author Stephen Cummins
 * 
 */
public class MathsContentManager {
	private static final int FONT_SIZE = 30;
	private final IAppDataManager<byte[]> cacheDatabase;

	/**
	 * Create a MathsContentManager.
	 * 
	 * @param database
	 *            - the database or data manager that can provide storage
	 *            support for the maths content.
	 */
	@Inject
	public MathsContentManager(final IAppDataManager<byte[]> database) {
		this.cacheDatabase = database;
	}

	/**
	 * Default constructor. Using this constructor will not allow caching and
	 * will generate the maths each time.
	 */
	public MathsContentManager() {
		cacheDatabase = null;
	}

	/**
	 * Get maths for the given latex string.
	 * 
	 * @param latexMathString
	 *            - the latex math string to convert into a png
	 * @return the string either from file or freshly generated.
	 * @throws SegueDatabaseException
	 *             - if there are an IO / database errors.
	 */
	public byte[] getMaths(final String latexMathString) throws SegueDatabaseException {
		if (null == this.cacheDatabase) {
			return this.generateMaths(latexMathString);
		}

		String cachedId = "maths-" + latexMathString.hashCode() + ".png";
		byte[] result = this.cacheDatabase.getById(cachedId);

		if (null == result) {
			// we can't find it so we have to generate it.
			byte[] imageToCache = this.generateMaths(latexMathString);
			this.cacheDatabase.save(cachedId, imageToCache);
			result = imageToCache;
		}

		return result;
	}

	/**
	 * Generate maths using the underlying library.
	 * 
	 * @param latexMathString
	 *            - the latex math string to convert into a png
	 * @return the image byte array.
	 * @throws SegueDatabaseException
	 *             - if there are an IO / database errors.
	 */
	private byte[] generateMaths(final String latexMathString) throws SegueDatabaseException {
		LatexMathToPngAdapter mathGen = new LatexMathToPngAdapter();
		BufferedImage image = mathGen.convertStringToPng(latexMathString, FONT_SIZE);
		byte[] imageInByte;
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(image, "png", baos);
			baos.flush();
			imageInByte = baos.toByteArray();
			baos.close();

		} catch (IOException e) {
			throw new SegueDatabaseException("Unable to generate maths: " + latexMathString, e);
		}

		return imageInByte;
	}
}
