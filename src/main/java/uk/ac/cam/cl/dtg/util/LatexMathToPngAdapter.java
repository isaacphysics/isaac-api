package uk.ac.cam.cl.dtg.util;

import java.awt.image.BufferedImage;
import javax.swing.JLabel;

import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.TeXFormula;
import org.scilab.forge.jlatexmath.TeXIcon;

/**
 * Helper class to convert math to a png.
 * 
 * @author Stephen Cummins
 */
public class LatexMathToPngAdapter {	
	
	/**
	 * Create an instance of the LatexMathToPngAdapter.
	 */
	public LatexMathToPngAdapter() {

	}
	
	/**
	 * Convert math string into png.
	 * 
	 * @param latexMathsString - to convert
	 * @param fontPointSize - font size to use for image.
	 * @return bufferedImage
	 */
	public BufferedImage convertStringToPng(final String latexMathsString, final int fontPointSize) {
		TeXFormula formula = new TeXFormula(latexMathsString);
		TeXIcon texIcon = formula.createTeXIcon(TeXConstants.STYLE_DISPLAY, fontPointSize);

		BufferedImage bufferedImage = new BufferedImage(texIcon.getIconWidth(), texIcon.getIconHeight(),
				BufferedImage.TYPE_4BYTE_ABGR);
		texIcon.paintIcon(new JLabel(), bufferedImage.getGraphics(), 0, 0);

		return bufferedImage;
	}
}
