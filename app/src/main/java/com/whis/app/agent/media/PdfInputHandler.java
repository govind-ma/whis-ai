package com.whis.app.agent.media;

import android.content.Context;
import android.net.Uri;
import android.util.Base64;

import com.whis.app.agent.model.MediaAttachment;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * PDF attachment handler (AI_AGENT_PLAN.md Section 4.2 Day 10).
 */
public class PdfInputHandler {

    private PdfInputHandler() {
        // Utility class
    }

    public static MediaAttachment processPdfUri(Context context, Uri uri) {
        if (context == null || uri == null) return null;

        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            byte[] bytes = byteBuffer.toByteArray();
            String base64 = Base64.encodeToString(bytes, Base64.DEFAULT);

            return new MediaAttachment(base64, "application/pdf", MediaAttachment.AttachmentType.PDF);
        } catch (Exception e) {
            return null;
        }
    }
}
