import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.qoppa.android.pdfProcess.PDFCanvas;
import com.qoppa.android.pdfProcess.PDFDocument;
import com.qoppa.android.pdfProcess.PDFPage;
import com.qoppa.android.pdfViewer.fonts.StandardFontTF;


import java.io.File;

public class ShowCroppedPictureActivity extends Activity {
    private static final String TAG = ShowCroppedPictureActivity.class.getSimpleName();
    /**
     * Path to read and write our images.
     */
    private static final String STORAGE_PATH = File.separator + "sdcard" + File.separator
            + Constants.GLOBAL_SD_DIRECTORY + File.separator + Constants.APP_SD_DIRECTORY
            + File.separator + Constants.SHARED_MEDIA_SD_FOLDER;
    /**
     * Button to take a new picture.
     */
    private Button mNewPictureButton;
    /**
     * Button to recrop the original image.
     */
    private Button mCropImageButton;
    /**
     * Button to send the cropped image by email.
     */
    private Button mNewMailButton;
    /**
     * Button to save the file to the shared media upload folder.
     */
    private Button mSaveSharedMedia;
    /**
     * Imageview on which we display the image.
     */
    private ImageView mImageView;
    /**
     * Bitmap to display
     */
    private Bitmap mBitmap;
    /**
     * Context of the application
     */
    private Context mContext;
    /**
     * File object of the image we get our of our Intent
     */
    private File mImgFile;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_crop);
        mContext = this;
        mImgFile = new File(this.getIntent().getStringExtra("imagePath"));
        if (mImgFile.exists()) {
            mBitmap = BitmapFactory.decodeFile(mImgFile.getAbsolutePath());
            mImageView = (ImageView) findViewById(R.id.image);
            mImageView.setImageBitmap(mBitmap);
        }
        mNewPictureButton = (Button) findViewById(R.id.newPictureButton);
        mCropImageButton = (Button) findViewById(R.id.newCropButton);
        mNewMailButton = (Button) findViewById(R.id.sendMailButton);
        mSaveSharedMedia = (Button) findViewById(R.id.sendSharedMedia);

        // set the button for taking a new picture
        mNewPictureButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mBitmap = null;
                mImageView.setImageBitmap(null);
                mImgFile.delete();
                NotificationManagerModule.getInstance().showToast(
                        getString(R.string.picture_deleted));
                Intent intent = new Intent(mContext, ScannerActivity.class);
                startActivity(intent);
            }
        });

        // set the button for cropping the original message again
        mCropImageButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mBitmap.recycle();
                Intent intent = new Intent(mContext, CropActivity.class);
                String path = mImgFile.getAbsolutePath().substring(0,
                        mImgFile.getAbsolutePath().length() - 12)
                        + mImgFile.getAbsolutePath().substring(
                                mImgFile.getAbsolutePath().length() - 4,
                                mImgFile.getAbsolutePath().length());

                AddApplication.getInstance().startActivity(
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("imgpath", path));
            }

        });

        // set the button for sending the picture by email
        mNewMailButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("message/rfc822");
                File pdfFile = getPdf(STORAGE_PATH + "/"
                        + mImgFile.getName().substring(0, mImgFile.getName().length() - 4) + ".pdf");
                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(pdfFile));
                // pdfFile.delete();
                try {
                    // try to start the emailActivity
                    startActivity(Intent.createChooser(intent, "Send mail..."));
                } catch (android.content.ActivityNotFoundException ex) {
                    NotificationManagerModule.getInstance().showToast(
                            "There are no email clients installed.");
                }
            }

        });

        mSaveSharedMedia.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder nameDialog = new AlertDialog.Builder(mContext);
                nameDialog.setIcon(android.R.drawable.ic_dialog_alert);
                nameDialog.setTitle(R.string.picture_dialog_title);
                final EditText input = new EditText(mContext);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                nameDialog.setView(input);
                nameDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                nameDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String fileName = input.getText().toString();
                        getPdf(STORAGE_PATH + File.separator
                                + SettingsManager.getFolderName(mContext) + File.separator
                                + "upload" + File.separator + fileName + ".pdf");
                        Intent intent = new Intent(mContext, ScannerActivity.class);
                        startActivity(intent);

                    }
                });
                nameDialog.show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        mBitmap.recycle();
        Intent intent = new Intent(mContext, CropActivity.class);
        String path = mImgFile.getAbsolutePath().substring(0,
                mImgFile.getAbsolutePath().length() - 12)
                + mImgFile.getAbsolutePath().substring(mImgFile.getAbsolutePath().length() - 4,
                        mImgFile.getAbsolutePath().length());

        AddApplication.getInstance().startActivity(
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("imgpath", path));
    }

    private File getPdf(String storePath) {
        PDFDocument pdf = null;

        try {
            // this static allows the sdk to access font assets,
            // it must be set prior to utilizing libraries
            StandardFontTF.mAssetMgr = getAssets();

            // Load a document and get the first page
            // pdf = new PDFDocument("/sdcard/input.pdf", null);
            pdf = new PDFDocument();
            PDFPage page = pdf.appendNewPage(mBitmap.getWidth(), mBitmap.getHeight());

            // the PDFCanvas object is used to draw to the page
            PDFCanvas canvas = page.createCanvas();

            // the matrix is used to size and position the bitmap on the page
            Matrix matrix = new Matrix();
            matrix.preTranslate(0, 0);// mBitmap.getHeight(), mBitmap.getWidth(
            matrix.preScale(1, 1);

            // add an image to the page
            canvas.drawBitmap(mBitmap, matrix, null);

            // save the document
            pdf.saveDocument(storePath);

        } catch (Exception e) {
            Log.e("error", Log.getStackTraceString(e));
        }
        File pdfFile = new File(storePath);
        return pdfFile;
    }
}
