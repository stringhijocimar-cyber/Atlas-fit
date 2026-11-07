# Atlas Fit

Aplicativo Android em português para acompanhar as fichas A, B e C, os treinos no condomínio e a evolução corporal. Os registros ficam no aparelho.

## Treinos

- Segunda, quarta e sexta na academia. A versão completa alterna entre A, B e C a partir de 09/11/2026. As outras duas fichas são expressas e usam exercícios da ficha original.
- Terça com cardio e mobilidade no condomínio. Sábado com cardio e pesos.
- Marque cada exercício realizado e registre carga, séries e repetições, ou minutos para atividades por tempo.
- O relatório mostra dias ativos, sessões por modalidade e detalhes dos exercícios. Use os filtros de 7 dias, 30 dias ou todo o histórico.
- Os botões de execução abrem vídeos específicos no YouTube, com o canal identificado na tela.

## Bioimpedância

Acompanhe peso, percentual de gordura e massa adiposa em gráficos. O histórico também aceita cintura, IMC, água, massa muscular, massa livre de gordura, nível de gordura visceral e metabolismo basal. Registre as unidades como aparecem no laudo.

Para importar avaliações, abra **Evolução → Importar histórico de avaliações** e selecione um JSON com uma lista de avaliações. Exemplo de estrutura:

```json
[
  {"date":"2026-10-01","source":"Minha balança","weight":80.0,"fat":20.0,"fatKg":16.0}
]
```

Use uma data ISO e pelo menos um indicador. Os demais campos são `leanKg`, `muscle`, `water`, `waist`, `bmi`, `visceralLevel` e `bmrKcal`. As unidades opcionais `muscleUnit` e `waterUnit` aceitam `%` ou `kg`. `muscleUnit` também aceita `kg esquelética`. Datas e origens já registradas são ignoradas durante a importação.

## APK Android

[Baixar a versão publicada](https://github.com/stringhijocimar-cyber/Atlas-fit/releases/latest/download/atlas-fit.apk)

O APK de teste é destinado à instalação inicial. Uma atualização de uma instalação existente depende de ambas as versões terem a mesma assinatura.

## Compilação

Requer Android SDK 35, JDK 17 e Gradle 8.9.

```sh
gradle testDebugUnitTest assembleDebug
```

O APK local fica em `app/build/outputs/apk/debug/app-debug.apk`.

## Interface 2.0

A aba **Exercícios** reúne ilustrações em duas fases, músculos-alvo, instruções e links diretos para demonstrações. As fichas e as ilustrações são locais. O YouTube usa conexão à internet.

No modo treino, registre repetições e carga por série. Marcar uma série inicia o descanso configurado. O treino em andamento é salvo no aparelho, incluindo o cronômetro e os campos preenchidos. Finalize para gravar a sessão no histórico e consultar a evolução de carga por exercício.


## Imagens e vídeos na versão 2.1

A biblioteca e o modo treino usam ilustrações 3D geradas para o Atlas Fit. A galeria alterna as duas posições em sequência e permite pausar em qualquer uma delas. Verde identifica os músculos principais e âmbar identifica os auxiliares. A imagem do equipamento é uma referência visual com instruções de ajuste.

Os vídeos são links externos. A seleção prioriza Leandro Twin quando a demonstração corresponde à variante da ficha. Os créditos aparecem junto a cada link.

Referências para os grupos musculares e as variantes: [biblioteca de exercícios da ACE](https://www.acefitness.org/resources/everyone/exercise-library/), [biblioteca de exercícios da NASM](https://www.nasm.org/workout-exercise-guidance) e [estudo de ativação muscular em ângulos de supino](https://pmc.ncbi.nlm.nih.gov/articles/PMC7579505/).
