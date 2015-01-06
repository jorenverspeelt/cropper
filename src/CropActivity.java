
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class CropActivity extends Activity {
    private final static String TAG = CropActivity.class.getSimpleName();
    private final static String EXTENSION = ".jpg";
    private CropView mCropView;
    private BitmapDrawable mBitmapDrawable;
    private Button mCropButton;
    private Button mNewPictureButton;
    private Context mContext;
    private File mFile;
    private File mNewFile;
    private ProgressDialog mProgressDialog;
    /**
     * A list for splitted bitmaps, to divide between multiple AsyncTasks.
     */
    private ArrayList<Bitmap> mBitmaps;
    @SuppressWarnings("rawtypes")
    /**
     * A list of AsyncTasks, process the bitmaps in mBitmaps.
     */
    private ArrayList<AsyncTask> mTasks;
    /**
     * The number of available cores, determines the size of mTasks and mBitmaps.
     */
    private int mNumberOfCores;
    /**
     * The ImageFilter object for applying filters to an image.
     */
    private ImageFilters mFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop);
        mContext = this;
        mCropView = new CropView(this);
        mFile = new File(getIntent().getStringExtra("imgpath"));
        mBitmaps = new ArrayList<Bitmap>();
        mTasks = new ArrayList();
        mNumberOfCores = Runtime.getRuntime().availableProcessors();
        mProgressDialog = new ProgressDialog(mContext);
        mProgressDialog.setTitle("Enhancing image");
        mBitmapDrawable = null;
        mBitmapDrawable = (BitmapDrawable) Drawable.createFromPath(mFile.getAbsolutePath());

        mCropView = (CropView) findViewById(R.id.image_preview);
        mCropView.setImageDrawable(mBitmapDrawable);

        mCropButton = (Button) findViewById(R.id.button_crop);

        mCropButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                try {
                    // gets the cropped image and puts it in a bytearray.
                    byte[] mByteArray = mCropView.getCroppedImage(mBitmapDrawable,
                            mCropView.getWidth(), mCropView.getHeight());
                    FileOutputStream output = null;
                    try {
                        mNewFile = new File(mFile.getParentFile().getAbsolutePath()
                                + File.separator
                                + mFile.getName().substring(0, mFile.getName().length() - 4)
                                + "_cropped" + EXTENSION);
                        // creates the file to store the cropped image in
                        mNewFile.createNewFile();
                        if (mNewFile != null) {
                            // write the bytearray to the file
                            output = new FileOutputStream(mNewFile);
                            output.write(mByteArray);
                            Log.d(TAG, "file cropped and created");
                            // free the memory used by the bitmap
                            mBitmapDrawable.getBitmap().recycle();
                            // create a bitmap from the created file.
                            Bitmap mBitmap = BitmapFactory.decodeFile(mNewFile.getAbsolutePath());

                            mCropView.setImageBitmap(null);
                            mFilter = new ImageFilters();
                            int y;
                            // split the bitmap into the number of availabe cores
                            for (int i = 0; i < mNumberOfCores; i++) {
                                y = (mBitmap.getHeight() / mNumberOfCores) * i;
                                mBitmaps.add(Bitmap.createBitmap(mBitmap, 0, y, mBitmap.getWidth(),
                                        mBitmap.getHeight() / mNumberOfCores));
                                // assign a FilterImageTasks to each splitted bitmap
                                mTasks.add(new FilterImageTask().executeOnExecutor(
                                        AsyncTask.THREAD_POOL_EXECUTOR, mBitmaps.get(i)));
                            }
                            // show a dialog that the image is being processed
                            mProgressDialog
                                    .setMessage("Please wait while we enhance and crop your image");
                            mProgressDialog.show();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        // button that sends to user back to take a new picture
        mNewPictureButton = (Button) findViewById(R.id.button_new_picture);
        mNewPictureButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mCropView.setImageBitmap(null);
                mBitmapDrawable = null;
                mFile.delete();
                NotificationManagerModule.getInstance().showToast(
                        getString(R.string.picture_deleted));
                Intent intent = new Intent(mContext, ScannerActivity.class);
                startActivity(intent);
            }
        });
    }

    // sends the user back so he can take a picture
    @Override
    public void onBackPressed() {
        mCropView.setImageBitmap(null);
        mBitmapDrawable = null;
        mFile.delete();
        NotificationManagerModule.getInstance().showToast(getString(R.string.picture_deleted));
        Intent intent = new Intent(mContext, ScannerActivity.class);
        startActivity(intent);
    }

    public class FilterImageTask extends AsyncTask<Bitmap, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(Bitmap... params) {
            params[0] = mFilter.enhanceImage(params[0], 0, 3);
            return params[0];
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            // wait for all the partial bitmaps to complete their enhancement
            for (int i = 0; i < mTasks.size(); i++) {
                if (mTasks.get(i) == this) {
                    mBitmaps.set(i, result);
                }
            }
            for (AsyncTask task : mTasks) {
                if (task.getStatus() != AsyncTask.Status.FINISHED && task != this) {
                    return;
                }
            }

            // paste all the bitmaps back together in one big picture
            int height = 0;
            for (Bitmap map : mBitmaps) {
                height += map.getHeight();
            }
            Bitmap temp = Bitmap.createBitmap(mBitmaps.get(0).getWidth(), height,
                    Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(temp);
            int top = 0;
            for (int i = 0; i < mBitmaps.size(); i++) {
                top = (i == 0 ? 0 : top + mBitmaps.get(i).getHeight());
                canvas.drawBitmap(mBitmaps.get(i), 0f, top, null);
            }
            // sharpen the big bitmap
            new SharpenImageTask().executeOnExecutor(THREAD_POOL_EXECUTOR, temp);
            // refresh the dialog
            mProgressDialog.setMessage("Sharpening the image for you.");

        }
    }

    public class SharpenImageTask extends AsyncTask<Bitmap, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(Bitmap... params) {
            params[0] = mFilter.applySharpenEffect(params[0], 3);
            return params[0];
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            FileOutputStream mStream;
            try {
                mStream = new FileOutputStream(mNewFile);
                result.compress(Bitmap.CompressFormat.JPEG, 100, mStream);
            } catch (FileNotFoundException e) {
                Log.d(TAG, "Could not find file Exception: " + e.toString());
            }
            // dismiss the dialog
            mProgressDialog.dismiss();
            // show the user the cropped image by starting a new activity
            Intent intent = new Intent(mContext, ShowCroppedPictureActivity.class);
            intent.putExtra("imagePath", mNewFile.getAbsolutePath());
            startActivity(intent);
        }
    }
}
