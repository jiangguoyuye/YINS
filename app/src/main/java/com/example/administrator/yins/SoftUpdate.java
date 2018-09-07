package com.example.administrator.yins;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.json.JSONArray;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by fucm on 2016-07-06.
 */
public class SoftUpdate {
    private static final int DOWNLOAD = 1;
    private static final int DOWNLOAD_FINISH = 2;
    private int str_version;
    private String str_appname;
    private String str_downurl;
    private String mSavePath;
    private int progress;
    private boolean cancelUpdate = false;
    private Context mContext;
    private ProgressBar mProgress;
    private Dialog mDownloadDialog;

    public SoftUpdate(Context context)
    {
        this.mContext = context;
    }

    private Handler mHandler = new Handler()
    {
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case DOWNLOAD:
                    mProgress.setProgress(progress);
                    break;
                case DOWNLOAD_FINISH:
                    installApk();
                    break;
                default:
                    break;
            }
        };
    };

    /**
     *
     */
    public void checkUpdate()
    {
        if (isUpdate())
        {
            showNoticeDialog();
        } else
        {
            Toast.makeText(mContext, R.string.soft_update_no, Toast.LENGTH_LONG).show();
        }
    }

    /**
     *
     *
     * @return
     */
    private boolean isUpdate(){
        int versionCode = getVersionCode(mContext);
        try {
            str_version=versionCode+1;
            str_appname="YINS";
            str_downurl="http://192.168.16.13/YINSAPPService/DownLoad/YINS.apk";
            if (str_version > versionCode) {
                return true;
            }
        }
        catch (Exception e) {
            //strError=e.toString();
        }
        return false;
    }

    /**
     *
     *
     * @param context
     * @return
     */
    private int getVersionCode(Context context)
    {
        int versionCode = 0;
        try
        {
            versionCode = context.getPackageManager().getPackageInfo("com.example.administrator.yins", 0).versionCode;
        } catch (PackageManager.NameNotFoundException e)
        {
            e.printStackTrace();
        }
        return versionCode;
    }

    /**
     *
     */
    private void showNoticeDialog()
    {
        AlertDialog.Builder builder = new Builder(mContext);
        builder.setTitle(R.string.soft_update_title);
        builder.setMessage(R.string.soft_update_info);
        builder.setPositiveButton(R.string.soft_update_updatebtn, new OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
                showDownloadDialog();
            }
        });
        builder.setNegativeButton(R.string.soft_update_later, new OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
            }
        });
        Dialog noticeDialog = builder.create();
        noticeDialog.show();
    }

    /**
     *
     */
    private void showDownloadDialog()
    {
        AlertDialog.Builder builder = new Builder(mContext);
        builder.setTitle(R.string.soft_updating);
        final LayoutInflater inflater = LayoutInflater.from(mContext);
        View v = inflater.inflate(R.layout.soft_update, null);
        mProgress = (ProgressBar) v.findViewById(R.id.update_progress);
        builder.setView(v);
        builder.setNegativeButton(R.string.soft_update_cancel, new OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
                cancelUpdate = true;
            }
        });
        mDownloadDialog = builder.create();
        mDownloadDialog.show();
        downloadApk();
    }

    /**
     *
     */
    private void downloadApk()
    {
        new downloadApkThread().start();
    }

    /**
     *
     *
     * @author coolszy
     *@date 2012-4-26
     *@blog http://blog.92coding.com
     */
    private class downloadApkThread extends Thread
    {
        @Override
        public void run()
        {
            try
            {
                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
                {
                    String sdpath = Environment.getExternalStorageDirectory() + "/";
                    mSavePath = sdpath + "download";
                    URL url= new URL(str_downurl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.connect();
                    int length = conn.getContentLength();
                    InputStream is = conn.getInputStream();
                    File file = new File(mSavePath);
                    if (!file.exists())
                    {
                        file.mkdir();
                    }
                    File apkFile = new File(mSavePath, str_appname);
                    FileOutputStream fos = new FileOutputStream(apkFile);
                    int count = 0;
                    byte buf[] = new byte[1024];
                    do
                    {
                        int numread = is.read(buf);
                        count += numread;
                        progress = (int) (((float) count / length) * 100);
                        mHandler.sendEmptyMessage(DOWNLOAD);
                        if (numread <= 0)
                        {
                            mHandler.sendEmptyMessage(DOWNLOAD_FINISH);
                            break;
                        }
                        fos.write(buf, 0, numread);
                    } while (!cancelUpdate);
                    fos.close();
                    is.close();
                }
            } catch (MalformedURLException e)
            {
                e.printStackTrace();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
            mDownloadDialog.dismiss();
        }
    };

    /**
      *
      */
    private void installApk()
    {
        File apkfile = new File(mSavePath, str_appname);
        if (!apkfile.exists())
        {
            return;
        }
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setDataAndType(Uri.parse("file://" + apkfile.toString()), "application/vnd.android.package-archive");
        mContext.startActivity(i);
    }
}