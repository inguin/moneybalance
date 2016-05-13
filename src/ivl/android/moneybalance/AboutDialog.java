package ivl.android.moneybalance;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

class AboutDialog extends Dialog {

	public AboutDialog(Context context) {
		super(context);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about_dialog);
		setTitle(R.string.app_name);

		TextView info = (TextView) findViewById(R.id.about_info);
		info.setMovementMethod(LinkMovementMethod.getInstance());

		TextView icons = (TextView) findViewById(R.id.about_icons);
		icons.setMovementMethod(LinkMovementMethod.getInstance());

		TextView license = (TextView) findViewById(R.id.about_licence_copy);
		license.setMovementMethod(LinkMovementMethod.getInstance());
	}

}
