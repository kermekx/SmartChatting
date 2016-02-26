package com.kermekx.smartchatting.commandes;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.TypedValue;
import android.widget.ImageView;

import com.kermekx.smartchatting.contact.Contact;
import com.kermekx.smartchatting.icon.IconManager;

/**
 * Created by kermekx on 23/02/2016.
 *
 * This task is used to load the user icon
 */
public class LoadIconTask extends AsyncTask<Void, Void, Boolean> {

    private final Context mContext;
    private final TaskListener mListener;
    private final String mUsername;
    private final int mSize;

    private Bitmap mIcon;

    public LoadIconTask(Context context, TaskListener listener, String contact, int size) {
        mContext = context;
        mListener = listener;
        mUsername = contact;
        mSize = size;
    }

    @Override
    protected Boolean doInBackground(Void... params) {

        mIcon = IconManager.getIcon(mContext, mUsername);

        if (mIcon != null)
            return true;

        return false;
    }

    @Override
    protected void onPostExecute(final Boolean success) {
        if (success) {
            float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, mSize, mContext.getResources().getDisplayMetrics());
            Bitmap bitmapResized = Bitmap.createScaledBitmap(mIcon, (int) px, (int) px, false);
            Drawable drawable = new BitmapDrawable(mContext.getResources(), bitmapResized);
            mListener.onData(new Drawable[] {drawable});
        }
    }

    @Override
    protected void onCancelled() {

    }

}