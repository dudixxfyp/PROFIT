package com.duda.profit

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import java.util.Calendar
import java.util.Locale

class PainelWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { update(context, manager, it) }
        scheduleNext(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_WATER -> {
                val store = Store(context)
                if (store.acessoLiberado()) store.addWater(1)
                refreshAll(context)
            }
            ACTION_TICK,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> refreshAll(context)
        }
    }

    override fun onDisabled(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(tickIntent(context))
    }

    companion object {
        private const val ACTION_WATER = "com.duda.profit.WATER"
        private const val ACTION_TICK = "com.duda.profit.TICK"

        private val ROWS = intArrayOf(R.id.row1, R.id.row2, R.id.row3, R.id.row4)
        private val ROW_ICONS = intArrayOf(R.id.rowIcon1, R.id.rowIcon2, R.id.rowIcon3, R.id.rowIcon4)
        private val ROW_TEXTS = intArrayOf(R.id.rowText1, R.id.rowText2, R.id.rowText3, R.id.rowText4)

        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, PainelWidget::class.java))
            ids.forEach { update(context, manager, it) }
            if (ids.isNotEmpty()) scheduleNext(context)
        }

        private fun update(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val rv = RemoteViews(context.packageName, R.layout.widget)

            // Tocar no widget abre o app; tocar na água soma 250 ml
            val open = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            rv.setOnClickPendingIntent(R.id.root, open)
            val water = PendingIntent.getBroadcast(
                context, 1,
                Intent(context, PainelWidget::class.java).setAction(ACTION_WATER),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            rv.setOnClickPendingIntent(R.id.btnWater, water)

            val store = Store(context)
            val cfg = store.config()
            val liberado = store.acessoLiberado()
            if (cfg == null || !liberado) {
                rv.setTextViewText(R.id.icon, if (cfg == null) "👋" else "🔒")
                rv.setTextViewText(R.id.label, "PROFiT")
                rv.setTextViewText(R.id.name, if (cfg == null) "Abra o app para entrar" else "Entre no PROFiT")
                rv.setTextViewText(R.id.sub, if (cfg == null) "e montar seu plano" else "toque para confirmar seu acesso")
                rv.setTextViewText(R.id.chipDay, "PROFiT")
                rv.setTextViewText(R.id.chipMeals, "🔐 dados só no celular")
                rv.setViewVisibility(R.id.btnWater, View.GONE)
                rv.setViewVisibility(R.id.count, View.GONE)
                ROWS.forEach { rv.setViewVisibility(it, View.INVISIBLE) }
                manager.updateAppWidget(widgetId, rv)
                return
            }

            rv.setViewVisibility(R.id.btnWater, View.VISIBLE)
            val cal = Calendar.getInstance()
            val nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            val wd = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0 = domingo
            val eventos = Plano.eventos(cfg, wd)

            if (eventos.isEmpty()) {
                rv.setTextViewText(R.id.icon, "📝")
                rv.setTextViewText(R.id.label, "COMECE")
                rv.setTextViewText(R.id.name, "Monte seu plano")
                rv.setTextViewText(R.id.sub, "no app, aba Editar")
                rv.setViewVisibility(R.id.count, View.GONE)
                ROWS.forEach { rv.setViewVisibility(it, View.INVISIBLE) }
                rv.setTextViewText(R.id.chipDay, Plano.DIAS[wd])
                rv.setTextViewText(R.id.chipMeals, "🍽️ 0/0")
                manager.updateAppWidget(widgetId, rv)
                return
            }
            val day = store.day()
            val done = day.optJSONObject("done")

            var atual: Ev? = null
            for (e in eventos) if (nowMin >= e.min && nowMin < e.min + e.dur) atual = e
            val proxIdx = eventos.indexOfFirst { it.min > nowMin }
            val prox = if (proxIdx >= 0) eventos[proxIdx] else null
            val amanha = Plano.eventos(cfg, (wd + 1) % 7).firstOrNull()

            // ---- contagem regressiva até o próximo item ----
            val alvoMin: Int? = when {
                prox != null -> prox.min
                amanha != null -> 24 * 60 + amanha.min
                else -> null
            }
            if (alvoMin != null) {
                val diffMs = ((alvoMin - nowMin) * 60L - cal.get(Calendar.SECOND)) * 1000L
                rv.setViewVisibility(R.id.count, View.VISIBLE)
                rv.setChronometer(R.id.count, SystemClock.elapsedRealtime() + diffMs, null, true)
                rv.setChronometerCountDown(R.id.count, true)
            } else {
                rv.setViewVisibility(R.id.count, View.GONE)
            }

            // ---- cabeçalho ----
            when {
                atual != null -> {
                    rv.setTextViewText(R.id.icon, atual.i)
                    rv.setTextViewText(R.id.label, "AGORA · ${atual.s.uppercase(Locale("pt", "BR"))}")
                    if (prox != null) {
                        rv.setTextViewText(R.id.name, "até ${prox.n}")
                        rv.setTextViewText(R.id.sub, "às ${prox.t}")
                    } else {
                        rv.setTextViewText(R.id.name, atual.n)
                        rv.setTextViewText(R.id.sub, "último do dia")
                    }
                }
                prox != null -> {
                    rv.setTextViewText(R.id.icon, prox.i)
                    rv.setTextViewText(R.id.label, "PRÓXIMO")
                    rv.setTextViewText(R.id.name, prox.n)
                    rv.setTextViewText(R.id.sub, "às ${prox.t}")
                }
                else -> {
                    rv.setTextViewText(R.id.icon, "🌙")
                    rv.setTextViewText(R.id.label, "AMANHÃ")
                    rv.setTextViewText(R.id.name, amanha?.n ?: "Descanso")
                    rv.setTextViewText(R.id.sub, amanha?.let { "às ${it.t}" } ?: "")
                }
            }

            // ---- lista à direita: próximos itens (como a lista de apps do widget de tempo de tela) ----
            val inicio = when {
                atual != null -> eventos.indexOf(atual)
                proxIdx >= 0 -> proxIdx
                else -> eventos.size
            }
            val lista = if (inicio < eventos.size) eventos.drop(inicio).take(4)
            else Plano.eventos(cfg, (wd + 1) % 7).take(4)
            for (r in ROWS.indices) {
                val e = lista.getOrNull(r)
                if (e == null) {
                    rv.setViewVisibility(ROWS[r], View.INVISIBLE)
                    continue
                }
                rv.setViewVisibility(ROWS[r], View.VISIBLE)
                val feito = done?.optBoolean(e.id, false) == true
                rv.setTextViewText(ROW_ICONS[r], if (feito) "✅" else e.i)
                rv.setTextViewText(ROW_TEXTS[r], "${e.t}  ${e.s}")
            }

            // ---- chips ----
            val treino = Plano.treinoDoDia(cfg, wd)
            rv.setTextViewText(R.id.chipDay, "${Plano.DIAS[wd]} · " + (treino?.let { "Treino $it" } ?: "Descanso"))
            val refeicoes = eventos.filter { !it.train }
            val feitas = refeicoes.count { done?.optBoolean(it.id, false) == true }
            rv.setTextViewText(R.id.chipMeals, "🍽️ $feitas/${refeicoes.size}")
            val copos = day.optInt("water", 0)
            val litros = copos * 0.25
            val litrosTxt = String.format(Locale("pt", "BR"), if (copos % 2 == 0) "%.1f" else "%.2f", litros)
            rv.setTextViewText(R.id.btnWater, "💧 $litrosTxt L  ＋")

            manager.updateAppWidget(widgetId, rv)
        }

        private fun tickIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
            context, 2,
            Intent(context, PainelWidget::class.java).setAction(ACTION_TICK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        /** Agenda a próxima atualização: início/fim do próximo item, ou meia-noite. */
        private fun scheduleNext(context: Context) {
            val cfg = Store(context).config() ?: return
            val cal = Calendar.getInstance()
            val nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            val wd = cal.get(Calendar.DAY_OF_WEEK) - 1
            val marcos = Plano.eventos(cfg, wd)
                .flatMap { listOf(it.min, it.min + it.dur) }
                .filter { it > nowMin }
            val alvo = (marcos.minOrNull() ?: (24 * 60)).coerceAtMost(24 * 60)

            val quando = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 5)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.MINUTE, alvo)
            }
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.set(AlarmManager.RTC, quando.timeInMillis, tickIntent(context))
        }
    }
}
