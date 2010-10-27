package com.aptana.editor.css.formatter.preferences;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS
{
	private static final String BUNDLE_NAME = "com.aptana.editor.css.formatter.preferences.messages"; //$NON-NLS-1$
	public static String CSSFormatterBracesPage_blocks;
	public static String CSSFormatterBracesPage_braces_group_label;
	public static String CSSFormatterCommentsPage_comments_group_label;
	public static String CSSFormatterCommentsPage_enable_warpping;
	public static String CSSFormatterCommentsPage_max_line_width;
	public static String CSSFormatterControlStatementsPage_general_group_label;
	public static String CSSFormatterControlStatementsPage_indentation_size_group_option;
	public static String CSSFormatterControlStatementsPage_tab_policy_group_option;
	public static String CSSFormatterControlStatementsPage_tab_size_group_option;
	public static String CSSFormatterModifyDialog_braces_;
	public static String CSSFormatterModifyDialog_indentation_page_tab_name;
	public static String CSSFormatterModifyDialog_CSS_formater_title;
	static
	{
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages()
	{
	}
}