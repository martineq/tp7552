package com.fiuba.app.udrive.network;

import android.content.Context;
import android.telecom.Call;

import com.fiuba.app.udrive.model.FileInfo;
import com.fiuba.app.udrive.model.FolderData;
import com.fiuba.app.udrive.model.GenericResult;
import com.fiuba.app.udrive.model.MyPhoto;
import com.fiuba.app.udrive.model.Tag;
import com.fiuba.app.udrive.model.UserAccount;
import com.fiuba.app.udrive.model.UserData;
import com.fiuba.app.udrive.model.UserFullName;
import com.fiuba.app.udrive.model.UserProfile;

import java.util.ArrayList;
import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.DELETE;
import retrofit.http.GET;
import retrofit.http.PUT;
import retrofit.http.Path;

public class FileMetadataService extends AbstractService {

    private interface FileTagServiceApi {

        // Gets the file or folder tags
        @GET("/filetags/users/{userId}/files/{fileId}")
        void getTags(@Path("userId") int userId, @Path("fileId") int fileId, Callback<ArrayList<Tag>> tags);

        // Updates the tag set for the given file ID
        @PUT("/filetags/users/{userId}/files/{fileId}")
        void updateTags(@Path("userId") int userId, @Path("fileId") int fileId, @Body ArrayList<Tag> tagList,
                        Callback<GenericResult> result);

        // Gets the complete info about a folder or file
        @GET("/fileinfo/users/{userId}/files/{fileId}")
        void getFileInfo(@Path("userId") int userId, @Path("fileId") int fileId, Callback<FileInfo> fileInfo);

        // Updates a file or folder name
        @PUT("/filename/users/{userId}/files/{fileId}")
        void updateFilename(@Path("userId") int userId, @Path("fileId") int fileId, @Body FolderData data,
                            Callback<GenericResult> result);
    }

    private FileTagServiceApi mFileTagServiceApi;

    public FileMetadataService(Context context) {
        super(context);
        this.mFileTagServiceApi = createService(FileTagServiceApi.class, null);
    }

    public FileMetadataService(String token, Context context) {
        super(context);
        this.mFileTagServiceApi = createService(FileTagServiceApi.class, token);
    }

    public void getTags(int userId, int fileId, final ServiceCallback<ArrayList<Tag>> tags){
        mFileTagServiceApi.getTags(userId, fileId, new Callback<ArrayList<Tag>>() {
            @Override
            public void success(ArrayList<Tag> tagList, Response response) {
                tags.onSuccess(tagList, response.getStatus());
            }

            @Override
            public void failure(RetrofitError error) {
                int status;
                if (error.getKind() == RetrofitError.Kind.NETWORK) {
                    status = 503;
                } else
                    status = error.getResponse().getStatus();
                tags.onFailure(error.getMessage(), status);
            }
        });
    }


    // Passes the final tag list corresponding to a file.
    public void updateFileTags(int userId, int fileId, @Body ArrayList<Tag> tagList,
                        final ServiceCallback<GenericResult> result){
        mFileTagServiceApi.updateTags(userId, fileId, tagList, new Callback<GenericResult>() {
            @Override
            public void success(GenericResult genericResult, Response response) {
                result.onSuccess(genericResult, response.getStatus());
            }

            @Override
            public void failure(RetrofitError error) {
                int status;
                if (error.getKind() == RetrofitError.Kind.NETWORK) {
                    status = 503;
                } else
                    status = error.getResponse().getStatus();
                result.onFailure(error.getMessage(), status);
            }
        });
    }

    // Gets all the folder or file info items
    public void getFileInfo(int userId, int fileId, final ServiceCallback<FileInfo> fileInfo){
        mFileTagServiceApi.getFileInfo(userId, fileId, new Callback<FileInfo>() {
            @Override
            public void success(FileInfo fInfo, Response response) {
                fileInfo.onSuccess(fInfo, response.getStatus());
            }

            @Override
            public void failure(RetrofitError error) {
                int status;
                if (error.getKind() == RetrofitError.Kind.NETWORK) {
                    status = 503;
                } else
                    status = error.getResponse().getStatus();
                fileInfo.onFailure(error.getMessage(), status);
            }
        });
    }

    // Sends the new name for a file or folder to be updated on database
    public void updateFilename(int userId, int fileId, FolderData data, final ServiceCallback<GenericResult> result){
        mFileTagServiceApi.updateFilename(userId, fileId, data, new Callback<GenericResult>() {
            @Override
            public void success(GenericResult genericResult, Response response) {
                result.onSuccess(genericResult, response.getStatus());
            }

            @Override
            public void failure(RetrofitError error) {
                int status;
                if (error.getKind() == RetrofitError.Kind.NETWORK) {
                    status = 503;
                } else
                    status = error.getResponse().getStatus();
                result.onFailure(error.getMessage(), status);
            }
        });
    }

}
