package kr.ac.ssu.zette.ui.activity;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import kr.ac.ssu.zette.R;
import kr.ac.ssu.zette.databinding.ActivityOnboardingBinding;
import kr.ac.ssu.zette.databinding.ItemOnboardingPageBinding;

// 온보딩 튜토리얼 (Figma 01-1 ~ 01-3). 밀어서 넘기고, 끝나면 권한 설정(01-4)으로 간다
public class OnboardingActivity extends AppCompatActivity {

    // 그림은 다크 모드면 drawable-night-nodpi 것을 쓴다
    private static final int[] IMAGES = {
            R.drawable.onboarding_tutorial_1, R.drawable.onboarding_tutorial_2, R.drawable.onboarding_tutorial_3};
    private static final int[] TITLES = {
            R.string.onboarding_1_title, R.string.onboarding_2_title, R.string.onboarding_3_title};
    private static final int[] BODIES = {
            R.string.onboarding_1_body, R.string.onboarding_2_body, R.string.onboarding_3_body};

    private ActivityOnboardingBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityOnboardingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        binding.pager.setAdapter(new PageAdapter());
        binding.pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                boolean last = position == IMAGES.length - 1;
                binding.btnNext.setText(last ? R.string.onboarding_to_permission : R.string.onboarding_next);
                binding.btnSkip.setVisibility(last ? View.INVISIBLE : View.VISIBLE);
            }
        });
        binding.btnSkip.setOnClickListener(v -> openPermission());
        binding.btnNext.setOnClickListener(v -> {
            int page = binding.pager.getCurrentItem();
            if (page < IMAGES.length - 1) {
                binding.pager.setCurrentItem(page + 1);
            } else {
                openPermission();
            }
        });
        // 뒤로 가기는 이전 장으로
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                int page = binding.pager.getCurrentItem();
                if (page > 0) {
                    binding.pager.setCurrentItem(page - 1);
                } else {
                    finish();
                }
            }
        });
    }

    private void openPermission() {
        startActivity(new Intent(this, OnboardingPermissionActivity.class));
    }

    private static final class PageAdapter extends RecyclerView.Adapter<PageAdapter.Holder> {

        static final class Holder extends RecyclerView.ViewHolder {
            final ItemOnboardingPageBinding binding;

            Holder(@NonNull ItemOnboardingPageBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(ItemOnboardingPageBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            ItemOnboardingPageBinding b = holder.binding;
            b.ivIllustration.setImageResource(IMAGES[position]);
            b.tvTitle.setText(TITLES[position]);
            b.tvBody.setText(BODIES[position]);

            // 페이지마다 자기 위치의 점을 길게 그린다
            View[] dots = {b.dot1, b.dot2, b.dot3};
            float density = b.getRoot().getResources().getDisplayMetrics().density;
            for (int i = 0; i < dots.length; i++) {
                boolean active = i == position;
                ViewGroup.LayoutParams lp = dots[i].getLayoutParams();
                lp.width = Math.round((active ? 20 : 6) * density);
                dots[i].setLayoutParams(lp);
                dots[i].setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(b.getRoot().getContext(),
                        active ? R.color.green_brand : R.color.border_hairline)));
            }
        }

        @Override
        public int getItemCount() {
            return IMAGES.length;
        }
    }
}
