package com.messages.ui.language.ui

import android.os.Bundle
import androidx.core.view.isVisible
import com.messages.R
import com.messages.data.models.LanguageModel
import com.messages.databinding.ActivityLanguageBinding
import com.messages.extentions.getStringValue
import com.messages.extentions.launchActivity
import com.messages.ui.base.BaseActivity
import com.messages.ui.language.adapter.LanguageAdapter
import com.messages.ui.onboarding.ui.OnBoardingActivity
import com.messages.utils.IS_FROM_SPLASH
import com.messages.utils.setOnSafeClickListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LanguageActivity : BaseActivity() {

    private val binding by lazy { ActivityLanguageBinding.inflate(layoutInflater) }
    private var languageAdapter: LanguageAdapter? = null
    private var isFromSplash = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initToolbar()
        initAdapter()
        initActions()

    }

    private fun initToolbar() {
        isFromSplash = intent.getBooleanExtra(IS_FROM_SPLASH, false)
        binding.apply {
            if (isFromSplash) {
                updateStatusBarColor(R.color.app_bg)
                toolbarLanguage.isVisible = false
                clToolbar.isVisible = true
            } else {
                toolbarLanguage.apply {
                    getBinding().tvToolbarTitle.text = getStringValue(R.string.app_language)
                    getBinding().btnToolbarAction.isVisible = true
                    getBinding().btnToolbarAction.text = getStringValue(R.string.done)
                }
                toolbarLanguage.isVisible = true
                clToolbar.isVisible = false
            }
        }
    }

    private fun initActions() {
        binding.apply {
            toolbarLanguage.getBinding().apply {
                ivToolbarBack.setOnSafeClickListener { finish() }

                btnToolbarAction.setOnSafeClickListener {
                    btnLanguageDone.performClick()
                }
            }

            btnLanguageDone.setOnSafeClickListener {
                appPreferences.appLanguage = languageAdapter?.selectedLanguageCode ?: "en"
                if (isFromSplash) {
                    launchActivity(OnBoardingActivity::class.java)
                } else {
                    recreateActivity()
                }
                finish()
            }
        }
    }

    private fun initAdapter() {
        val languageList = ArrayList<LanguageModel>()
        val languageCode = resources.getStringArray(R.array.language_code)
        val languageName = resources.getStringArray(R.array.language_name)
        val languageCountry = resources.getStringArray(R.array.language_country)
        languageCode.forEachIndexed { index, s ->
            languageList.add(
                LanguageModel(
                    code = s,
                    name = languageName[index],
                    countryName = languageCountry[index]
                )
            )
        }
        binding.rcvLanguages.apply {
            languageAdapter = LanguageAdapter(languageList, appPreferences.appLanguage)
            adapter = languageAdapter
        }
    }

    override fun onResume() {
        super.onResume()
        if (appPreferences.isShowOnBoarding) {
            checkAndAskDefaultApp()
        }
    }
}