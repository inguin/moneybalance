package ivl.android.moneybalance;

import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
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

        TextView versionNumberView = (TextView) findViewById(R.id.about_version_number);
        try {
            Context ctx = getContext();
            Resources res = ctx.getResources();
            PackageInfo packageInfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
            String versionName = packageInfo.versionName;
            Integer versionBuild = packageInfo.versionCode;
            String text = String.format(res.getString(R.string.about_version_number), res.getString(R.string.app_name), versionName, versionBuild);
            versionNumberView.setText(text);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }


	}

}
