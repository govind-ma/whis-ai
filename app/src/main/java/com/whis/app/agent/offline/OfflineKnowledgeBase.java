package com.whis.app.agent.offline;

import android.content.Context;
import android.content.res.AssetManager;

import com.whis.app.agent.model.FraudScenario;
import com.whis.app.agent.model.RiskLevel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads offline knowledge base JSON assets into memory (AI_AGENT_PLAN.md Section 4.2 Day 2).
 */
public class OfflineKnowledgeBase {

    private static List<FraudScenario> scenarioList = null;
    private static Map<String, String> bankHelplines = null;

    private OfflineKnowledgeBase() {
        // Utility class
    }

    public static synchronized void init(Context context) {
        if (scenarioList != null && bankHelplines != null) return;

        scenarioList = new ArrayList<>();
        bankHelplines = new HashMap<>();

        if (context == null) {
            populateFallbackData();
            return;
        }

        try {
            AssetManager assets = context.getAssets();

            // Load offline_scenarios.json
            BufferedReader reader = new BufferedReader(new InputStreamReader(assets.open("offline_scenarios.json")));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();

            JSONArray array = new JSONArray(sb.toString());
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                FraudScenario scenario = new FraudScenario();
                scenario.id = obj.optString("id", "UNKNOWN");
                scenario.responseHindi = obj.optString("responseHindi", "");
                scenario.responseGujarati = obj.optString("responseGujarati", "");
                scenario.responseEnglish = obj.optString("responseEnglish", "");
                scenario.escalate = obj.optBoolean("escalate", false);

                String riskStr = obj.optString("risk", "MEDIUM");
                try {
                    scenario.risk = RiskLevel.valueOf(riskStr);
                } catch (Exception e) {
                    scenario.risk = RiskLevel.MEDIUM;
                }

                JSONArray kwArr = obj.optJSONArray("keywords");
                if (kwArr != null) {
                    for (int k = 0; k < kwArr.length(); k++) {
                        scenario.keywords.add(kwArr.getString(k));
                    }
                }

                JSONArray nextHi = obj.optJSONArray("nextStepsHindi");
                if (nextHi != null) {
                    for (int k = 0; k < nextHi.length(); k++) {
                        scenario.nextStepsHindi.add(nextHi.getString(k));
                    }
                }
                scenarioList.add(scenario);
            }

            // Load bank_helplines.json
            BufferedReader bankReader = new BufferedReader(new InputStreamReader(assets.open("bank_helplines.json")));
            StringBuilder bankSb = new StringBuilder();
            while ((line = bankReader.readLine()) != null) bankSb.append(line);
            bankReader.close();

            JSONObject bankObj = new JSONObject(bankSb.toString());
            JSONArray banks = bankObj.optJSONArray("banks");
            if (banks != null) {
                for (int b = 0; b < banks.length(); b++) {
                    JSONObject bank = banks.getJSONObject(b);
                    bankHelplines.put(bank.getString("name").toUpperCase(), bank.getString("helpline"));
                }
            }
        } catch (Exception e) {
            populateFallbackData();
        }
    }

    private static void populateFallbackData() {
        if (scenarioList == null) scenarioList = new ArrayList<>();

        if (scenarioList.isEmpty()) {
            FraudScenario upi = new FraudScenario();
            upi.id = "UPI_COLLECT_REQUEST";
            upi.keywords.add("collect");
            upi.keywords.add("request");
            upi.keywords.add("approve");
            upi.risk = RiskLevel.HIGH;
            upi.responseHindi = "Ye UPI Collect Request fraud ho sakta hai! UPI PIN sirf paisa bhejne ke liye hota hai.";
            upi.nextStepsHindi.add("PAISA PAY par click mat karo");
            upi.nextStepsHindi.add("1930 par call karo");
            scenarioList.add(upi);
        }

        if (bankHelplines == null) bankHelplines = new HashMap<>();
        if (bankHelplines.isEmpty()) {
            bankHelplines.put("SBI", "1800-11-2211");
            bankHelplines.put("HDFC", "1800-258-6161");
            bankHelplines.put("ICICI", "1800-1080");
            bankHelplines.put("DEFAULT", "Debit card ke peeche helpline number dekho");
        }
    }

    public static List<FraudScenario> getScenarios(Context context) {
        init(context);
        return scenarioList;
    }

    public static String getBankHelpline(Context context, String bankName) {
        init(context);
        if (bankName == null) return bankHelplines.get("DEFAULT");
        String number = bankHelplines.get(bankName.trim().toUpperCase());
        return number != null ? number : bankHelplines.get("DEFAULT");
    }
}
