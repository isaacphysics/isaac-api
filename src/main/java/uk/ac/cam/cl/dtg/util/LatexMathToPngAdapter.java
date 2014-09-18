package uk.ac.cam.cl.dtg.util;

import java.awt.Color;
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
		final String withDefs = "\\newcommand{\\quantity}[2]{{#1}\\,{\\rm{#2}}}"
				              + "\\newcommand{\\valuedef}[3]{{#1}={\\quantity{#2}{#3}}}"
				              + "\\newcommand{\\vtr}[1]{\\underline{\\boldsymbol{#1}}}"
				              + "\\newcommand{\\d}{\\mathrm{d}}"
				              + "\\newcommand{\\vari}[1]{#1}"
				              + "\\newcommand{\\s}[1]{_{\\sf{#1}}}"
				              + "\\newcommand{\\half}{\\frac{1}{2}}"
				              + "\\newcommand{\\third}{\\frac{1}{3}}"
				              + "\\newcommand{\\quarter}{\\frac{1}{4}}"
				              + "\\newcommand{\\eighth}{\\frac{1}{8}}"
				              + "\\newcommand{\\e}{\\textrm{e}}"
				              + "\\newcommand{\\units}[1]{\\rm{#1}}"
	                          + latexMathsString;
		TeXFormula formula = new TeXFormula(withDefs);
		TeXIcon texIcon = formula.createTeXIcon(TeXConstants.STYLE_DISPLAY, fontPointSize);
		texIcon.setForeground(Color.WHITE);

		BufferedImage bufferedImage = new BufferedImage(texIcon.getIconWidth(), texIcon.getIconHeight(),
				BufferedImage.TYPE_4BYTE_ABGR);
		texIcon.paintIcon(new JLabel(), bufferedImage.getGraphics(), 0, 0);

		return bufferedImage;
	}
}
