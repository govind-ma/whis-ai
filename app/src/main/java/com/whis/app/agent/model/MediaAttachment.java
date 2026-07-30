package com.whis.app.agent.model;

/**
 * Media attachment model for images, audio files, or PDF documents (AI_AGENT_PLAN.md Section 4.2).
 */
public class MediaAttachment {

    public enum AttachmentType {
        IMAGE,
        AUDIO,
        PDF
    }

    public String base64Data;
    public String mimeType;
    public AttachmentType attachmentType;

    public MediaAttachment() {}

    public MediaAttachment(String base64Data, String mimeType, AttachmentType attachmentType) {
        this.base64Data = base64Data;
        this.mimeType = mimeType;
        this.attachmentType = attachmentType;
    }
}
