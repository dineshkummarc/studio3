package com.aptana.theme;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import com.aptana.scope.ScopeSelector;
import com.aptana.theme.internal.ThemeManager;

/**
 * Reads in the theme from a java properties file. Intentionally similar to the Textmate themes. keys are token types,
 * values are comma delimited with hex colors and font style keywords. First hex color becomes FG, second becomes BG (if
 * there).
 * 
 * @author cwilliams
 */
public class Theme
{

	static final String DELIMETER = ","; //$NON-NLS-1$

	private static final String UNDERLINE = "underline"; //$NON-NLS-1$
	private static final String BOLD = "bold"; //$NON-NLS-1$
	private static final String ITALIC = "italic"; //$NON-NLS-1$

	static final String THEME_NAME_PROP_KEY = "name"; //$NON-NLS-1$
	static final String FOREGROUND_PROP_KEY = "foreground"; //$NON-NLS-1$
	private static final String BACKGROUND_PROP_KEY = "background"; //$NON-NLS-1$
	private static final String SELECTION_PROP_KEY = "selection"; //$NON-NLS-1$
	private static final String LINE_HIGHLIGHT_PROP_KEY = "lineHighlight"; //$NON-NLS-1$
	private static final String CARET_PROP_KEY = "caret"; //$NON-NLS-1$

	private Map<ScopeSelector, DelayedTextAttribute> coloringRules;
	private ColorManager colorManager;
	private RGB defaultFG;
	private RGB lineHighlight;
	private RGB defaultBG;
	private RGB selection;
	private RGB caret;
	private String name;

	private RGB searchResultBG;

	/**
	 * Used for recursion in getDelayedTextAttribute to avoid matching same rule on scope twice
	 */
	private ScopeSelector lastSelectorMatch;

	public Theme(ColorManager colormanager, Properties props)
	{
		this.colorManager = colormanager;
		coloringRules = new HashMap<ScopeSelector, DelayedTextAttribute>();
		parseProps(props);
		storeDefaults();
	}

	private void parseProps(Properties props)
	{
		name = (String) props.remove(THEME_NAME_PROP_KEY);
		if (name == null)
			throw new IllegalStateException("Invalid theme properties!"); //$NON-NLS-1$
		// The general editor colors
		defaultFG = parseHexRGB((String) props.remove(FOREGROUND_PROP_KEY));
		defaultBG = parseHexRGB((String) props.remove(BACKGROUND_PROP_KEY));
		lineHighlight = parseHexRGB((String) props.remove(LINE_HIGHLIGHT_PROP_KEY), true);
		selection = parseHexRGB((String) props.remove(SELECTION_PROP_KEY), true);
		caret = parseHexRGB((String) props.remove(CARET_PROP_KEY), true);

		for (Entry<Object, Object> entry : props.entrySet())
		{
			String scopeSelector = (String) entry.getKey();
			int style = SWT.NORMAL;
			RGBa foreground = null;
			RGBa background = null;
			List<String> values = tokenize((String) entry.getValue());
			for (String token : values)
			{
				if (token.startsWith("#")) //$NON-NLS-1$
				{
					// it's a color!
					if (foreground == null)
						foreground = parseHexRGBa(token);
					else
						background = parseHexRGBa(token);
				}
				else
				{
					if (token.equalsIgnoreCase(ITALIC))
						style |= SWT.ITALIC;
					else if (token.equalsIgnoreCase(UNDERLINE))
						style |= TextAttribute.UNDERLINE;
					else if (token.equalsIgnoreCase(BOLD))
						style |= SWT.BOLD;
				}
			}
			if (foreground == null)
				foreground = new RGBa(defaultFG);
			DelayedTextAttribute attribute = new DelayedTextAttribute(foreground, background, style);
			coloringRules.put(new ScopeSelector(scopeSelector), attribute);
		}
	}

	private List<String> tokenize(String value)
	{
		List<String> tokens = new ArrayList<String>();
		if (!value.contains(DELIMETER))
		{
			tokens.add(value);
			return tokens;
		}
		StringTokenizer tokenizer = new StringTokenizer(value, ", "); //$NON-NLS-1$
		while (tokenizer.hasMoreTokens())
		{
			tokens.add(tokenizer.nextToken());
		}
		return tokens;
	}

	private RGB parseHexRGB(String hex)
	{
		return parseHexRGB(hex, false);
	}

	private RGB parseHexRGB(String hex, boolean alphaMergeWithBG)
	{
		if (hex == null)
			return new RGB(0, 0, 0);
		if (hex.length() != 7 && hex.length() != 9)
		{
			ThemePlugin.logError(MessageFormat.format("Received RGB Hex value with invalid length: {0}", hex), null); //$NON-NLS-1$
			if (defaultFG != null)
				return defaultFG;
			return new RGB(0, 0, 0);
		}
		String s = hex.substring(1, 3);
		int r = Integer.parseInt(s, 16);
		s = hex.substring(3, 5);
		int g = Integer.parseInt(s, 16);
		s = hex.substring(5, 7);
		int b = Integer.parseInt(s, 16);
		if (hex.length() == 9 && alphaMergeWithBG)
		{
			// Handle RGBa values by mixing against BG, etc
			s = hex.substring(7, 9);
			int a = Integer.parseInt(s, 16);
			return alphaBlend(defaultBG, new RGB(r, g, b), a);
		}
		return new RGB(r, g, b);
	}

	private RGBa parseHexRGBa(String hex)
	{
		if (hex == null)
			return new RGBa(0, 0, 0);
		if (hex.length() != 7 && hex.length() != 9)
		{
			ThemePlugin.logError(MessageFormat.format("Received RGBa Hex value with invalid length: {0}", hex), null); //$NON-NLS-1$
			if (defaultFG != null)
				return new RGBa(defaultFG);
			return new RGBa(0, 0, 0);
		}
		String s = hex.substring(1, 3);
		int r = Integer.parseInt(s, 16);
		s = hex.substring(3, 5);
		int g = Integer.parseInt(s, 16);
		s = hex.substring(5, 7);
		int b = Integer.parseInt(s, 16);
		int a = 255;
		if (hex.length() == 9)
		{
			s = hex.substring(7, 9);
			a = Integer.parseInt(s, 16);
		}
		return new RGBa(r, g, b, a);
	}

	public static RGB alphaBlend(RGB baseToBlendWith, RGB colorOnTop, int alpha)
	{
		int new_r = (baseToBlendWith.red * (255 - alpha) + colorOnTop.red * alpha) / 255;
		int new_g = (baseToBlendWith.green * (255 - alpha) + colorOnTop.green * alpha) / 255;
		int new_b = (baseToBlendWith.blue * (255 - alpha) + colorOnTop.blue * alpha) / 255;
		return new RGB(new_r, new_g, new_b);
	}

	public TextAttribute getTextAttribute(String scope)
	{
		lastSelectorMatch = null;
		return toTextAttribute(getDelayedTextAttribute(scope));
	}

	private DelayedTextAttribute getDelayedTextAttribute(String scope)
	{
		ScopeSelector match = findMatch(scope);
		if (match != null)
		{
			// This is to avoid matching the same selector multiple times when recursing up the scope! Basically our
			// match may have been many steps up our scope, not at the end!
			if (lastSelectorMatch != null && lastSelectorMatch.equals(match))
			{
				// We just matched the same rule! We need to recurse from parent scope!
				return getParent(scope);
			}
			lastSelectorMatch = match;
			DelayedTextAttribute attr = coloringRules.get(match);

			// if our coloring has no background, we should use parent's. If it has some opacity (alpha != 255), we
			// need to alpha blend
			if (attr.getBackground() == null || !attr.getBackground().isFullyOpaque())
			{
				// Need to merge bg color up the scope!
				DelayedTextAttribute parentAttr = getParent(scope);
				// Now do actual merge
				attr = merge(attr, parentAttr);
			}
			return attr;
		}

		// Some tokens are special. They have fallbacks even if not in the theme! Looks like bundles can contribute
		// them?
		if (scope.startsWith("markup.changed")) //$NON-NLS-1$
			return new DelayedTextAttribute(new RGBa(255, 255, 255), new RGBa(248, 205, 14), 0);

		if (scope.startsWith("markup.deleted")) //$NON-NLS-1$
			return new DelayedTextAttribute(new RGBa(255, 255, 255), new RGBa(255, 86, 77), 0);

		if (scope.startsWith("markup.inserted")) //$NON-NLS-1$
			return new DelayedTextAttribute(new RGBa(0, 0, 0), new RGBa(128, 250, 120), 0);

		if (scope.startsWith("meta.diff.index") || scope.startsWith("meta.diff.range") || scope.startsWith("meta.separator.diff")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			return new DelayedTextAttribute(new RGBa(255, 255, 255), new RGBa(65, 126, 218), SWT.ITALIC);

		if (scope.startsWith("meta.diff.header")) //$NON-NLS-1$
			return new DelayedTextAttribute(new RGBa(255, 255, 255), new RGBa(103, 154, 233), 0);

		return new DelayedTextAttribute(new RGBa(defaultFG));
	}

	protected DelayedTextAttribute getParent(String scope)
	{
		DelayedTextAttribute parentAttr = null;
		int index = scope.lastIndexOf(' ');
		if (index != -1)
		{
			String subType = scope.substring(0, index);
			parentAttr = getDelayedTextAttribute(subType);
		}
		if (parentAttr == null)
		{
			// If we never find a parent, use default bg
			parentAttr = new DelayedTextAttribute(new RGBa(defaultFG), new RGBa(defaultBG), 0);
		}
		return parentAttr;
	}

	private ScopeSelector findMatch(String scope)
	{
		return ScopeSelector.bestMatch(coloringRules.keySet(), scope);
	}

	private DelayedTextAttribute merge(DelayedTextAttribute childAttr, DelayedTextAttribute parentAttr)
	{
		// TODO Do we need to merge font style or FG?
		// Merge the bg up!
		RGBa bg = childAttr.getBackground();
		RGBa mergedBG = null;
		if (bg != null)
		{
			RGB bgRGB = bg.toRGB();
			if (!bg.isFullyOpaque())
			{
				bgRGB = alphaBlend(parentAttr.getBackground().toRGB(), bgRGB, bg.getAlpha());
			}
			mergedBG = new RGBa(bgRGB);
		}
		else
		{
			mergedBG = parentAttr.getBackground();
		}
		return new DelayedTextAttribute(childAttr.getForeground(), mergedBG, childAttr.getStyle());
	}

	private TextAttribute toTextAttribute(DelayedTextAttribute attr)
	{
		RGBa fg = attr.getForeground(); // TODO Do we ever need to handle FG alpha?!
		Color bgColor = null;
		RGBa bg = attr.getBackground();
		if (bg != null)
		{
			RGB bgRGB = bg.toRGB();
			if (!bg.isFullyOpaque())
			{
				bgRGB = alphaBlend(defaultBG, bgRGB, bg.getAlpha());
			}
			bgColor = colorManager.getColor(bgRGB);
		}
		return new TextAttribute(colorManager.getColor(fg.toRGB()), bgColor, attr.getStyle());
	}

	public RGB getBackground()
	{
		return defaultBG;
	}

	public RGB getSelection()
	{
		return selection;
	}

	public RGB getForeground()
	{
		return defaultFG;
	}

	public RGB getLineHighlight()
	{
		return lineHighlight;
	}

	public RGB getCaret()
	{
		return caret;
	}

	public String getName()
	{
		return name;
	}

	public Map<String, TextAttribute> getTokens()
	{
		Map<String, TextAttribute> tokens = new HashMap<String, TextAttribute>();
		for (Map.Entry<ScopeSelector, DelayedTextAttribute> entry : coloringRules.entrySet())
		{
			tokens.put(entry.getKey().toString(), toTextAttribute(entry.getValue()));
		}
		return tokens;
	}

	/**
	 * Updates the TextAttribute for a token and immediately saves the theme.
	 * TODO take in a ScopeSelector, not a String! 
	 * @param scopeSelector
	 * @param at
	 */
	public void update(String scopeSelector, TextAttribute at)
	{
		coloringRules.put(new ScopeSelector(scopeSelector), new DelayedTextAttribute(new RGBa(at.getForeground()
				.getRGB()), new RGBa(at.getBackground().getRGB()), at.getStyle()));
		save();
	}

	public Properties toProps()
	{
		Properties props = new Properties();
		props.put(THEME_NAME_PROP_KEY, getName());
		props.put(SELECTION_PROP_KEY, toHex(getSelection()));
		props.put(LINE_HIGHLIGHT_PROP_KEY, toHex(getLineHighlight()));
		props.put(FOREGROUND_PROP_KEY, toHex(getForeground()));
		props.put(BACKGROUND_PROP_KEY, toHex(getBackground()));
		props.put(CARET_PROP_KEY, toHex(caret));
		for (Map.Entry<ScopeSelector, DelayedTextAttribute> entry : coloringRules.entrySet())
		{
			if (entry.getKey() == null)
				continue;
			StringBuilder value = new StringBuilder();
			DelayedTextAttribute attr = entry.getValue();
			RGBa color = attr.getForeground();
			if (color != null)
			{
				value.append(toHex(color)).append(DELIMETER);
			}
			color = attr.getBackground();
			if (color != null)
			{
				value.append(toHex(color)).append(DELIMETER);
			}
			int style = attr.getStyle();
			if ((style & SWT.ITALIC) != 0)
			{
				value.append(ITALIC).append(DELIMETER);
			}
			if ((style & TextAttribute.UNDERLINE) != 0)
			{
				value.append(UNDERLINE).append(DELIMETER);
			}
			if ((style & SWT.BOLD) != 0)
			{
				value.append(BOLD).append(DELIMETER);
			}
			value.deleteCharAt(value.length() - 1);
			if (value.length() == 0)
				continue;
			props.put(entry.getKey().toString(), value.toString());
		}
		return props;
	}

	private String toHex(RGBa color)
	{
		String rgbString = toHex(color.toRGB());
		if (color.getAlpha() == 0)
		{
			return rgbString;
		}
		return rgbString + pad(Integer.toHexString(color.getAlpha()), 2, '0');
	}

	private String toHex(RGB rgb)
	{
		return MessageFormat.format("#{0}{1}{2}", pad(Integer.toHexString(rgb.red), 2, '0'), pad(Integer //$NON-NLS-1$
				.toHexString(rgb.green), 2, '0'), pad(Integer.toHexString(rgb.blue), 2, '0'));
	}

	private String pad(String string, int desiredLength, char padChar)
	{
		while (string.length() < desiredLength)
			string = padChar + string;
		return string;
	}

	protected void storeDefaults()
	{
		// Only save to defaults if it has never been saved there. Basically take a snapshot of first version and
		// use that as the "default"
		IEclipsePreferences prefs = new DefaultScope().getNode(ThemePlugin.PLUGIN_ID);
		if (prefs == null)
			return; // TODO Log something?
		Preferences preferences = prefs.node(ThemeManager.THEMES_NODE);
		if (preferences == null)
			return;
		String value = preferences.get(getName(), null);
		if (value == null)
		{
			save(new DefaultScope());
		}
	}

	public void save()
	{
		save(new InstanceScope());
		if (getThemeManager().getCurrentTheme().equals(this))
			getThemeManager().setCurrentTheme(this);
	}

	protected IThemeManager getThemeManager()
	{
		return ThemePlugin.getDefault().getThemeManager();
	}

	private void save(IScopeContext scope)
	{
		try
		{
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			toProps().storeToXML(os, null);
			IEclipsePreferences prefs = scope.getNode(ThemePlugin.PLUGIN_ID);
			Preferences preferences = prefs.node(ThemeManager.THEMES_NODE);
			preferences.put(getName(), os.toString());
			prefs.flush();
		}
		catch (Exception e)
		{
			ThemePlugin.logError(e);
		}
	}

	public void loadFromDefaults() throws InvalidPropertiesFormatException, UnsupportedEncodingException, IOException
	{
		IEclipsePreferences prefs = new DefaultScope().getNode(ThemePlugin.PLUGIN_ID);
		Preferences preferences = prefs.node(ThemeManager.THEMES_NODE);
		String xmlProps = preferences.get(getName(), null);
		if (xmlProps == null)
			return;
		Properties props = new Properties();
		props.loadFromXML(new ByteArrayInputStream(xmlProps.getBytes("UTF-8"))); //$NON-NLS-1$
		coloringRules.clear();
		parseProps(props);
		deleteCustomVersion();
	}

	/**
	 * Removes the saved instance version of theme.
	 */
	private void deleteCustomVersion()
	{
		delete(new InstanceScope());
	}

	private void deleteDefaultVersion()
	{
		delete(new DefaultScope());
	}

	private void delete(IScopeContext context)
	{
		try
		{
			IEclipsePreferences prefs = context.getNode(ThemePlugin.PLUGIN_ID);
			Preferences preferences = prefs.node(ThemeManager.THEMES_NODE);
			preferences.remove(getName());
			preferences.flush();
		}
		catch (BackingStoreException e)
		{
			ThemePlugin.logError(e);
		}
	}

	/**
	 * Removes a scope selector rule from the theme.
	 * TODO take in a ScopeSelector, not a String! 
	 * @param scopeSelector
	 */
	public void remove(String scopeSelector)
	{
		coloringRules.remove(new ScopeSelector(scopeSelector));
	}

	/**
	 * Adds a new token entry with no font styling, no bg, same FG as default for theme.
	 * TODO take in a ScopeSelector, not a String! 
	 */
	public void addNewDefaultToken(String scopeSelector)
	{
		DelayedTextAttribute attr = new DelayedTextAttribute(new RGBa(defaultFG));
		coloringRules.put(new ScopeSelector(scopeSelector), attr);
		save();
	}

	public void updateCaret(RGB newColor)
	{
		if (newColor == null)
			return;
		if (caret != null && caret.equals(newColor))
			return;
		caret = newColor;
		save();
	}

	public void updateFG(RGB newColor)
	{
		if (newColor == null)
			return;
		if (defaultFG != null && defaultFG.equals(newColor))
			return;
		defaultFG = newColor;
		save();
	}

	public void updateBG(RGB newColor)
	{
		if (newColor == null)
			return;
		if (defaultBG != null && defaultBG.equals(newColor))
			return;
		defaultBG = newColor;
		save();
	}

	public void updateLineHighlight(RGB newColor)
	{
		if (newColor == null)
			return;
		if (lineHighlight != null && lineHighlight.equals(newColor))
			return;
		lineHighlight = newColor;
		save();
	}

	public void updateSelection(RGB newColor)
	{
		if (newColor == null)
			return;
		if (selection != null && selection.equals(newColor))
			return;
		selection = newColor;
		save();
	}

	public Theme copy(String value)
	{
		if (value == null)
			return null;
		Properties props = toProps();
		props.setProperty(THEME_NAME_PROP_KEY, value);
		Theme newTheme = new Theme(colorManager, props);
		addTheme(newTheme);
		return newTheme;
	}

	protected void addTheme(Theme newTheme)
	{
		getThemeManager().addTheme(newTheme);
	}

	public void delete()
	{
		removeTheme();
		deleteCustomVersion();
		deleteDefaultVersion();
	}

	protected void removeTheme()
	{
		getThemeManager().removeTheme(this);
	}

	/**
	 * Determines if the theme defines this exact token type (not checking parents by dropping periods).
	 * 
	 * @param scopeSelector
	 * @return
	 */
	public boolean hasEntry(String scopeSelector)
	{
		return coloringRules.containsKey(scopeSelector);
	}

	public Color getForeground(String scope)
	{
		TextAttribute attr = getTextAttribute(scope);
		if (attr == null)
			return null;
		return attr.getForeground();
	}

	/**
	 * Returns the RGB value for the foreground of a specific token.
	 * 
	 * @param string
	 * @return
	 */
	public RGB getForegroundAsRGB(String scope)
	{
		Color fg = getForeground(scope);
		if (fg == null)
			return null;
		return fg.getRGB();
	}

	public Color getBackground(String scope)
	{
		TextAttribute attr = getTextAttribute(scope);
		if (attr == null)
		{
			return null;
		}
		return attr.getBackground();
	}

	/**
	 * Returns the RGB value for the background of a specific token.
	 * 
	 * @param string
	 * @return
	 */
	public RGB getBackgroundAsRGB(String scope)
	{
		Color bg = getBackground(scope);
		if (bg == null)
			return null;
		return bg.getRGB();
	}

	public RGB getSearchResultColor()
	{
		if (searchResultBG == null)
		{
			searchResultBG = isDark(getSelection()) ? lighten(getSelection()) : darken(getSelection());
		}
		return searchResultBG;
	}

	private RGB lighten(RGB color)
	{
		float[] hsb = color.getHSB();
		return new RGB(hsb[0], hsb[1], (float) (hsb[2] + 0.15));
	}

	private RGB darken(RGB color)
	{
		float[] hsb = color.getHSB();
		return new RGB(hsb[0], hsb[1], (float) (hsb[2] - 0.15));
	}

	public boolean hasDarkBG()
	{
		return isDark(getBackground());
	}

	public boolean hasLightFG()
	{
		return !isDark(getForeground());
	}

	private boolean isDark(RGB color)
	{
		// Convert to grayscale
		double grey = 0.3 * color.red + 0.59 * color.green + 0.11 * color.blue;
		return grey <= 128;
	}

}
