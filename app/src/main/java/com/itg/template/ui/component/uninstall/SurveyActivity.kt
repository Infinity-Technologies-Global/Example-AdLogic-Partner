package com.itg.template.ui.component.uninstall

import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri
import com.itg.template.R
import com.itg.template.databinding.ActivitySurveyBinding
import com.itg.template.ui.bases.BaseActivity
import com.itg.template.ui.bases.ext.click
import com.itg.template.utils.Routes
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SurveyActivity : BaseActivity<ActivitySurveyBinding>() {


    override fun getLayoutActivity() = R.layout.activity_survey

    override fun initViews() {
        super.initViews()
    }

    override fun onClickViews() {
        super.onClickViews()

        mBinding.btnCancel.click {
            whenBack()
        }

        mBinding.imgBack.click {
            whenBack()
        }

        mBinding.btnUninstall.click {
            try {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    "package:${packageName}".toUri()
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } catch (_: Exception) {
            }
            finish()
        }
    }

    private fun whenBack() {
        Routes.startMainActivity(this)
        finish()
    }

//    override fun onActivityBackPressed() {
//        whenBack()
//    }

}