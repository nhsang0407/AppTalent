package com.shoplens.ai;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.shoplens.ai.admin.AdminDashboardActivity;
import com.shoplens.ai.auth.LoginActivity;
import com.shoplens.ai.user.HomeActivity;
import com.shoplens.ai.utils.Constants;
import com.shoplens.ai.utils.FirebaseUtils;

/**
 * Launcher activity acting purely as a router:
 *   - signed out          -> LoginActivity
 *   - signed in (admin)   -> AdminDashboardActivity
 *   - signed in (user)    -> HomeActivity
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!FirebaseUtils.isLoggedIn()) {
            go(LoginActivity.class);
            return;
        }

        FirebaseUtils.getCurrentUserRole(role -> {
            if (Constants.ROLE_ADMIN.equals(role)) {
                go(AdminDashboardActivity.class);
            } else {
                go(HomeActivity.class);
            }
        });
    }

    private void go(Class<?> target) {
        startActivity(new Intent(this, target));
        finish();
    }
}
