package com.dennisbrink.mt.global.mypackedfileviewer;

import androidx.annotation.NonNull;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.signature.ObjectKey;

import java.io.InputStream;

public class InputStreamLoader implements ModelLoader<InputStream, InputStream> {

    @Override
    public LoadData<InputStream> buildLoadData(@NonNull InputStream model, int width, int height, @NonNull Options options) {
        return new LoadData<>(new ObjectKey(model), new InputStreamFetcher(model));
    }

    @Override
    public boolean handles(@NonNull InputStream model) {
        return true;
    }

    public static class Factory implements ModelLoaderFactory<InputStream, InputStream> {
        @NonNull
        @Override
        public ModelLoader<InputStream, InputStream> build(@NonNull MultiModelLoaderFactory multiFactory) {
            return new InputStreamLoader();
        }

        @Override
        public void teardown() {
            // Do nothing, this instance doesn't own the model.
        }
    }
}

class InputStreamFetcher implements DataFetcher<InputStream> {
    private final InputStream inputStream;

    InputStreamFetcher(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public void loadData(@NonNull Priority priority, DataCallback<? super InputStream> callback) {
        callback.onDataReady(inputStream);
    }

    @Override
    public void cleanup() {
        // Close stream if necessary
    }

    @Override
    public void cancel() {
        // Cancel the operation if needed
    }

    @NonNull
    @Override
    public Class<InputStream> getDataClass() {
        return InputStream.class;
    }

    @NonNull
    @Override
    public DataSource getDataSource() {
        return DataSource.LOCAL;
    }
}