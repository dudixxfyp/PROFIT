package com.duda.profit

import org.json.JSONObject

data class Ev(
    val id: String,
    val t: String,
    val n: String,
    val s: String,
    val i: String,
    val train: Boolean
) {
    val min: Int get() = Plano.toMin(t)
    val dur: Int get() = when (id) {
        "treino" -> 70
        "cardio" -> 20
        else -> 30
    }
}

/** Mesma lógica de horários da página. */
object Plano {
    val DIAS = arrayOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb")

    fun toMin(t: String): Int {
        val p = t.split(":")
        val h = p.getOrNull(0)?.trim()?.toIntOrNull() ?: 0
        val m = p.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
        return h * 60 + m
    }

    /** wd: 0 = domingo ... 6 = sábado. Retorna a letra do treino ou null se for descanso. */
    fun treinoDoDia(cfg: JSONObject, wd: Int): String? {
        val k = cfg.optJSONObject("plan")?.optString(wd.toString(), "") ?: ""
        return if (k.isNotEmpty() && cfg.optJSONObject("wk")?.has(k) == true) k else null
    }

    fun eventos(cfg: JSONObject, wd: Int): List<Ev> {
        val arr = cfg.optJSONArray("meals") ?: return emptyList()
        val diaDeTreino = treinoDoDia(cfg, wd) != null
        val out = mutableListOf<Ev>()
        for (idx in 0 until arr.length()) {
            val o = arr.optJSONObject(idx) ?: continue
            val train = o.optBoolean("train", false)
            if (train && !diaDeTreino) continue
            val nome = o.optString("n", "")
            out += Ev(
                id = o.optString("id", "item$idx"),
                t = o.optString("t", "00:00"),
                n = nome,
                s = o.optString("s", nome).ifBlank { nome },
                i = o.optString("i", "•").ifBlank { "•" },
                train = train
            )
        }
        return out.sortedBy { it.min }
    }
}
