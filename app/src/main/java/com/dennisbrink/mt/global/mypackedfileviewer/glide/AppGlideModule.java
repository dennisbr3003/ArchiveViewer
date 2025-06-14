package com.dennisbrink.mt.global.mypackedfileviewer.glide;

import android.content.Context;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;

import java.io.InputStream;

@GlideModule
public final class AppGlideModule extends com.bumptech.glide.module.AppGlideModule {
    @Override
    public void registerComponents(Context context, Glide glide, Registry registry) {
        registry.append(InputStream.class, InputStream.class, new InputStreamLoader.Factory());
    }
}