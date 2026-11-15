package com.duda.profit

import android.content.Context
import org.json.JSONObject
import java.util.Calendar

/** Armazena os dados do painel. A página (WebView) e o widget leem e gravam aqui. */
class Store(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("painel", Context.MODE_PRIVATE)

    fun get(key: String): String? = sp.getString(key, null)

    fun set(key: String, value: String) {
        sp.edit().putString(key, value).apply()
    }

    fun remove(key: String) {
        sp.edit().remove(key).apply()
    }

    /** Apaga tudo o que o app guardou neste celular (usado ao excluir a conta). */
    fun clear() {
        sp.edit().clear().commit()
    }

    /**
     * O acesso está liberado se a última checagem no servidor disse "ativo"
     * e foi há menos de 7 dias (tolerância para ficar sem internet).
     */
    fun acessoLiberado(): Boolean = try {
        val a = get(ACESSO_KEY)?.let { JSONObject(it) }
        a != null && a.optString("status") == "ativo" &&
            System.currentTimeMillis() - a.optLong("checkedAt", 0L) < GRACE_MS
    } catch (e: Exception) {
        false
    }

    /** Mesmo formato da página: "profit-AAAA-M-D". */
    fun dayKey(cal: Calendar = Calendar.getInstance()): String =
        "profit-${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH) + 1}-${cal.get(Calendar.DAY_OF_MONTH)}"

    fun day(): JSONObject {
        val raw = get(dayKey())
        return try {
            if (raw != null) JSONObject(raw) else emptyDay()
        } catch (e: Exception) {
            emptyDay()
        }
    }

    fun config(): JSONObject? = try {
        get(CONFIG_KEY)?.let { JSONObject(it) }
    } catch (e: Exception) {
        null
    }

    fun addWater(cups: Int) {
        val d = day()
        val water = (d.optInt("water", 0) + cups).coerceIn(0, 20)
        d.put("water", water)
        set(dayKey(), d.toString())
    }

    private fun emptyDay() = JSONObject()
        .put("done", JSONObject())
        .put("sets", JSONObject())
        .put("water", 0)
        .put("supl", false)

    companion object {
        const val CONFIG_KEY = "profit-config"
        const val ACESSO_KEY = "profit-acesso"
        const val GRACE_MS = 7L * 24 * 60 * 60 * 1000
    }
}
