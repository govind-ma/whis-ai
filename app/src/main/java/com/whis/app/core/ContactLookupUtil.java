package com.whis.app.core;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;

/**
 * Shared contact-lookup utility — MASTER_PLAN.md Section 3.6.
 * <p>
 * Built once, used by both Call module ({@code com.whis.app.call}) and
 * MSG module ({@code com.whis.app.msg}). Handles Indian phone number
 * normalization ({@code +91XXXXXXXXXX}, {@code 0XXXXXXXXXX},
 * {@code XXXXXXXXXX} formats) and queries
 * {@link ContactsContract.PhoneLookup} to determine whether the number
 * is a saved contact.
 * <p>
 * <b>Requires {@code READ_CONTACTS} permission.</b> If the permission
 * has not been granted at runtime, {@link #check(String, Context)} returns
 * a result with {@code isContact = false} and a {@code null} contact name —
 * it does not throw.
 */
public class ContactLookupUtil {

    private ContactLookupUtil() {
        // Static-only utility — do not instantiate.
    }

    /**
     * Look up a phone number against the device's contacts.
     *
     * @param rawNumber the incoming phone number in any common Indian format
     *                  ({@code +91XXXXXXXXXX}, {@code 0XXXXXXXXXX}, or
     *                  {@code XXXXXXXXXX})
     * @param context   any context
     * @return a {@link ContactResult} with the lookup outcome
     */
    public static ContactResult check(String rawNumber, Context context) {
        if (TextUtils.isEmpty(rawNumber)) {
            return new ContactResult(false, null);
        }

        String normalized = normalizeIndianNumber(rawNumber.trim());

        // Build the set of number variants to try.
        // ContactsContract.PhoneLookup does its own normalization, but being
        // explicit about variants makes the lookup more reliable on OEMs
        // that strip or rewrite the country code inconsistently.
        String[] variants = buildVariants(normalized);

        ContentResolver resolver = context.getContentResolver();
        for (String variant : variants) {
            ContactResult result = lookupNumber(resolver, variant);
            if (result.isContact()) {
                return result;
            }
        }

        return new ContactResult(false, null);
    }

    // ── Normalization ────────────────────────────────────────────────────

    /**
     * Normalize an Indian phone number to a bare 10-digit form.
     * <p>
     * Strips:
     * <ul>
     *   <li>{@code +91} country prefix</li>
     *   <li>Leading {@code 0} trunk prefix</li>
     *   <li>Dashes, spaces, parentheses</li>
     * </ul>
     *
     * @param number raw number string
     * @return 10-digit (if valid Indian mobile/landline) or the cleaned
     *         input as-is if it doesn't match expected patterns
     */
    static String normalizeIndianNumber(String number) {
        // Strip all non-digit characters except leading '+'
        String cleaned = number.replaceAll("[^+\\d]", "");

        // +91XXXXXXXXXX → XXXXXXXXXX
        if (cleaned.startsWith("+91") && cleaned.length() == 13) {
            return cleaned.substring(3);
        }

        // 91XXXXXXXXXX (without +) → XXXXXXXXXX
        if (cleaned.startsWith("91") && cleaned.length() == 12) {
            return cleaned.substring(2);
        }

        // 0XXXXXXXXXX → XXXXXXXXXX
        if (cleaned.startsWith("0") && cleaned.length() == 11) {
            return cleaned.substring(1);
        }

        // Already 10 digits, or a non-Indian format — return cleaned
        return cleaned;
    }

    /**
     * Build an array of format variants for lookup.
     * Given a normalized 10-digit number, returns:
     * {@code +91XXXXXXXXXX}, {@code 0XXXXXXXXXX}, {@code XXXXXXXXXX}.
     * For non-10-digit inputs, returns the input as-is.
     */
    private static String[] buildVariants(String normalized) {
        if (normalized.length() == 10 && normalized.matches("\\d{10}")) {
            return new String[]{
                    "+91" + normalized,   // E.164 with country code
                    "0" + normalized,     // Trunk prefix
                    normalized            // Bare 10 digits
            };
        }
        // Non-standard length — try as-is
        return new String[]{normalized};
    }

    // ── Contacts query ───────────────────────────────────────────────────

    /**
     * Query ContactsContract.PhoneLookup for a single number variant.
     */
    private static ContactResult lookupNumber(ContentResolver resolver, String number) {
        Uri lookupUri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number)
        );

        String[] projection = {ContactsContract.PhoneLookup.DISPLAY_NAME};

        Cursor cursor = null;
        try {
            cursor = resolver.query(lookupUri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                String displayName = cursor.getString(
                        cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME)
                );
                return new ContactResult(true, displayName);
            }
        } catch (SecurityException e) {
            // READ_CONTACTS permission not granted — degrade gracefully.
        } catch (Exception e) {
            // Defensive: don't crash the caller on unexpected content-provider errors.
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return new ContactResult(false, null);
    }

    // ── Result type ──────────────────────────────────────────────────────

    /**
     * Immutable result of a contact lookup.
     */
    public static class ContactResult {

        private final boolean isContact;
        private final String contactName;

        public ContactResult(boolean isContact, String contactName) {
            this.isContact = isContact;
            this.contactName = contactName;
        }

        /**
         * @return {@code true} if the number belongs to a saved contact
         */
        public boolean isContact() {
            return isContact;
        }

        /**
         * @return the contact's display name, or {@code null} if not a contact
         */
        public String getContactName() {
            return contactName;
        }
    }
}
