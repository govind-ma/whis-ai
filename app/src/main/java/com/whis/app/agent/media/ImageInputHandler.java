package com.whis.app.agent.media;

import android.content.Context;
import android.net.Uri;
import android.util.Base64;

import com.whis.app.agent.model.MediaAttachment;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Image attachment handler (AI_AGENT_PLAN.md Section 4.2 Day 10).
 */
public class ImageInputHandler {

    private ImageInputHandler() {
        // Utility class
    }

    public static MediaAttachment processImageUri(Context context, Uri uri) {
        if (context == null || uri == null) return null;

        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            byte[] bytes = byteBuffer.toByteArray();
            String base64 = Base64.encodeToString(bytes, Base64.DEFAULT);
            String mimeType = context.getContentResolver().getType(uri);
            if (mimeType == null) mimeType = "image/jpeg";

            return new MediaAttachment(base64, mimeType, MediaAttachment.AttachmentType.IMAGE);
        } catch (Exception e) {
            return null;
        }
    }
}
