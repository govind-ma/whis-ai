package com.whis.app.ui.learn;

import java.util.Arrays;
import java.util.List;

/**
 * Static scam education lesson data in Hindi + English.
 * Covers the 6 most common Indian digital banking scams.
 * No network needed — fully offline.
 */
public class ScamLessonData {

    public static class Lesson {
        public final String emoji;
        public final String titleHindi;
        public final String titleEnglish;
        public final String exampleScript;   // What the scammer actually says
        public final String[] redFlags;      // 3 red flags in Hindi+English
        public final String whatToDo;        // Action in Hindi
        public final String helpline;        // Who to call

        public Lesson(String emoji, String titleHindi, String titleEnglish,
                      String exampleScript, String[] redFlags,
                      String whatToDo, String helpline) {
            this.emoji = emoji;
            this.titleHindi = titleHindi;
            this.titleEnglish = titleEnglish;
            this.exampleScript = exampleScript;
            this.redFlags = redFlags;
            this.whatToDo = whatToDo;
            this.helpline = helpline;
        }
    }

    public static List<Lesson> getAllLessons() {
        return Arrays.asList(

            new Lesson(
                "\uD83C\uDFE6",
                "KYC Scam",
                "KYC Fraud",
                "SMS: \"Dear Customer, your SBI account will be blocked. Update KYC immediately: bit.ly/xxxxx\"",
                new String[]{
                    "\u274C Link से KYC कभी नहीं होती — असली बैंक ब्रांच बुलाते हैं",
                    "\u274C 'Immediately' या 'Urgent' शब्द — डराने की कोशिश",
                    "\u274C Short URL (bit.ly, tinyurl) — fake website का link"
                },
                "\u2705 Link पर क्लिक मत करें। अपने बैंक का official नंबर dial करें।",
                "बैंक हेल्पलाइन / 1930"
            ),

            new Lesson(
                "\uD83D\uDCB8",
                "UPI Collect Request Scam",
                "UPI Reverse Fraud",
                "WhatsApp: \"मैं आपको ₹5000 भेज रहा हूं। बस इस UPI request को accept करें।\"",
                new String[]{
                    "\u274C 'Accept करें' = पैसे आना नहीं, जाना है!",
                    "\u274C अनजान नंबर से UPI request — कभी accept मत करें",
                    "\u274C पैसे receive करने के लिए कभी PIN नहीं डालते"
                },
                "\u2705 UPI request accept करने से पैसे जाते हैं, आते नहीं। Request reject करें।",
                "1930 / cybercrime.gov.in"
            ),

            new Lesson(
                "\uD83D\uDCF1",
                "Fake Loan App Scam",
                "Loan App Fraud",
                "SMS: \"Congratulations! ₹50,000 loan approved. Pay ₹999 processing fee to unlock.\"",
                new String[]{
                    "\u274C पहले fee लेना = 100% scam। असली बैंक fee loan से काटते हैं",
                    "\u274C बिना documents के loan = fraud",
                    "\u274C Unknown app से loan = contacts/photos की blackmail का खतरा"
                },
                "\u2705 कोई fee मत दें। RBI registered lenders की list rbi.org.in पर देखें।",
                "1930 / RBI Helpline: 14440"
            ),

            new Lesson(
                "\uD83D\uDD11",
                "OTP Fraud",
                "OTP Sharing Scam",
                "Call: \"Main SBI se bol raha hun. Account verify karne ke liye OTP share karein.\"",
                new String[]{
                    "\u274C कोई भी असली बैंक OTP कभी नहीं मांगता — कभी नहीं!",
                    "\u274C OTP = आपकी bank ki चाबी — किसी को मत दें",
                    "\u274C 'Bank से call' — banks सिर्फ registered number से call करती है"
                },
                "\u2705 फोन काटें। OTP कभी share न करें — चाहे कोई भी हो।",
                "अपना बैंक का official नंबर / 1930"
            ),

            new Lesson(
                "\uD83C\uDFC6",
                "Prize / Lottery Scam",
                "Lottery Fraud",
                "Call: \"Aapka number KBC lottery mein select hua hai. ₹25 lakh jeetne ke liye tax bharo.\"",
                new String[]{
                    "\u274C जो lottery खेली नहीं, उसमें जीत कैसे?",
                    "\u274C Prize लेने के लिए fee/tax = हमेशा fraud",
                    "\u274C 'Secret रखो' — असली prize में secrecy क्यों?"
                },
                "\u2705 फोन काटें। परिवार को बताएं। पैसे भेजने से पहले 1930 पर call करें।",
                "1930 / cybercrime.gov.in"
            ),

            new Lesson(
                "\uD83D\uDCA1",
                "Electricity / Utility Scam",
                "Utility Bill Fraud",
                "SMS: \"URGENT: Your electricity connection will be cut in 2 hours. Pay ₹1850 now: bit.ly/pay\"",
                new String[]{
                    "\u274C Electricity board कभी SMS link से payment नहीं लेती",
                    "\u274C '2 hours में connection कट जाएगा' — डराने की technique",
                    "\u274C Link पर payment = scammer का account"
                },
                "\u2705 अपने local electricity office का number Google करें और direct call करें।",
                "DISCOM helpline / 1930"
            )
        );
    }
}
