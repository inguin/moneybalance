package ivl.android.moneybalance;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

public class AboutDialog extends Dialog {

	private static String INFO_HTML =
			"Copyright (C) 2012 Ingo van Lil<br>" +
			"<a href=\"https://github.com/inguin/moneybalance\">https://github.com/inguin/moneybalance</a><br><br>" +
			"Icons created by <a href=\"http://www.visualpharm.com\">VisualPharm</a>, " +
			"used under a <a href=\"http://creativecommons.org/licenses/by-nd/3.0/\">CC BY-ND 3.0</a> license.";

	private static String LICENSE_HTML =
			"Licensed under the Apache License, Version 2.0 (the \"License\"). " +
			"You may not use this program except in compliance with the License.<br>" +
			"You may obtain a copy of the License at " +
			"<a href=\"http://www.apache.org/licenses/LICENSE-2.0\">http://www.apache.org/licenses/LICENSE-2.0</a>.";

	public AboutDialog(Context context) {
		super(context);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about_dialog);
		setTitle(R.string.app_name);

		TextView info = (TextView) findViewById(R.id.about_info);
		info.setText(Html.fromHtml(INFO_HTML));
		info.setMovementMethod(LinkMovementMethod.getInstance());

		TextView license = (TextView) findViewById(R.id.about_license);
		license.setText(Html.fromHtml(LICENSE_HTML));
		license.setMovementMethod(LinkMovementMethod.getInstance());
	}

}
