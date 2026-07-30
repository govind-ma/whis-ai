package com.whis.app.ui.onboarding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.whis.app.R;

/**
 * Step 4 — Primary UPI app selection: GPay / PhonePe / Paytm / BHIM / Other
 * (UI_PLAN.md §2.2 step 4).
 */
public class UpiSelectFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_upi_select, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button btnGPay = view.findViewById(R.id.btn_gpay);
        Button btnPhonePe = view.findViewById(R.id.btn_phonepe);
        Button btnPaytm = view.findViewById(R.id.btn_paytm);
        Button btnBhim = view.findViewById(R.id.btn_bhim);
        Button btnOther = view.findViewById(R.id.btn_other_upi);

        Button[] allButtons = { btnGPay, btnPhonePe, btnPaytm, btnBhim, btnOther };
        String[] upiValues = { "GPay", "PhonePe", "Paytm", "BHIM", "Other" };

        for (int i = 0; i < allButtons.length; i++) {
            final String val = upiValues[i];
            allButtons[i].setOnClickListener(v -> {
                OnboardingActivity host = (OnboardingActivity) requireActivity();
                OnboardingData data = host.getData();
                data.primaryUpi = val;

                // Visual feedback before navigating
                highlightSelected(allButtons, (Button) v);

                // Small delay so the user sees the selection
                v.postDelayed(() -> host.goToNext(new EmergencyContactFragment()), 200);
            });
        }
    }

    private void highlightSelected(Button[] group, Button selected) {
        for (Button btn : group) {
            if (btn == selected) {
                btn.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.whis_trusted));
                btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.whis_surface));
            } else {
                btn.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.whis_border));
                btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.whis_text_hi));
            }
        }
    }
}
