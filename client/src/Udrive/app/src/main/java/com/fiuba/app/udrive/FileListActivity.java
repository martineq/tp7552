package com.fiuba.app.udrive;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import com.fiuba.app.udrive.model.Collaborator;
import com.fiuba.app.udrive.model.File;
import com.fiuba.app.udrive.model.FileInfo;
import com.fiuba.app.udrive.model.GenericResult;
import com.fiuba.app.udrive.model.ObjectStream;
import com.fiuba.app.udrive.model.StringTags;
import com.fiuba.app.udrive.model.Tag;
import com.fiuba.app.udrive.model.UserAccount;
import com.fiuba.app.udrive.model.UserLocation;
import com.fiuba.app.udrive.model.Util;
import com.fiuba.app.udrive.network.FileMetadataService;
import com.fiuba.app.udrive.network.FilesService;
import com.fiuba.app.udrive.network.ServiceCallback;
import com.fiuba.app.udrive.network.StatusCode;
import com.fiuba.app.udrive.network.UserService;
import com.fiuba.app.udrive.view.FileContextMenu;
import com.fiuba.app.udrive.view.FileContextMenuManager;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.nononsenseapps.filepicker.FilePickerActivity;

import java.io.FileOutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class FileListActivity extends AppCompatActivity implements
        FilesArrayAdapter.OnContextMenuClickListener, AdapterView.OnItemClickListener,
        FileContextMenu.OnFileContextMenuItemClickListener, GoogleApiClient.ConnectionCallbacks,
        FileContextMenu.OnFileContextMenuItemClickButtonVisibilityListener {

    public static final String TAG = "FileListActivity";

    private List<File> mFiles = new ArrayList<File>();

    private FilesArrayAdapter mFilesAdapter;

    private ProgressDialog mProgressDialog;

    private FilesService mFilesService;

    private UserService mUserService = null;

    private FileMetadataService mFileMetadataService = null;

    private UserAccount mUserAccount;

    private Integer mDirId;

    private Integer mFileId;

    private File mActualFile;

    private File selectedFileForDownload;

    public static final int FILE_CODE = 1;

    public static final int DIR_CODE = 2;

    public static final String EXTRA_USER_ACCOUNT = "userAccount";

    public static final String EXTRA_DIR_ID = "dirId";

    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation = null;
    private double mLatitude;
    private double mLongitude;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_list);
        mUserAccount = (UserAccount) getIntent().getSerializableExtra(EXTRA_USER_ACCOUNT);
        mDirId = (Integer) getIntent().getSerializableExtra(EXTRA_DIR_ID);
        Log.d(TAG, "TOKEN: " + mUserAccount.getToken());
        mFilesAdapter = new FilesArrayAdapter(this, R.layout.file_list_item, mFiles, this);
        ListView list = (ListView)findViewById(R.id.fileListView);
        list.setAdapter(mFilesAdapter);
        list.setOnItemClickListener(this);
        mFilesService = new FilesService(mUserAccount.getToken(), FileListActivity.this);
        mUserService = new UserService(mUserAccount.getToken(), FileListActivity.this);
        mFileMetadataService = new FileMetadataService(mUserAccount.getToken(), FileListActivity.this);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .build();
        mGoogleApiClient.connect();


        System.out.println("idDir: "+mDirId);
        if (mDirId == null)
            mDirId = 0;
        loadFiles(mUserAccount.getUserId(), mDirId); // Change 0 to the corresponding dirId
    }
    @Override
    public void onConnected(Bundle connectionHint) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            mLatitude = mLastLocation.getLatitude();
            mLongitude = mLastLocation.getLongitude();
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        mLatitude = 0.0;
        mLongitude = 0.0;
        Toast.makeText(FileListActivity.this, R.string.error_gps, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_file_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        // Action of signing out
        if (id == R.id.action_signout) {
            // Stop GPS service
            if (mGoogleApiClient.isConnected())
                mGoogleApiClient.disconnect();
            ObjectStream<UserAccount> os = new ObjectStream<>(MainActivity.getAccountFilename(), FileListActivity.this);
            os.delete();
            Intent main = new Intent(getApplicationContext(), MainActivity.class);
            main.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(main);
            finishAffinity();

        }  else if (id == R.id.action_profile) {
            Intent profile = new Intent(FileListActivity.this, UserProfileActivity.class);
            profile.putExtra("userAccount", mUserAccount);
            startActivity(profile);

        }  else if (id == R.id.action_upload_file) {
            Intent i = new Intent(this, FilePickerActivity.class);
            i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
            i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
            i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);
            i.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());

            startActivityForResult(i, FILE_CODE);

        } else if (id == R.id.action_add_folder) {
            LayoutInflater li = LayoutInflater.from(this);
            View newFolderView = li.inflate(R.layout.new_folder, null);

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setView(newFolderView);

            final EditText userInput = (EditText) newFolderView.findViewById(R.id.editTextDialogUserInput);
            String ok_option = getString(R.string.alert_ok);
            String cancel_option = getString(R.string.alert_cancel);

            // set dialog message
            alertDialogBuilder.setCancelable(false).setPositiveButton(ok_option, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,int id) {
                    // get user input and set it to result
                    // edit text
                    String nameFolder = userInput.getText().toString();
                    mFilesService.addFolder(mUserAccount.getUserId(), mDirId, nameFolder, new ServiceCallback<List<File>>() {
                        @Override
                        public void onSuccess(List<File> files, int status) {
                            mFilesAdapter.updateFiles(files);
                            Log.d(TAG, "Number of files received " + files.size());
                            // Update user location
                            UserLocation userLocation = new UserLocation(mLatitude, mLongitude);
                            mUserService.updateUserLocation(mUserAccount.getUserId(), userLocation, new ServiceCallback<GenericResult>() {
                                @Override
                                public void onSuccess(GenericResult object, int status) {
                                    // Does nothing. Transparent for the user
                                }

                                @Override
                                public void onFailure(String message, int status) {
                                    // Does nothing. Transparent for the user
                                }
                            });
                        }

                        @Override
                        public void onFailure(String message, int status) {
                            if (StatusCode.isHumanReadable(status)) {
                                message = StatusCode.getMessage(FileListActivity.this, status);
                                Toast.makeText(FileListActivity.this, message, Toast.LENGTH_LONG).show();
                            }
                            Log.e(TAG, message);
                        }
                    });

                }
            });
            alertDialogBuilder.setNegativeButton(cancel_option, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });

            // create alert dialog
            AlertDialog alertDialog = alertDialogBuilder.create();

            // show it
            alertDialog.show();

        } else if (id == R.id.action_show_trash) {
            Intent i = new Intent(this, TrashActivity.class);
            i.putExtra(TrashActivity.EXTRA_USER_ACCOUNT, mUserAccount);
            startActivity(i);
        } else if (id == R.id.action_file_search) {
            launchFileSearch();
        }

        return super.onOptionsItemSelected(item);
    }

    private void loadFiles(int userId, int dirId){
        final ProgressDialog progressDialog = ProgressDialog.show(this, null, getString(R.string.loading), true);
        progressDialog.setCancelable(false);

        mFilesService.getFiles(userId, dirId, new ServiceCallback<List<File>>() {
            @Override
            public void onSuccess(List<File> files, int status) {
                mFilesAdapter.updateFiles(files);
                progressDialog.dismiss();
                Log.d(TAG, "Number of files received " + files.size());
            }

            @Override
            public void onFailure(String message, int status) {
                if (StatusCode.isHumanReadable(status)) {
                    message = StatusCode.getMessage(FileListActivity.this, status);
                    Toast.makeText(FileListActivity.this, message, Toast.LENGTH_LONG).show();
                }
                progressDialog.dismiss();
                Log.e(TAG, message);
            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.d(TAG, "Click--> position " + position);
        File actualFile = mFiles.get(position);
        if (actualFile.isDir()){
            Intent mNextIntent = new Intent(this, FileListActivity.class);
            mNextIntent.putExtra(this.EXTRA_USER_ACCOUNT, mUserAccount);
            mNextIntent.putExtra(this.EXTRA_DIR_ID, actualFile.getId());
            startActivity(mNextIntent);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == FILE_CODE && resultCode == Activity.RESULT_OK) {
            uploadSelectedFile(data);
        } else if (requestCode == DIR_CODE && resultCode == Activity.RESULT_OK) {
            downloadFileIntoSelectedDir(data);
        }
    }

    private void downloadFileIntoSelectedDir(Intent data) {
        final File selectedFileForDownload = getSelectedFileForDownload();
        if (selectedFileForDownload != null) {
            final String fullPath = data.getData().getPath() + "/" + selectedFileForDownload.getName();

            ServiceCallback<FileOutputStream> callback = new ServiceCallback<FileOutputStream>() {
                @Override
                public void onSuccess(FileOutputStream outputStream, int status) {
                    Log.i(TAG, "File downloaded to " + fullPath);
                    setSelectedFileForDownload(null);
                }

                @Override
                public void onFailure(String message, int status) {
                    Log.e(TAG, message);
                    setSelectedFileForDownload(null);
                }
            };

            if (selectedFileForDownload.isFile()) {
                mFilesService.downloadFile(mUserAccount.getUserId(), selectedFileForDownload.getId(),
                        selectedFileForDownload.getLastVersion(), fullPath, callback);
            } else {
                mFilesService.downloadDir(mUserAccount.getUserId(), selectedFileForDownload.getId(), fullPath, callback);
            }
        }
    }

    private void uploadSelectedFile(Intent data) {
        Uri uri = data.getData();
        //Toast.makeText(this, uri.getPath(), Toast.LENGTH_LONG).show();
        mFilesService.upload(mUserAccount.getUserId(), mDirId, uri.getPath(), new ServiceCallback<List<File>>() {
            @Override
            public void onSuccess(List<File> files, int status) {
                mFilesAdapter.updateFiles(files);
                Log.d(TAG, "Number of files received " + files.size());
                // Update user location
                UserLocation userLocation = new UserLocation(mLatitude, mLongitude);
                mUserService.updateUserLocation(mUserAccount.getUserId(), userLocation, new ServiceCallback<GenericResult>() {
                    @Override
                    public void onSuccess(GenericResult object, int status) {
                        // Does nothing. Transparent for the user
                    }

                    @Override
                    public void onFailure(String message, int status) {
                        // Does nothing. Transparent for the user
                    }
                });
            }

            @Override
            public void onFailure(String message, int status) {
                if (StatusCode.isHumanReadable(status)) {
                    message = StatusCode.getMessage(FileListActivity.this, status);
                    Toast.makeText(FileListActivity.this, message, Toast.LENGTH_LONG).show();
                }
                Log.e(TAG, message);
            }
        });
    }

    @Override
    public void onDownloadClick(int FileItem) {
        FileContextMenuManager.getInstance().hideContextMenu();
        setSelectedFileForDownload(mFiles.get(FileItem));
        Intent i = new Intent(this, FilePickerActivity.class);
        i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
        i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
        i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_DIR);
        i.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());

        startActivityForResult(i, DIR_CODE);
        FileContextMenuManager.getInstance().hideContextMenu();
    }

    @Override
    public void onDownloadPrevClick(int FileItem) {
        FileContextMenuManager.getInstance().hideContextMenu();
        if (mFiles.get(FileItem).getLastVersion()<=1){
            Toast.makeText(FileListActivity.this, "No other versions found!", Toast.LENGTH_SHORT).show();
        } else {
            ArrayList<String> versions = getFileVersions(mFiles.get(FileItem).getLastVersion());
            LayoutInflater inflater = getLayoutInflater();
            final View layout = inflater.inflate(R.layout.prev_version_layout, null);
            Spinner spinner = (Spinner)layout.findViewById(R.id.spinner_prev);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
            spinner.setAdapter(adapter);
            for (int i = 0; i < versions.size(); i++)
                adapter.add(versions.get(i));
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(layout);
            builder.setIcon(R.drawable.ic_down_18);
            builder.setCancelable(false)
                    .setTitle(R.string.version)
                    .setPositiveButton(getString(R.string.prev_download), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                        }
                    })
                    .setNegativeButton(getString(R.string.settings_cancel), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
        }
        /*setSelectedFileForDownload(mFiles.get(FileItem));
        Intent i = new Intent(this, FilePickerActivity.class);
        i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
        i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
        i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_DIR);
        i.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());

        startActivityForResult(i, DIR_CODE);
        FileContextMenuManager.getInstance().hideContextMenu();*/
    }

    @Override
    public void onInformationClick(int FileItem) {
        Log.i(TAG, "Information File position " + FileItem);
        /*System.out.println("Position - Lat >>>> "+getLatitude()+
                "Position - Long >>>> "+getLongitude());*/
        String type = mFiles.get(FileItem).isDir() ? "dir" : "file";
        mFileMetadataService.getFileInfo(mUserAccount.getUserId(), type, mFiles.get(FileItem).getId(), new ServiceCallback<FileInfo>() {
            @Override
            public void onSuccess(FileInfo object, int status) {
                Intent infoIntent = new Intent(FileListActivity.this, FileInfoActivity.class);
                infoIntent.putExtra("fileInfo", object);
                infoIntent.putExtra("token", mUserAccount.getToken());
                infoIntent.putExtra("userId", mUserAccount.getUserId());
                startActivity(infoIntent);
            }

            @Override
            public void onFailure(String message, int status) {
                Toast.makeText(FileListActivity.this, getString(R.string.error_fileinfo), Toast.LENGTH_LONG).show();
            }
        });

    }

    @Override
    public void onShareClick(int FileItem) {
        Log.i(TAG, "Share File position " + FileItem);
        Integer idFile = mFiles.get(FileItem).getId();
        Intent shareIntent = new Intent(FileListActivity.this, ShareActivity.class);
        shareIntent.putExtra(ShareActivity.EXTRA_USER_ACCOUNT, mUserAccount);
        shareIntent.putExtra(ShareActivity.EXTRA_FILE_ID, idFile);
        startActivity(shareIntent);
        FileContextMenuManager.getInstance().hideContextMenu();
    }

    @Override
    public void onTagClick(final int FileItem) {
        Log.i(TAG, "FileTag File position " + FileItem);
        String name = mFiles.get(FileItem).getName();
        String type = mFiles.get(FileItem).isDir()?getString(R.string.dir_type):getString(R.string.file_type);
        LayoutInflater inflater = getLayoutInflater();
        final View layout = inflater.inflate(R.layout.file_tag_layout, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(layout);
        builder.setIcon(R.drawable.ic_tag);
        ((TextView)layout.findViewById(R.id.file_name)).setText(Html.fromHtml(type));
        ((TextView)layout.findViewById(R.id.file_name)).append(": " + name);

        final ArrayList<Tag> tagList = new ArrayList<Tag>();

        final WebView panel = ((WebView) layout.findViewById(R.id.tagView));
        WebSettings webSettings = panel.getSettings();
        webSettings.setJavaScriptEnabled(true);
        panel.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void removeTag(final int tagIndex) {
                Log.i(TAG, "Removing tag >>>>> " + tagList.get(tagIndex).getTagName());
                tagList.remove(tagIndex);
                updatePanel(panel, tagList);
            }
        }, "Android");
        // Get tags from server
        type = mFiles.get(FileItem).isDir()?"dir":"file";
        mFileMetadataService.getTags(mUserAccount.getUserId(), type, mFiles.get(FileItem).getId(), new ServiceCallback<StringTags>() {
            @Override
            public void onSuccess(StringTags object, int status) {
                if (object.getTags() != "") {
                    ArrayList<Tag> tags = Util.stringToTagsArray(object.getTags());
                    int i;
                    for (i = 0; i < tags.size(); i++) {
                        tagList.add(tags.get(i));
                    }
                    if (tags.size() > 0) {
                        updatePanel(panel, tagList);
                    }
                }

            }

            @Override
            public void onFailure(String message, int status) {
                // Do something
            }
        });
        layout.findViewById(R.id.button_add_tag).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                TextView input = ((TextView) layout.findViewById(R.id.tag_input));
                String newTag = input.getText().toString();
                ArrayList<String> wrong = new ArrayList<String>();
                wrong.add("");
                wrong.add(" ");
                wrong.add(null);

                int i;
                boolean tagExists = false;
                if (tagList.size() > 0) {
                    // Check tag was not repeated in list
                    for (i = 0; i < tagList.size(); i++) {
                        if (tagList.get(i).getTagName().toLowerCase().compareTo(newTag.toLowerCase()) == 0) {
                            tagExists = true;
                            break;
                        }
                    }
                    if (!tagExists)
                        if (!Util.matchString(newTag, wrong))
                            tagList.add(new Tag(newTag));
                } else { // Tag list is empty so add the tag directly
                    if (!Util.matchString(newTag, wrong))
                        tagList.add(new Tag(newTag));
                }
                updatePanel(panel, tagList);
                input.setText("");
            }
        });

        builder.setCancelable(false)
                .setTitle(getString(R.string.file_tag_title))
                .setPositiveButton(getString(R.string.save_changes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        final ProgressDialog progressDialog = ProgressDialog.show(layout.getContext(), null, getString(R.string.loading), true);
                        progressDialog.setCancelable(false);
                        // Send tag list to be updated on the server
                        final StringTags tags = new StringTags(Util.tagsToString(tagList));
                        String type = mFiles.get(FileItem).isDir()?"dir":"file";
                        mFileMetadataService.updateFileTags(mUserAccount.getUserId(), type, mFiles.get(FileItem).getId(), tags, new ServiceCallback<GenericResult>() {
                            @Override
                            public void onSuccess(GenericResult object, int status) {
                                if (object.getResultCode() != 1)
                                    Toast.makeText(FileListActivity.this, getString(R.string.error_savetags), Toast.LENGTH_LONG).show();
                                else
                                    Toast.makeText(FileListActivity.this, getString(R.string.ok_savetags), Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void onFailure(String message, int status) {
                                progressDialog.dismiss();
                                if (StatusCode.isHumanReadable(status)) {
                                    message = StatusCode.getMessage(FileListActivity.this, status);
                                    Toast.makeText(FileListActivity.this, message, Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                        progressDialog.dismiss();
                    }
                })
                .setNegativeButton(getString(R.string.settings_cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        alert.show();
        FileContextMenuManager.getInstance().hideContextMenu();
    }


    public void updatePanel(final WebView panel, final ArrayList<Tag> tagList) {
        FileListActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                panel.loadDataWithBaseURL("file:///android_asset/",
                        getPanelHTML(tagList), "text/html", "utf-8", null);
            }
        });
    }


    private String getPanelHTML(ArrayList<Tag> tagList){
        String html = "<script type=\"text/javascript\">" +
                "    function callRemoveTag(tagIndex) {" +
                "        Android.removeTag(tagIndex);" +
                "    }" +
                "</script>" +
                "<html>" +
                "   <body style=\"text-align:center;\">";
        int index;
        String tagCSS = "style=\"font-size:11pt; padding:4px; border:1px solid black; background-color:#f19c47" +
                "; border-radius:2px; font-family:monospace; margin-top:4px; margin-bottom:4px; display:inline-block; " +
                "text-align:center\"";
        for (index = 0; index < tagList.size(); index++) {
            System.out.println("TagName >>>> " + tagList.get(index).getTagName());
            html = html + "<span " + tagCSS + " onClick=\"callRemoveTag(" + index + ");\">" +
                    tagList.get(index).getTagName() + "<img src=\"ic_close_black_18dp.png\" width=\"12px\" /></span>  ";
        }
        html = html + "</body></html>";
        return html;
    }

    @Override
    public void onDeleteClick(int FileItem) {
        FileContextMenuManager.getInstance().hideContextMenu();
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        String title = getString(R.string.delete_option_title);
        String message = getString(R.string.delete_option_message);
        String ok_option = getString(R.string.alert_ok);
        String cancel_option = getString(R.string.alert_cancel);
        alertDialogBuilder.setTitle(title).setMessage(message);
        mActualFile = mFiles.get(FileItem);
        mFileId = mActualFile.getId();

        alertDialogBuilder.setCancelable(false).setPositiveButton(ok_option, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int id) {
                if (mActualFile.isDir()){
                    deleteDirectory();
                }else{
                    deleteFile();
                }

            }
        });
        alertDialogBuilder.setNegativeButton(cancel_option, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
        FileContextMenuManager.getInstance().hideContextMenu();
    }

    private void deleteDirectory(){
        mFilesService.deleteDirectory(mUserAccount.getUserId(), mFileId, new ServiceCallback<List<File>>() {
            @Override
            public void onSuccess(List<File> files, int status) {
                mFilesAdapter.updateFiles(files);
                Log.d(TAG, "Number of files received " + files.size());
            }

            @Override
            public void onFailure(String message, int status) {
                if (StatusCode.isHumanReadable(status)) {
                    message = StatusCode.getMessage(FileListActivity.this, status);
                    Toast.makeText(FileListActivity.this, message, Toast.LENGTH_LONG).show();
                }
                Log.e(TAG, message);
            }
        });
    }

    private void deleteFile(){
        mFilesService.deleteFile(mUserAccount.getUserId(), mFileId, new ServiceCallback<List<File>>() {
            @Override
            public void onSuccess(List<File> files, int status) {
                mFilesAdapter.updateFiles(files);
                Log.d(TAG, "Number of files received " + files.size());
            }

            @Override
            public void onFailure(String message, int status) {
                if (StatusCode.isHumanReadable(status)) {
                    message = StatusCode.getMessage(FileListActivity.this, status);
                    Toast.makeText(FileListActivity.this, message, Toast.LENGTH_LONG).show();
                }
                Log.e(TAG, message);
            }
        });
    }

    @Override
    public void onCancelClick(int FileItem) {

    }

    @Override
    public void onClick(View v, int position) {
        FileContextMenuManager.getInstance().toggleContextMenuFromView(v, position, this, this);
    }


    public File getSelectedFileForDownload() {
        return selectedFileForDownload;
    }

    public void setSelectedFileForDownload(File selectedFileForDownload) {
        this.selectedFileForDownload = selectedFileForDownload;
    }

    private void launchFileSearch(){
        LayoutInflater inflater = getLayoutInflater();
        final View layout = inflater.inflate(R.layout.filesearch_custom_dialog, null);
        AlertDialog.Builder dBuilder = new AlertDialog.Builder(this);
        dBuilder.setTitle(R.string.action_file_search);
        dBuilder.setView(layout);

        // get our tabHost from the xml
        final TabHost tabHost = (TabHost)layout.findViewById(R.id.TabHost01);
        tabHost.setup();
        // create tab 1
        TabHost.TabSpec spec1 = tabHost.newTabSpec("tab1");
        spec1.setIndicator(getTabIndicator(tabHost.getContext(), R.string.by_name, R.drawable.ic_alphabetical_24));
        spec1.setContent(R.id.layout1);
        tabHost.addTab(spec1);

        //create tab2
        TabHost.TabSpec spec2 = tabHost.newTabSpec("tab2");
        spec2.setIndicator(getTabIndicator(tabHost.getContext(), R.string.by_ext, R.drawable.ic_regex_24));
        spec2.setContent(R.id.layout2);
        tabHost.addTab(spec2);

        //create tab3
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        final Spinner spinner_tab3 = (Spinner)layout.findViewById(R.id.spinner_tab3);
        spinner_tab3.setAdapter(adapter);
        // Fill tags spinner with the user tags coming from server
        mFileMetadataService.getUserTags(mUserAccount.getUserId(), new ServiceCallback<StringTags>() {
            @Override
            public void onSuccess(StringTags object, int status) {
                if (object.getTags() != "") {
                    ArrayList<Tag> tags = Util.stringToTagsArray(object.getTags());
                    for (int i = 0; i < tags.size(); i++) {
                        adapter.add(tags.get(i).getTagName());
                    }
                }
            }
            @Override
            public void onFailure(String message, int status) {
                // Do nothing
            }
        });
        TabHost.TabSpec spec3 = tabHost.newTabSpec("tab3");
        spec3.setIndicator(getTabIndicator(tabHost.getContext(), R.string.by_tag, R.drawable.ic_tag_multiple_24));
        spec3.setContent(R.id.layout3);
        tabHost.addTab(spec3);

        //create tab4
        final ArrayList<String> names = new ArrayList<>();
        final ArrayList<String> mails = new ArrayList<>();
        final ArrayList<Collaborator> collab = new ArrayList<>();
        // Fill owners spinner with the owners coming from server
        mUserService.getOwners(mUserAccount.getUserId(), new ServiceCallback<List<Collaborator>>() {
            @Override
            public void onSuccess(List<Collaborator> coll, int status) {
                for (int i = 0; i < coll.size(); i++) {
                    names.add(coll.get(i).getFirstName() + " " + coll.get(i).getLastName());
                    mails.add(coll.get(i).getEmail());
                    collab.add(coll.get(i));
                }
            }

            @Override
            public void onFailure(String message, int status) {
                // Do nothing
            }
        });

        // TODO: delete after testing
        /*************/
        /*collab.add(new Collaborator("Name1", "Name1", "name1@name1.com"));
        collab.add(new Collaborator("Name2", "Name2", "name2@name2.com"));
        collab.add(new Collaborator("Name3", "Name3", "name3@name3.com"));
        for (int i = 0; i < collab.size(); i++){
            names.add(collab.get(i).getFirstName()+" "+collab.get(i).getLastName());
            mails.add(collab.get(i).getEmail());
        }*/
        /*************/
        final CollaboratorsListAdapter ownerAdapter = new CollaboratorsListAdapter(this, android.R.layout.simple_spinner_item,
                R.layout.file_info_item, names, mails);
        ownerAdapter.setDropDownViewResource(R.layout.file_info_item);
        final Spinner spinner_tab4 = (Spinner)layout.findViewById(R.id.spinner_tab4);
        spinner_tab4.setAdapter(ownerAdapter);
        for(int i = 0; i < collab.size(); i++) {
            collab.get(i).setId(i);
            ownerAdapter.add(collab.get(i));
        }
        TabHost.TabSpec spec4 = tabHost.newTabSpec("tab4");
        spec4.setIndicator(getTabIndicator(tabHost.getContext(), R.string.by_owner, R.drawable.ic_account_24));
        spec4.setContent(R.id.layout4);
        tabHost.addTab(spec4);

        dBuilder.setCancelable(false)
                .setIcon(R.drawable.ic_magnify)
                .setPositiveButton(getString(R.string.search), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        final ProgressDialog progressDialog = ProgressDialog.show(layout.getContext(),
                                null, getString(R.string.searching), true);
                        progressDialog.setCancelable(false);
                        // launch this activity with the result of the request
                        if (tabHost.getCurrentTab() == 2) { // Search by tag
                            System.out.println("Selected item >>>> " + spinner_tab3.getSelectedItem());
                            mFilesService.getFilesByTag(mUserAccount.getUserId(), spinner_tab3.getSelectedItem().toString().toLowerCase(),
                                    new ServiceCallback<List<File>>() {
                                        @Override
                                        public void onSuccess(List<File> files, int status) {
                                            if (files.size() > 0) {
                                                progressDialog.dismiss();
                                                mFilesAdapter.updateFiles(files);
                                                Log.d(TAG, "Number of files received " + files.size());
                                            } else {
                                                // Show no results msg
                                                progressDialog.dismiss();
                                                Intent emptySearch = new Intent(FileListActivity.this,
                                                        FileSearchEmptyActivity.class);
                                                startActivity(emptySearch);
                                            }
                                        }
                                        @Override
                                        public void onFailure(String message, int status) {
                                            progressDialog.dismiss();
                                        }
                                    });
                        } else if (tabHost.getCurrentTab() == 0) { // Search by Name
                            String name = ((EditText) layout.findViewById(R.id.edit_tab1)).getText().toString().toLowerCase();
                            if (name.compareTo("") != 0) {
                                mFilesService.getFilesByName(mUserAccount.getUserId(), name, new ServiceCallback<List<File>>() {
                                    @Override
                                    public void onSuccess(List<File> files, int status) {
                                        if (files.size() > 0) {
                                            progressDialog.dismiss();
                                            mFilesAdapter.updateFiles(files);
                                            Log.d(TAG, "Number of files received " + files.size());
                                        } else {
                                            // Show no results msg
                                            progressDialog.dismiss();
                                            Intent emptySearch = new Intent(FileListActivity.this,
                                                    FileSearchEmptyActivity.class);
                                            startActivity(emptySearch);
                                        }
                                    }
                                    @Override
                                    public void onFailure(String message, int status) {
                                        // do nothing
                                        progressDialog.dismiss();
                                    }
                                });
                            } else
                                Toast.makeText(FileListActivity.this, "You must enter a name", Toast.LENGTH_LONG).show();
                        } else if (tabHost.getCurrentTab() == 1) { // Search by extension
                            String extension = ((EditText) layout.findViewById(R.id.edit_tab2)).getText().toString().toLowerCase();
                            if ((extension.compareTo("") != 0) && (extension.compareTo(" ") != 0)) {
                                mFilesService.getFilesByExtension(mUserAccount.getUserId(), extension, new ServiceCallback<List<File>>() {
                                    @Override
                                    public void onSuccess(List<File> files, int status) {
                                        if (files.size() > 0) {
                                            progressDialog.dismiss();
                                            mFilesAdapter.updateFiles(files);
                                            Log.d(TAG, "Number of files received " + files.size());
                                        } else {
                                            // Show no results msg
                                            progressDialog.dismiss();
                                            Intent emptySearch = new Intent(FileListActivity.this,
                                                    FileSearchEmptyActivity.class);
                                            startActivity(emptySearch);
                                        }
                                    }
                                    @Override
                                    public void onFailure(String message, int status) {
                                        // do nothing
                                        progressDialog.dismiss();
                                    }
                                });
                            } else
                                Toast.makeText(FileListActivity.this, "You must enter a valid extension", Toast.LENGTH_LONG).show();
                        } else { // Tab 3 was active. Search by owner.
                            int ownerId = ((Collaborator)spinner_tab4.getSelectedItem()).getId();
                            System.out.println("Owner selected >>>>> "+ownerId);
                            mFilesService.getFilesByOwner(mUserAccount.getUserId(), ownerId, new ServiceCallback<List<File>>() {
                                @Override
                                public void onSuccess(List<File> files, int status) {
                                    if (files.size() > 0) {
                                        progressDialog.dismiss();
                                        mFilesAdapter.updateFiles(files);
                                        Log.d(TAG, "Number of files received " + files.size());
                                    } else {
                                        // Show no results msg
                                        progressDialog.dismiss();
                                        Intent emptySearch = new Intent(FileListActivity.this,
                                                FileSearchEmptyActivity.class);
                                        startActivity(emptySearch);
                                    }
                                }
                                @Override
                                public void onFailure(String message, int status) {
                                    // Do nothing
                                    progressDialog.dismiss();
                                }
                            });
                        }
                        dialog.cancel();
                    }
                })
                .setNegativeButton(getString(R.string.settings_cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog dialog = dBuilder.create();
        dialog.show();
    }

    private View getTabIndicator(Context context, int title, int icon) {
        View view = LayoutInflater.from(context).inflate(R.layout.tab_layout, null);
        ImageView iv = (ImageView) view.findViewById(R.id.imageView);
        iv.setImageResource(icon);
        //TextView tv = (TextView) view.findViewById(R.id.textView);
        //tv.setText(title);
        return view;
    }

    public double getLatitude() {
        return mLatitude;
    }

    public void setLatitude(double mLatitude) {
        this.mLatitude = mLatitude;
    }

    public double getLongitude() {
        return mLongitude;
    }

    public void setLongitude(double mLongitude) {
        this.mLongitude = mLongitude;
    }

    @Override
    public boolean shouldShowDeleteButton(int position) {
        File actualFile = mFiles.get(position);
        return (actualFile.getUserOwner().equals(mUserAccount.getUserId()));
    }

    @Override
    public boolean shouldShowPrevDownloadButton(int position) {
        File actualFile = mFiles.get(position);
        return (!actualFile.isDir());
    }

    ArrayList<String> getFileVersions(int lastVersion){
        ArrayList<String> v = new ArrayList<>();
        for (int last = (lastVersion-1); last >= 1; last--){
            v.add(getString(R.string.versionNumber)+" "+last);
        }
        return v;
    }
}