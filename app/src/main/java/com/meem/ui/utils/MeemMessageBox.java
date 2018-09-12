package com.meem.ui.utils;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.meem.androidapp.MainActivity;
import com.meem.androidapp.R;
import com.meem.androidapp.UiContext;

public class MeemMessageBox extends Dialog implements View.OnClickListener {

    MainActivity mActivity;
    Dialog mDialog;

    UiContext mUiCtxt = UiContext.getInstance();

    TextView mMsgTextView;

    View middleLine;

    Button yesBtn, noBtn;
    Runnable mOnYes, mOnNo;

    public MeemMessageBox(MainActivity activity) {
        super(activity);
        mActivity = activity;

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        setContentView(R.layout.dialog);

        yesBtn = (Button) findViewById(R.id.btn_yes);
        yesBtn.setText(/*R.string.fine*/ "OK"); // TODO: Translation

		/*middleLine = findViewById(R.id.dialogDivider);
        middleLine.setVisibility(View.INVISIBLE);*/

        noBtn = (Button) findViewById(R.id.btn_no);
        noBtn.setVisibility(View.GONE);

        mMsgTextView = (TextView) findViewById(R.id.dlg_text);

        if (mMsgTextView != null) {
            /*mMsgTextView.setTypeface(mActivity.getmTypeFaces().get("DM"));*/
            mMsgTextView.setTextSize(17f);
            //			mMsgTextView.setTextColor(Color.rgb(183, 183, 183));
            mMsgTextView.setTextColor(Color.BLACK);
        }

        if (yesBtn != null) {
			/*yesBtn.setTypeface(mActivity.getmTypeFaces().get("DM"));*/
            yesBtn.setTextSize(17f);
            //			yesBtn.setTextColor(Color.rgb(183, 183, 183));
            yesBtn.setTextColor(Color.BLACK);
        }

        if (noBtn != null) {
			/*noBtn.setTypeface(mActivity.getmTypeFaces().get("DM"));*/
            noBtn.setTextSize(17f);
            //			noBtn.setTextColor(Color.rgb(183, 183, 183));
            noBtn.setTextColor(Color.BLACK);
        }

        yesBtn.setOnClickListener(this);

        setCanceledOnTouchOutside(false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void setMessage(String msg) {
        mMsgTextView.setText(msg);
    }

    public void setOnOK(Runnable okAction) {
        mOnYes = okAction;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_yes:
                if (null != mOnYes) {
                    mOnYes.run();
                }
                break;
            default:
                break;
        }

        dismiss();
    }
}
