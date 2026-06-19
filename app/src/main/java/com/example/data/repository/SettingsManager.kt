package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("warshati_prefs", Context.MODE_PRIVATE)

    private val _appLanguage = MutableStateFlow(getSavedLanguage())
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    private val _appTheme = MutableStateFlow(getSavedTheme())
    val appTheme: StateFlow<String> = _appTheme.asStateFlow()

    private val _darkMode = MutableStateFlow(getSavedDarkMode())
    val darkMode: StateFlow<String> = _darkMode.asStateFlow()

    private val _walletPocketInit = MutableStateFlow(getSavedWalletPocketInit())
    val walletPocketInit: StateFlow<Double> = _walletPocketInit.asStateFlow()

    private val _walletBankInit = MutableStateFlow(getSavedWalletBankInit())
    val walletBankInit: StateFlow<Double> = _walletBankInit.asStateFlow()

    private val _walletGoodsInit = MutableStateFlow(getSavedWalletGoodsInit())
    val walletGoodsInit: StateFlow<Double> = _walletGoodsInit.asStateFlow()

    private val _walletPersonalInit = MutableStateFlow(getSavedWalletPersonalInit())
    val walletPersonalInit: StateFlow<Double> = _walletPersonalInit.asStateFlow()

    private val _walletPocketName = MutableStateFlow(getSavedWalletPocketName())
    val walletPocketName: StateFlow<String> = _walletPocketName.asStateFlow()

    private val _walletBankName = MutableStateFlow(getSavedWalletBankName())
    val walletBankName: StateFlow<String> = _walletBankName.asStateFlow()

    private val _walletGoodsName = MutableStateFlow(getSavedWalletGoodsName())
    val walletGoodsName: StateFlow<String> = _walletGoodsName.asStateFlow()

    private val _walletPersonalName = MutableStateFlow(getSavedWalletPersonalName())
    val walletPersonalName: StateFlow<String> = _walletPersonalName.asStateFlow()

    private val _walletPocketInclude = MutableStateFlow(getSavedWalletPocketInclude())
    val walletPocketInclude: StateFlow<Boolean> = _walletPocketInclude.asStateFlow()

    private val _walletBankInclude = MutableStateFlow(getSavedWalletBankInclude())
    val walletBankInclude: StateFlow<Boolean> = _walletBankInclude.asStateFlow()

    private val _walletGoodsInclude = MutableStateFlow(getSavedWalletGoodsInclude())
    val walletGoodsInclude: StateFlow<Boolean> = _walletGoodsInclude.asStateFlow()

    private val _walletPersonalInclude = MutableStateFlow(getSavedWalletPersonalInclude())
    val walletPersonalInclude: StateFlow<Boolean> = _walletPersonalInclude.asStateFlow()

    fun getSavedLanguage(): String {
        return prefs.getString("key_language", "ar") ?: "ar"
    }

    fun setAppLanguage(lang: String) {
        prefs.edit().putString("key_language", lang).apply()
        _appLanguage.value = lang
    }

    fun getSavedTheme(): String {
        return prefs.getString("key_theme", "LIQUID_GLASS") ?: "LIQUID_GLASS"
    }

    fun setAppTheme(theme: String) {
        prefs.edit().putString("key_theme", theme).apply()
        _appTheme.value = theme
    }

    fun getSavedDarkMode(): String {
        return prefs.getString("key_dark_mode", "SYSTEM") ?: "SYSTEM"
    }

    fun setDarkMode(mode: String) {
        prefs.edit().putString("key_dark_mode", mode).apply()
        _darkMode.value = mode
    }

    fun getSavedWalletPocketInit(): Double {
        return prefs.getString("key_pocket_init_v2", "0.0")?.toDoubleOrNull() ?: 0.0
    }

    fun setWalletPocketInit(valDouble: Double) {
        prefs.edit().putString("key_pocket_init_v2", valDouble.toString()).apply()
        _walletPocketInit.value = valDouble
    }

    fun getSavedWalletBankInit(): Double {
        return prefs.getString("key_bank_init_v2", "0.0")?.toDoubleOrNull() ?: 0.0
    }

    fun setWalletBankInit(valDouble: Double) {
        prefs.edit().putString("key_bank_init_v2", valDouble.toString()).apply()
        _walletBankInit.value = valDouble
    }

    fun getSavedWalletGoodsInit(): Double {
        return prefs.getString("key_goods_init_v2", "-4000000.0")?.toDoubleOrNull() ?: -4000000.0
    }

    fun setWalletGoodsInit(valDouble: Double) {
        prefs.edit().putString("key_goods_init_v2", valDouble.toString()).apply()
        _walletGoodsInit.value = valDouble
    }

    fun getSavedWalletPersonalInit(): Double {
        return prefs.getString("key_personal_init_v2", "0.0")?.toDoubleOrNull() ?: 0.0
    }

    fun setWalletPersonalInit(valDouble: Double) {
        prefs.edit().putString("key_personal_init_v2", valDouble.toString()).apply()
        _walletPersonalInit.value = valDouble
    }

    fun getSavedWalletPocketName(): String {
        return prefs.getString("key_pocket_name_v2", "محفظة المحل") ?: "محفظة المحل"
    }

    fun setWalletPocketName(name: String) {
        prefs.edit().putString("key_pocket_name_v2", name).apply()
        _walletPocketName.value = name
    }

    fun getSavedWalletBankName(): String {
        return prefs.getString("key_bank_name_v2", "حساب بنكي") ?: "حساب بنكي"
    }

    fun setWalletBankName(name: String) {
        prefs.edit().putString("key_bank_name_v2", name).apply()
        _walletBankName.value = name
    }

    fun getSavedWalletGoodsName(): String {
        return prefs.getString("key_goods_name_v2", "سلعة") ?: "سلعة"
    }

    fun setWalletGoodsName(name: String) {
        prefs.edit().putString("key_goods_name_v2", name).apply()
        _walletGoodsName.value = name
    }

    fun getSavedWalletPersonalName(): String {
        return prefs.getString("key_personal_name_v2", "مصروف شخصي") ?: "مصروف شخصي"
    }

    fun setWalletPersonalName(name: String) {
        prefs.edit().putString("key_personal_name_v2", name).apply()
        _walletPersonalName.value = name
    }

    fun getSavedWalletPocketInclude(): Boolean {
        return prefs.getBoolean("key_pocket_include", true)
    }

    fun setWalletPocketInclude(include: Boolean) {
        prefs.edit().putBoolean("key_pocket_include", include).apply()
        _walletPocketInclude.value = include
    }

    fun getSavedWalletBankInclude(): Boolean {
        return prefs.getBoolean("key_bank_include", true)
    }

    fun setWalletBankInclude(include: Boolean) {
        prefs.edit().putBoolean("key_bank_include", include).apply()
        _walletBankInclude.value = include
    }

    fun getSavedWalletGoodsInclude(): Boolean {
        return prefs.getBoolean("key_goods_include", true)
    }

    fun setWalletGoodsInclude(include: Boolean) {
        prefs.edit().putBoolean("key_goods_include", include).apply()
        _walletGoodsInclude.value = include
    }

    fun getSavedWalletPersonalInclude(): Boolean {
        return prefs.getBoolean("key_personal_include", true)
    }

    fun setWalletPersonalInclude(include: Boolean) {
        prefs.edit().putBoolean("key_personal_include", include).apply()
        _walletPersonalInclude.value = include
    }

    fun getBudgetLimit(category: String, default: Double): Double {
        return prefs.getString("key_budget_limit_$category", default.toString())?.toDoubleOrNull() ?: default
    }

    fun setBudgetLimit(category: String, limit: Double) {
        prefs.edit().putString("key_budget_limit_$category", limit.toString()).apply()
    }
}
