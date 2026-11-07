# Atlas Fit 1.0

Aplicativo Android nativo em Kotlin para treino pessoal.

## Recursos
- Fichas A, B e C originais, completas e expressas
- Calendário de 3 semanas (A, B, C completas em rodízio), começando em 09/11/2026
- Academia principal segunda, quarta e sexta; condomínio terça e sábado
- Registro local de cargas, séries, repetições, peso e cintura
- Pesquisa de vídeos demonstrativos no YouTube (não são links de vídeos previamente validados)
- Interface em português e tema escuro

## APK pelo GitHub Actions
1. Abra a aba **Actions**.
2. Selecione **Build Android APK**.
3. Após uma execução bem-sucedida, baixe o artefato **atlas-fit-debug-apk**.
4. Extraia o ZIP do artefato e instale **app-debug.apk** no Android.

O APK de debug é para testes pessoais e não foi publicado na Play Store.

## Compilação local
Requer JDK 17, Android SDK 35 e Gradle 8.9. Execute `gradle assembleDebug`.

## Limitações da versão 1.0
O histórico de cargas armazena o último valor por exercício, sem séries temporais; medidas guardam o último registro. Não há mídia incorporada nem autenticação. O plano não substitui acompanhamento de profissional de educação física.