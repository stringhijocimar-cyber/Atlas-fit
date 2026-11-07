package br.com.atlasfit

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.graphics.Color
import android.widget.*
import android.text.InputType
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {
 private lateinit var root: LinearLayout
 private val navy = Color.rgb(13,18,29)
 private val blue = Color.rgb(75,164,255)
 private val white = Color.WHITE
 private val prefs by lazy { getSharedPreferences("atlas_fit", MODE_PRIVATE) }
 private val exercises = mapOf(
 "A" to listOf("Fly pegada neutra","Supino convergente neutro","Supino barra banco inclinado","Supino reto barra","Desenvolvimento máquina","Elevação frontal halter","Tríceps pulley crossover barra polia alta","Tríceps francês crossover barra polia alta","Caminhada na esteira"),
 "B" to listOf("Extensora cadeira","Agachamento rack","Leg press horizontal máquina","Cadeira flexora máquina","Flexora mesa","Adutor cadeira","Banco sóleo","Caminhada na esteira"),
 "C" to listOf("Puxada aberta","Remada baixa triângulo","Remada baixa pegada aberta","Remada convergente neutra","Fly inverso","Encolhimento halter","Rosca martelo duplo halter banco inclinado","Rosca direta máquina","Bicicleta") )
 private val express = mapOf("A" to listOf(0,1,2,6),"B" to listOf(0,1,2,3),"C" to listOf(0,1,4,6))
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); home() }
 private fun screen(title:String) { root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(22,25,22,24); setBackgroundColor(navy) }; val scroll=ScrollView(this); scroll.addView(root); setContentView(scroll); label(title,26); button("⌂ Início") { home() } }
 private fun label(t:String,size:Int=17) { root.addView(TextView(this).apply { text=t; textSize=size.toFloat(); setTextColor(white); setPadding(0,10,0,10) }) }
 private fun button(t:String,action:()->Unit) { root.addView(Button(this).apply { text=t; setTextColor(Color.BLACK); setBackgroundTintList(android.content.res.ColorStateList.valueOf(blue)); setOnClickListener { action() } }) }
 private fun input(hint:String,value:String=""):EditText { val e=EditText(this); e.hint=hint; e.setText(value); e.setTextColor(white); e.setHintTextColor(Color.LTGRAY); e.inputType=InputType.TYPE_CLASS_TEXT; root.addView(e); return e }
 private fun week():Int { val start=Calendar.getInstance().apply { set(2026,10,9,0,0,0); set(Calendar.MILLISECOND,0) }; val delta=(System.currentTimeMillis()-start.timeInMillis)/(7L*86400000L); return ((delta.coerceAtLeast(0)%3)+1).toInt() }
 private fun home() { screen("ATLAS FIT 1.0"); label("Seu treino. Sua evolução."); label("Semana do ciclo: ${week()} · Ficha completa: ${listOf("A","B","C")[week()-1]}"); button("Calendário semanal") { calendar() }; listOf("A","B","C").forEach { f->button("Ficha $f — completa") { workout(f,true) }; button("Ficha $f — expressa") { workout(f,false) } }; button("Treino do condomínio") { condo() }; button("Registrar medidas") { metrics() }; button("Histórico") { history() } }
 private fun calendar() { screen("Calendário de rodízio"); val full=listOf("A","B","C")[week()-1]; label("Segunda: A — ${if(full=="A") "completa" else "expressa"}"); label("Terça: condomínio — cardio leve"); label("Quarta: B — ${if(full=="B") "completa" else "expressa"}"); label("Quinta: descanso"); label("Sexta: C — ${if(full=="C") "completa" else "expressa"}"); label("Sábado: condomínio — cardio e pesos leves"); label("Domingo: descanso"); for(w in 1..6) { val f=listOf("A","B","C")[(w-1)%3]; label("Semana $w · Completa $f · demais expressas") }; label("Ciclo inicia em 09/11/2026 e repete a cada 3 semanas.") }
 private fun workout(f:String,full:Boolean) { screen("Ficha $f — ${if(full) "Completa" else "Expressa"}"); label(if(full) "Ajuste o volume à recuperação; descanso de 60–120 s." else "Até 30 min; aquecimento e intervalos controlados."); val ids=if(full) exercises[f]!!.indices.toList() else express[f]!!; ids.forEach { idx -> val ex=exercises[f]!![idx]; label("${idx+1}. $ex",19); val key="$f-$ex"; val prev=prefs.getString(key,"")?:""; val edit=input("Carga (kg) / séries x repetições",prev); button("Salvar registro") { prefs.edit().putString(key,edit.text.toString()).apply(); Toast.makeText(this,"Registro salvo",Toast.LENGTH_SHORT).show() }; button("▶ Ver vídeo demonstrativo") { val url="https://www.youtube.com/results?search_query="+Uri.encode("Leandro Twin execução $ex musculação"); startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url))) } } }
 private fun condo() { screen("Condomínio"); label("Terça: 20 min de esteira ou bicicleta + 5–10 min de mobilidade."); label("Sábado: 10–15 min de bicicleta + 15 min de pesos leves, se recuperado."); label("Não é necessário forçar treino se houver dor importante ou fadiga.") }
 private fun metrics() { screen("Evolução corporal"); val weight=input("Peso em kg",prefs.getString("weight","86")?:"86"); val waist=input("Cintura em cm",prefs.getString("waist","")?:""); button("Salvar medidas") { prefs.edit().putString("weight",weight.text.toString()).putString("waist",waist.text.toString()).putString("last",SimpleDateFormat("dd/MM/yyyy",Locale("pt","BR")).format(Date())).apply(); Toast.makeText(this,"Medidas salvas",Toast.LENGTH_SHORT).show() } }
 private fun history() { screen("Histórico de registros"); label("Peso: ${prefs.getString("weight","—")} kg · Cintura: ${prefs.getString("waist","—")} cm · Atualizado: ${prefs.getString("last","—")}"); exercises.forEach { (f,items)->items.forEach { ex-> val v=prefs.getString("$f-$ex",""); if(!v.isNullOrBlank()) label("$f · $ex: $v") } } }
}