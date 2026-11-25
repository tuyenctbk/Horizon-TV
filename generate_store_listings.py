import csv
import os
import shutil

# List of 20 languages with localized Google Play store listing metadata
# Constraints (Google Play Policy Compliance):
# - app_name <= 30 chars (no emojis, no ALL CAPS, no promotional buzzwords)
# - short_description <= 80 chars
# - full_description <= 4000 chars

store_listings = [
    {
        "language_code": "en-US",
        "language_name": "English (United States)",
        "app_name": "Horizon TV - Weather Radar",
        "short_description": "24/7 weather TV, 7-day forecast, Doppler radar, AI briefing & storm alerts.",
        "full_description": """Horizon TV brings 24/7 broadcast television weather directly to your Android phone, tablet, and Android TV. Experience live meteorological telemetry, an iconic 24/7 Broadcast L-Bar news ticker, 7-day extended forecasts, interactive multi-layer Doppler radar, an AI Chief Meteorologist Desk, and background severe storm push alerts in a studio-grade interface.

Key Features:
- 24/7 Live Broadcast View & L-Bar Ticker: Iconic television L-Bar layout with a live local clock, breaking weather alert badges, scrolling news ticker marquee, and 12-hour synoptic quick strip.
- 7-Day Extended Forecast & Recharts Trends: Live Open-Meteo 7-day highs/lows with thermal range bars, expandable daily synoptic breakdowns, plus 5-day and 24-hour temperature and precipitation (POP %) curves.
- AI Meteorologist Broadcast Desk: Gemini-powered 1-tap Live Broadcast Briefings, Outfit & Commute Guide, Streaming Atmospheric Q&A, and TTS Voice Anchor—protected by Smart Free-Tier Guardrails (Eco Lite Mode, +3 Daily Grace Credits, 0-Credit On-Device Local Meteorology Engine, and optional BYOK key support).
- Interactive Multi-Layer Doppler Radar: High-resolution RainViewer Doppler sweep with precipitation reflectivity, infrared satellite, wind streamlines, street map overlay, and point inspection.
- Background Severe Weather Push Alerts: Periodic WorkManager background monitoring dispatches high-priority push notifications for severe thunderstorms, gale winds, extreme heat, hard freezes, and hazardous air quality (AQI).
- Real-Time Micro-Sensor Telemetry & VOD Archive: Live temperature, feels-like, humidity, dew point, barometric pressure, UV index, visibility, synodic lunar phase, and telemetry-driven Video On Demand (VOD) dispatches.
- Studio Onboarding, Accessibility & 20 Languages: Interactive 4-step Broadcast Studio Onboarding Tour, native Android TV D-Pad & virtual remote navigation, Picture-in-Picture (PiP) mode, CRT scanline aesthetics, full TalkBack/WCAG accessibility, and 20-language localization.

Stay informed with Horizon TV—your studio command center for continuous, accurate, and accessible weather intelligence."""
    },
    {
        "language_code": "es-ES",
        "language_name": "Spanish (Spain)",
        "app_name": "Horizon TV: Clima y Radar",
        "short_description": "TV del clima 24/7, pronóstico 7 días, radar Doppler, IA y alertas de tormenta.",
        "full_description": """Horizon TV lleva la televisión meteorológica 24/7 directamente a su teléfono Android, tablet y Android TV. Disfrute de telemetría en tiempo real, barra L de transmisión con teletipo de noticias, pronóstico extendido de 7 días, radar Doppler multicapa, Mesa de Meteorólogo IA y alertas push en segundo plano.

Características principales:
- Transmisión en Vivo 24/7 y Barra L: Diseño televisivo en L con reloj local en vivo, insignias de alerta urgente, teletipo meteorológico continuo y franja sinóptica de 12 horas.
- Pronóstico de 7 Días y Tendencias Recharts: Máximas y mínimas de 7 días con Open-Meteo, barras de rango térmico, desglose diario desplegable y curvas de 5 días y 24 horas (POP %).
- Mesa de Meteorólogo IA: Boletines en vivo con 1 toque impulsados por Gemini, guía de vestimenta y transporte, preguntas y respuestas en streaming y locutor de voz TTS, con Modo Eco Lite, +3 créditos diarios de gracia, motor meteorológico local sin costo y soporte opcional BYOK.
- Radar Doppler Interactivo Multicapa: Barrido RainViewer en alta resolución con reflectividad de lluvia, satélite infrarrojo, vectores de viento y mapa de calles.
- Alertas Push de Clima Severo en Segundo Plano: Monitoreo periódico con WorkManager que envía notificaciones de alta prioridad por tormentas severas, vendavales, calor extremo, heladas y calidad del aire (AQI).
- Telemetría en Tiempo Real y Archivo VOD: Temperatura, sensación térmica, humedad, punto de rocío, presión barométrica, índice UV, fase lunar y boletines VOD dinámicos.
- Tour de Estudio, Accesibilidad y 20 Idiomas: Tour interactivo de bienvenida de 4 pasos, control remoto para Android TV, modo Picture-in-Picture (PiP), efectos CRT, accesibilidad completa TalkBack/WCAG y soporte en 20 idiomas."""
    },
    {
        "language_code": "fr-FR",
        "language_name": "French (France)",
        "app_name": "Horizon TV: Météo Radar",
        "short_description": "TV météo 24/7, prévisions 7 jours, radar Doppler, briefing IA et alertes.",
        "full_description": """Horizon TV apporte la télévision météo en continu 24h/24 et 7j/7 sur votre smartphone Android, tablette et Android TV. Profitez de la télémétrie en direct, du bandeau L-Bar avec téléscripteur, des prévisions à 7 jours, du radar Doppler multicouche, du Bureau Météo IA et des alertes tempête en arrière-plan.

Caractéristiques principales :
- Diffusion Direct 24/7 et Bandeau L-Bar : Disposition TV emblématique avec horloge locale, badges d'alerte, bandeau défilant d'actualités météo et aperçu horaire sur 12h.
- Prévisions à 7 Jours et Tendances Recharts : Températures max/min Open-Meteo sur 7 jours avec barres thermiques, détails quotidiens extensibles et courbes sur 5 jours et 24 heures (POP %).
- Bureau du Météorologue IA : Briefings météo en 1 geste avec Gemini, conseils tenue et trajet, Q&R en streaming et présentateur vocal TTS—avec mode Eco Lite, +3 crédits quotidiens, moteur météo local hors ligne à 0 crédit et clé BYOK optionnelle.
- Radar Doppler Interactif Multicouche : Balayage RainViewer haute résolution avec réflectivité des précipitations, satellite IR, flux de vent et carte routière.
- Alertes Push Météo Grave (WorkManager) : Surveillance périodique en arrière-plan avec notifications prioritaires pour orages violents, rafales, canicule, gel et qualité de l'air (AQI).
- Télémétrie Temps Réel et VOD : Température, ressenti, humidité, point de rosée, pression, indice UV, cycle lunaire et reportages VOD dynamiques.
- Visite Guidée, Accessibilité et 20 Langues : Parcours d'accueil interactif en 4 étapes, navigation télécommande Android TV, mode PiP, filtre CRT, accessibilité TalkBack/WCAG complète et 20 langues."""
    },
    {
        "language_code": "de-DE",
        "language_name": "German (Germany)",
        "app_name": "Horizon TV: Wetter Radar",
        "short_description": "24/7 Wetter-TV, 7-Tage-Trend, Doppler-Radar, KI-Briefing & Unwetterwarnungen.",
        "full_description": """Horizon TV bringt 24/7-Wetterfernsehen direkt auf Ihr Android-Smartphone, Tablet und Android TV. Erleben Sie Live-Telemetrie, das ikonische L-Bar-Nachrichtenband, 7-Tage-Wettervorhersagen, interaktiven Mehrschicht-Doppler-Radar, das KI-Meteorologen-Studio und Hintergrund-Unwetterwarnungen.

Hauptmerkmale:
- 24/7 Live-TV & L-Bar-Ticker: Klassisches TV-L-Bar-Layout mit lokaler Live-Uhr, Eilmeldungs-Badges, laufendem Wetternachrichten-Ticker und 12-Stunden-Leiste.
- 7-Tage-Vorhersage & Recharts-Trends: Open-Meteo 7-Tage-Höchst-/Tiefstwerte mit Wärmebalken, aufklappbarer Tagesanalyse sowie 5-Tage- und 24-Stunden-Temperatur- und Regenkurven (POP %).
- KI-Meteorologen-Studio: Gemini-gestützte 1-Tap-Live-Briefings, Kleidungs- & Pendler-Tipps, Streaming-Q&A und TTS-Sprachmoderator—inklusive Eco-Lite-Modus, +3 täglichen Kulanz-Credits, kostenfreier lokaler Wetter-Engine und optionalem BYOK-Schlüssel.
- Interaktiver 4-Schichten-Doppler-Radar: RainViewer-Radar mit Niederschlagsreflektivität, Infrarot-Satellit, Windströmungen und Straßenkarte.
- Hintergrund-Unwetter-Push-Warnungen: Periodische WorkManager-Überwachung sendet Prioritäts-Benachrichtigungen bei Gewitterstürmen, Orkanböen, extremer Hitze, Frost und Luftqualität (AQI).
- Echtzeit-Sensor-Telemetrie & VOD-Archiv: Temperatur, gefühlte Temperatur, Luftfeuchtigkeit, Taupunkt, Luftdruck, UV-Index, Mondphase und dynamische VOD-Berichte.
- Studio-Onboarding, Barrierefreiheit & 20 Sprachen: Interaktive 4-Schritte-Einführungstour, Android TV-Fernbedienung, Bild-in-Bild (PiP), CRT-Effekt, volle TalkBack/WCAG-Unterstützung und 20 Sprachen."""
    },
    {
        "language_code": "it-IT",
        "language_name": "Italian (Italy)",
        "app_name": "Horizon TV: Meteo & Radar",
        "short_description": "TV meteo 24/7, previsioni 7 giorni, radar Doppler, briefing IA e allerte.",
        "full_description": """Horizon TV porta la televisione meteorologica 24/7 direttamente su smartphone Android, tablet e Android TV. Scopri telemetria in tempo reale, barra L-Bar con ticker di notizie, previsioni estese a 7 giorni, radar Doppler multistrato, Desk Meteorologo IA e notifiche push per maltempo in background.

Caratteristiche principali:
- Diretta TV 24/7 e Ticker L-Bar: Iconico layout televisivo a L con orologio locale, badge di allerta, ticker scorrevole continuo e fascia oraria di 12 ore.
- Previsioni a 7 Giorni e Grafici Recharts: Massime e minime Open-Meteo a 7 giorni con barre termiche, dettagli giornalieri espandibili e curve a 5 giorni e 24 ore (POP %).
- Desk Meteorologo IA: Briefing meteo in 1 tocco con Gemini, consigli abbigliamento e pendolari, Q&A in streaming e conduttore vocale TTS—con modalità Eco Lite, +3 crediti giornalieri extra, motore meteorologico locale a 0 crediti e supporto BYOK.
- Radar Doppler Interattivo Multistrato: Scansione RainViewer ad alta risoluzione con riflettività pioggia, satellite IR, flussi del vento e mappa stradale.
- Allerte Push Maltempo in Background: Monitoraggio periodico WorkManager con notifiche ad alta priorità per temporali, raffiche di vento, ondate di calore, gelate e qualità dell'aria (AQI).
- Telemetria in Tempo Reale e VOD: Temperatura, percepita, umidità, punto di rugiada, pressione, indice UV, fasi lunari e servizi VOD dinamici.
- Tour Onboarding, Accessibilità e 20 Lingue: Tour guidato interattivo in 4 passaggi, telecomando Android TV, modalità Picture-in-Picture (PiP), filtro CRT, piena accessibilità TalkBack/WCAG e 20 lingue."""
    },
    {
        "language_code": "pt-BR",
        "language_name": "Portuguese (Brazil)",
        "app_name": "Horizon TV: Clima e Radar",
        "short_description": "TV de clima 24/7, previsão 7 dias, radar Doppler, briefing IA e alertas.",
        "full_description": """O Horizon TV traz a televisão meteorológica 24/7 diretamente para o seu celular Android, tablet e Android TV. Acompanhe telemetria ao vivo, barra L-Bar com ticker de notícias, previsão estendida de 7 dias, radar Doppler multicamadas, Bancada do Meteorologista IA e alertas de tempestade em segundo plano.

Principais recursos:
- Transmissão ao Vivo 24/7 e Ticker L-Bar: Layout icônico em barra L com relógio local, selos de alerta urgente, letreiro deslizante de notícias e faixa de 12 horas.
- Previsão de 7 Dias e Tendências Recharts: Máximas e mínimas Open-Meteo para 7 dias com barras térmicas, detalhes diários expansíveis e curvas de 5 dias e 24 horas (POP %).
- Bancada do Meteorologista IA: Boletins ao vivo em 1 toque com Gemini, guia de roupas e deslocamento, chat meteorológico e âncora de voz TTS—com Modo Eco Lite, +3 créditos diários de cortesia, motor local de 0 créditos e chave BYOK opcional.
- Radar Doppler Interativo de 4 Camadas: Varredura RainViewer de alta resolução com refletividade de chuva, satélite IR, correntes de vento e mapa de ruas.
- Alertas Push de Tempestade (WorkManager): Monitoramento periódico em segundo plano com notificações prioritárias para tempestades severas, vendavais, calor extremo, geada e qualidade do ar (AQI).
- Telemetria em Tempo Real e VOD: Temperatura, sensação térmica, umidade, ponto de orvalho, pressão, índice UV, fases da lua e vídeos VOD dinâmicos.
- Tour de Boas-Vindas, Acessibilidade e 20 Idiomas: Tour interativo de 4 etapas, controle remoto Android TV, modo PiP, efeito CRT, acessibilidade completa TalkBack/WCAG e 20 idiomas."""
    },
    {
        "language_code": "ja-JP",
        "language_name": "Japanese (Japan)",
        "app_name": "Horizon TV: お天気＆レーダー",
        "short_description": "24時間ライブ気象TV、7日間予報、ドップラーレーダー、AI解説と気象警報。",
        "full_description": """Horizon TVは、24時間365日放送の気象テレビ番組をAndroidスマートフォン、タブレット、Android TVにお届けします。リアルタイム気象テレメトリ、ニュース番組風L字バー＆ティッカー、7日間週間予報、多層ドップラーレーダー、AI気象キャスターデスク、バックグラウンド気象警報通知をスタジオ品質のUIで体験できます。

主な機能：
- 24/7 ライブ放送＆Lバー・ティッカー：現地時計、緊急警報バッジ、スクロール気象ニュースティッカー、12時間予報ストリップを備えた本格L字放送レイアウト。
- 7日間週間予報＆Rechartsトレンド：Open-Meteoによる7日間の最高/最低気温レンジバー、日別詳細展開、5日間・24時間の気温＆降水確率（POP %）グラフ。
- AI気象キャスターデスク：Gemini搭載の1タップ生放送ブリーフィング、服装・通勤ガイド、対話型Q&A、TTS音声アナウンサー機能（Eco Liteモード、毎日+3無料追加クレジット、0クレジットの端末内ローカル気象エンジン、BYOKキー対応）。
- インタラクティブ4層ドップラーレーダー：RainViewer高解像度雨雲レーダー、赤外線衛星、風向ストリームライン、道路マップの切り替えと地点詳細検査。
- バックグラウンド気象警報プッシュ通知：WorkManagerによる定期監視で、激しい雷雨、暴風、猛暑、厳冬凍結、大気汚染（AQI）を即座に高優先度通知。
- リアルタイム微気象データ＆VOD：気温、体感温度、湿度、露点、気圧、UV指数、月相、最新テレメトリ連動VODレポート。
- スタジオ案内ツアー・アクセシビリティ・20言語対応：4ステップの初回オンボーディングツアー、Android TVリモコン対応、PiPモード、CRT走査線エフェクト、TalkBack/WCAG完全準拠、20言語ローカライズ。"""
    },
    {
        "language_code": "ko-KR",
        "language_name": "Korean (South Korea)",
        "app_name": "Horizon TV: 날씨 & 레이더",
        "short_description": "24/7 실시간 날씨 TV, 7일 예보, 도플러 레이더, AI 브리핑 및 기상 특보.",
        "full_description": """Horizon TV는 24시간 전문 기상 방송을 Android 스마트폰, 태블릿 및 Android TV로 전달합니다. 실시간 기상 텔레메트리, 뉴스 채널 스타일 L-Bar 티커, 7일 주간 예보, 다층 도플러 레이더, AI 기상캐스터 데스크 및 백그라운드 기상 특보 푸시 알림을 스튜디오급 인터페이스로 경험하세요.

주요 기능:
- 24/7 실시간 방송 및 L-Bar 뉴스 티커: 현지 시계, 긴급 특보 배지, 실시간 스크롤 뉴스 티커, 12시간 예보 스트립을 갖춘 방송용 L자형 레이아웃.
- 7일 확장 예보 및 Recharts 트렌드: Open-Meteo 7일 최고/최저 기온 온도 막대, 일별 상세 분석, 5일 및 24시간 기온·강수 확률(POP %) 차트.
- AI 기상캐스터 브로드캐스트 데스크: Gemini 기반 원탭 라이브 기상 브리핑, 옷차림 및 출퇴근 가이드, 실시간 Q&A, TTS 음성 앵커 지원(Eco Lite 모드, 매일 +3 무료 보너스 크레딧, 0크레딧 온디바이스 로컬 엔진 및 BYOK 개인 키 지원).
- 대화형 4계층 도플러 레이더: RainViewer 강수 반사도, 적외선 위성, 바람 벡터 및 거리 지도 레이어 지원.
- 백그라운드 기상 특보 푸시 알림: WorkManager 주기적 감지를 통해 호우·뇌우, 강풍, 폭염, 한파 및 대기질(AQI) 경보를 고우선순위로 즉시 알림.
- 실시간 미세 기상 데이터 및 VOD: 기온, 체감 온도, 습도, 이슬점, 기압, UV 지수, 달의 위상 및 실시간 데이터 기반 VOD 리포트.
- 스튜디오 온보딩 투어, 접근성 및 20개 국어: 4단계 대화형 온보딩 가이드, Android TV 리모컨 탐색, PIP 모드, CRT 효과, TalkBack/WCAG 완벽 준수 및 20개 언어 지원."""
    },
    {
        "language_code": "zh-CN",
        "language_name": "Chinese (Simplified)",
        "app_name": "Horizon TV: 天气与雷达",
        "short_description": "24/7 直播天气 TV、7 天预报、多普勒雷达、AI 气象主播与恶劣天气推送。",
        "full_description": """Horizon TV 将 24/7 全天候广播电视天气节目带到您的 Android 手机、平板电脑和 Android TV。在专业演播室级界面中体验实时气象遥测、经典新闻台 L-Bar 滚动字幕条、7 天延伸预报、交互式多层多普勒雷达、AI 首席气象主播台以及后台恶劣天气推送警报。

核心功能：
- 24/7 全天候直播与 L-Bar 滚动资讯条：新闻频道标志性 L 型边框，集成当地实时时钟、突发预警徽章、滚动天气快讯与 12 小时预报条。
- 7 天天气预报与 Recharts 趋势图表：Open-Meteo 7 天最高/最低温差条、可展开的每日详尽气象分析，以及 5 天和 24 小时气温与降水概率 (POP %) 曲线。
- AI 气象主播播报台：由 Gemini 驱动的一键直播气象简报、穿衣与通勤指南、流式问答与 TTS 语音播报——配备智能免费额度护栏（Eco Lite 轻量模式、每日 +3 免费宽限额度、0 额度本地气象引擎及可选 BYOK 自定义密钥）。
- 交互式四层多普勒雷达：RainViewer 高清降水反射率扫描、红外卫星云图、风场流线与街道地图叠加。
- WorkManager 后台恶劣天气推送：定期后台监测雷暴、强风、极端高温、寒潮冰冻及空气质量 (AQI) 并发送高优先级通知。
- 实时微气象遥测与 VOD 点播：气温、体感温度、湿度、露点、气压、紫外线指数、月相周期及实时数据驱动的 VOD 专题报道。
- 演播室新手导览、无障碍与 20 种语言：4 步交互式首次启动导览、Android TV 遥控器支持、画中画 (PiP)、复古 CRT 扫描线、完整 TalkBack/WCAG 无障碍支持及 20 国语言本地化。"""
    },
    {
        "language_code": "zh-TW",
        "language_name": "Chinese (Traditional)",
        "app_name": "Horizon TV: 天氣與雷達",
        "short_description": "24/7 直播天氣 TV、7 天預報、都卜勒雷達、AI 氣象主播與劇烈天氣推播。",
        "full_description": """Horizon TV 將 24/7 全天候廣播電視天氣節目帶到您的 Android 手機、平板與 Android TV。在專業攝影棚級介面中體驗即時氣象遙測、新聞台 L-Bar 跑馬燈資訊列、7 天延伸預報、互動式多層都卜勒雷達、AI 首席氣象主播台與背景劇烈天氣推播警報。

核心功能：
- 24/7 全天候直播與 L-Bar 跑馬燈：新聞頻道經典 L 型邊框，整合當地即時時鐘、突發警報徽章、滾動天氣快訊與 12 小時預報條。
- 7 天天氣預報與 Recharts 趨勢圖表：Open-Meteo 7 天最高/最低溫差條、可展開的每日詳盡氣象分析，以及 5 天與 24 小時氣溫與降雨機率 (POP %) 曲線。
- AI 氣象主播播報台：由 Gemini 驅動的一鍵直播氣象簡報、穿搭與通勤指南、串流問答與 TTS 語音主播——具備智慧免費額度護欄（Eco Lite 輕量模式、每日 +3 免費寬限額度、0 額度本機氣象引擎及選用 BYOK 自訂金鑰）。
- 互動式四層都卜勒雷達：RainViewer 高解析度降雨反射率掃描、紅外線衛星雲圖、風場流線與街道地圖疊加。
- WorkManager 背景劇烈天氣推播：定期於背景監測雷暴、強風、極端高溫、寒害冰凍與空氣品質 (AQI) 並發送高優先級通知。
- 即時微氣象遙測與 VOD 影音：氣溫、體感溫度、濕度、露點、氣壓、紫外線指數、月相週期及即時數據驅動的 VOD 專題報導。
- 攝影棚新手導覽、無障礙與 20 種語言：4 步互動式導覽體驗、Android TV 遙控器導覽、子母畫面 (PiP)、復古 CRT 掃描線、完整 TalkBack/WCAG 無障礙支援與 20 國語言本地化。"""
    },
    {
        "language_code": "vi-VN",
        "language_name": "Vietnamese (Vietnam)",
        "app_name": "Horizon TV: Thời Tiết Radar",
        "short_description": "TV thời tiết 24/7, dự báo 7 ngày, radar Doppler, bản tin AI & cảnh báo bão.",
        "full_description": """Horizon TV mang truyền hình thời tiết phát sóng 24/7 trực tiếp đến điện thoại Android, máy tính bảng và Android TV của bạn. Trải nghiệm dữ liệu khí tượng thời gian thực, thanh tin tức L-Bar 24/7, dự báo 7 ngày, radar Doppler đa lớp, Bàn Biên Tập Khí Tượng AI và thông báo đẩy cảnh báo bão chạy nền.

Các tính năng nổi bật:
- Truyền Hình Trực Tiếp 24/7 & Thanh Tin Tức L-Bar: Bố cục chữ L chuẩn truyền hình với đồng hồ địa phương, huy hiệu cảnh báo khẩn, dòng chữ chạy liên tục và dải dự báo 12 giờ.
- Dự Báo 7 Ngày & Biểu Đồ Recharts: Nhiệt độ cao/thấp 7 ngày từ Open-Meteo với thanh dải nhiệt, chi tiết từng ngày mở rộng, cùng biểu đồ nhiệt độ và xác suất mưa (POP %) 5 ngày & 24 giờ.
- Bàn Biên Tập Khí Tượng AI: Bản tin thời tiết trực tiếp 1 chạm với Gemini, tư vấn trang phục & di chuyển, hỏi đáp trực tuyến và phát thanh viên giọng nói TTS—tích hợp chế độ Eco Lite, +3 lượt miễn phí mỗi ngày, bộ máy khí tượng nội bộ 0 tốn lượt và hỗ trợ khóa BYOK.
- Radar Doppler Tương Tác 4 Lớp: Quét radar RainViewer độ phân giải cao với độ phản xạ mưa, vệ tinh hồng ngoại, luồng gió và bản đồ đường phố.
- Cảnh Báo Thời Tiết Nguy Hiểm Chạy Nền (WorkManager): Tự động kiểm tra định kỳ và gửi thông báo đẩy ưu tiên cao khi có dông bão, gió giật, nắng nóng, băng giá và chất lượng không khí (AQI).
- Telemetry Thời Gian Thực & Kho VOD: Nhiệt độ, cảm giác thực, độ ẩm, điểm sương, áp suất, chỉ số UV, chu kỳ mặt trăng và bản tin VOD động.
- Hướng Dẫn Studio, Trợ Năng & 20 Ngôn Ngữ: Tour giới thiệu 4 bước tương tác, điều khiển Android TV, chế độ Hình trong Hình (PiP), hiệu ứng CRT, hỗ trợ đầy đủ TalkBack/WCAG và 20 ngôn ngữ."""
    },
    {
        "language_code": "id-ID",
        "language_name": "Indonesian (Indonesia)",
        "app_name": "Horizon TV: Cuaca & Radar",
        "short_description": "TV cuaca 24/7, prakiraan 7 hari, radar Doppler, briefing AI & peringatan.",
        "full_description": """Horizon TV menghadirkan siaran televisi cuaca 24/7 langsung ke ponsel Android, tablet, dan Android TV Anda. Nikmati telemetri meteorologi langsung, ticker berita L-Bar 24/7, prakiraan 7 hari, radar Doppler interaktif multi-lapis, Meja Meteorolog AI, dan notifikasi peringatan cuaca ekstrem di latar belakang.

Fitur Utama:
- Siaran Langsung 24/7 & Ticker L-Bar: Tata letak bilah L ikonik dengan jam lokal langsung, lencana peringatan darurat, teks berjalan berita cuaca, dan strip prakiraan 12 jam.
- Prakiraan 7 Hari & Grafik Recharts: Suhu tertinggi/terendah 7 hari Open-Meteo dengan bilah rentang termal, rincian harian yang dapat diperluas, serta kurva suhu dan peluang hujan (POP %) 5 hari & 24 jam.
- Meja Meteorolog AI: Briefing cuaca langsung 1 ketukan bertenaga Gemini, panduan pakaian & perjalanan, tanya jawab streaming, dan pembaca berita suara TTS—lengkap dengan Mode Eco Lite, +3 kredit ekstra harian, mesin meteorologi lokal 0 kredit, dan opsi kunci BYOK.
- Radar Doppler Interaktif 4 Lapis: Pemindaian RainViewer resolusi tinggi dengan reflektivitas hujan, satelit IR, arus angin, dan peta jalan.
- Peringatan Push Cuaca Buruk Latar Belakang: Pemantauan berkala WorkManager mengirimkan notifikasi prioritas tinggi untuk badai petir, angin kencang, panas ekstrem, pembekuan, dan kualitas udara (AQI).
- Telemetri Waktu Nyata & Arsip VOD: Suhu, terasa seperti, kelembapan, titik embun, tekanan udara, indeks UV, fase bulan, dan laporan VOD dinamis.
- Tur Onboarding Studio, Aksesibilitas & 20 Bahasa: Tur pengenalan interaktif 4 langkah, navigasi remote Android TV, mode PiP, efek CRT, dukungan penuh TalkBack/WCAG, dan 20 bahasa."""
    },
    {
        "language_code": "hi-IN",
        "language_name": "Hindi (India)",
        "app_name": "Horizon TV: मौसम और रडार",
        "short_description": "24/7 लाइव मौसम टीवी, 7-दिन का पूर्वानुमान, डॉपलर रडार, AI ब्रीफिंग और अलर्ट।",
        "full_description": """Horizon TV आपके Android फ़ोन, टैबलेट और Android TV पर 24/7 मौसम प्रसारण लाता है। स्टूडियो-ग्रेड इंटरफ़ेस में लाइव मौसम डेटा, 24/7 L-Bar न्यूज़ टिकर, 7-दिवसीय मौसम पूर्वानुमान, इंटरएक्टिव मल्टी-लेयर डॉपलर रडार, AI मुख्य मौसम विज्ञानी डेस्क और बैकग्राउंड गंभीर तूफान पुश अलर्ट का अनुभव करें।

मुख्य विशेषताएं:
- 24/7 लाइव प्रसारण और L-Bar टिकर: लाइव स्थानीय घड़ी, ब्रेकिंग अलर्ट बैज, स्क्रॉलिंग मौसम समाचार टिकर और 12-घंटे की पूर्वानुमान पट्टी के साथ आइकॉनिक टीवी L-Bar लेआउट।
- 7-दिवसीय पूर्वानुमान और Recharts रुझान: थर्मल रेंज बार के साथ Open-Meteo 7-दिन के उच्च/निम्न तापमान, विस्तृत दैनिक विवरण, और 5-दिन व 24-घंटे के तापमान और वर्षा (POP %) चार्ट।
- AI मौसम विज्ञानी डेस्क: Gemini द्वारा संचालित 1-टैप लाइव मौसम ब्रीफिंग, पहनावा और यात्रा गाइड, स्ट्रीमिंग प्रश्नोत्तर और TTS वॉयस एंकर—Eco Lite मोड, +3 दैनिक ग्रेस क्रेडिट, 0-क्रेडिट ऑन-डिवाइस लोकल इंजन और वैकल्पिक BYOK कुंजी के साथ।
- इंटरएक्टिव 4-लेयर डॉपलर रडार: वर्षा परावर्तन, इन्फ्रारेड उपग्रह, पवन प्रवाह और सड़क मानचित्र के साथ उच्च-रिज़ॉल्यूशन RainViewer रडार।
- बैकग्राउंड गंभीर मौसम पुश अलर्ट: WorkManager आवधिक निगरानी गरज-चमक, तेज हवाओं, भीषण गर्मी, पाला और वायु गुणवत्ता (AQI) के लिए उच्च-प्राथमिकता वाले पुश नोटिफिकेशन भेजती है।
- रियल-टाइम टेलीमेट्री और VOD: तापमान, महसूस होने वाला तापमान, आर्द्रता, ओस बिंदु, दबाव, यूवी सूचकांक, चंद्र कला और लाइव टेलीमेट्री VOD रिपोर्ट।
- स्टूडियो ऑनबोर्डिंग, एक्सेसिबिलिटी और 20 भाषाएं: 4-चरणीय इंटरएक्टिव ऑनबोर्डिंग टूर, Android TV रिमोट सपोर्ट, PiP मोड, CRT प्रभाव, पूर्ण TalkBack/WCAG एक्सेसिबिलिटी और 20 भाषाओं में उपलब्ध।"""
    },
    {
        "language_code": "ar",
        "language_name": "Arabic",
        "app_name": "Horizon TV: الطقس والرادار",
        "short_description": "بث مباشر للطقس 24/7، توقعات 7 أيام، رادار دوبلر، موجز الذكاء الاصطناعي وتنبيهات.",
        "full_description": """يقدم تطبيق Horizon TV بثاً تلفزيونياً مباشراً للطقس على مدار 24/7 إلى هاتف Android والجهاز اللوحي و Android TV. استمتع ببيانات الأرصاد الجوية الحية، وشريط الأخبار التلفزيوني L-Bar، وتوقعات الطقس لمدة 7 أيام، ورادار دوبلر التفاعلي متعدد الطبقات، ومكتب خبير الأرصاد بالذكاء الاصطناعي، وإشعارات الطوارئ في الخلفية.

الميزات الرئيسية:
- بث مباشر 24/7 وشريط L-Bar الإخباري: تصميم شريط L الشهير مع ساعة محلية حية، وشارات تنبيه عاجلة، وشريط أخبار طقس متحرك، وتوقعات 12 ساعة.
- توقعات 7 أيام ورسوم Recharts البيانية: درجات الحرارة العظمى والصغرى لمدة 7 أيام عبر Open-Meteo مع أشرطة النطاق الحراري، وتفاصيل يومية قابلة للتوسيع، ومنحنيات 5 أيام و24 ساعة لاحتمالية الأمطار (POP %).
- مكتب خبير الأرصاد بالذكاء الاصطناعي: موجز طقس مباشر بضغطة واحدة مدعوم بـ Gemini، ودليل الملابس والتنقل، وأسئلة وأجوبة فورية، ومذيع صوتي TTS—مع وضع Eco Lite، و+3 أرصدة مجانية يومية، ومحرك أرصاد محلي مجاني، ودعم مفتاح BYOK الاختياري.
- رادار دوبلر تفاعلي بـ 4 طبقات: مسح RainViewer عالي الدقة مع انعكاسية هطول الأمطار، والأقمار الصناعية بالأشعة تحت الحمراء، وتيارات الرياح، وخريطة الشوارع.
- تنبيهات الطقس القاسي في الخلفية (WorkManager): فحص دوري في الخلفية يرسل إشعارات فورية عالية الأولوية للعواصف الرعدية والرياح العاتية وموجات الحر والصقيع وجودة الهواء (AQI).
- قياسات حية وأرشيف فيديو VOD: الحرارة، الحرارة المحسوسة، الرطوبة، نقطة الندى، الضغط الجوي، مؤشر UV، أطوار القمر، وتقارير VOD ديناميكية.
- جولة تعريفية، إمكانية الوصول و20 لغة: جولة ترحيبية تفاعلية من 4 خطوات، دعم ريموت Android TV، وضع صورة داخل صورة (PiP)، تأثيرات CRT، توافق كامل مع TalkBack/WCAG ودعم 20 لغة."""
    },
    {
        "language_code": "ru-RU",
        "language_name": "Russian (Russia)",
        "app_name": "Horizon TV: Погода и Радар",
        "short_description": "24/7 ТВ погоды, прогноз на 7 дней, доплеровский радар, ИИ-синоптик и штормы.",
        "full_description": """Horizon TV переносит круглосуточное телевизионное вещание погоды на ваш смартфон Android, планшет и Android TV. Оцените живую метеотелеметрию, фирменную ТВ-панель L-Bar с бегущей строкой, прогноз на 7 дней, многослойный доплеровский радар, студию ИИ-синоптика и фоновые push-уведомления о штормах.

Основные возможности:
- Прямой эфир 24/7 и бегущая строка L-Bar: Культовый телевизионный L-макет с местными часами, индикаторами тревоги, новостной бегущей строкой и лентой прогноза на 12 часов.
- Прогноз на 7 дней и графики Recharts: Дневные максимумы и минимумы Open-Meteo на 7 дней с температурными шкалами, подробной разбивкой по дням и графиками на 5 дней и 24 часа (POP %).
- Студия ИИ-метеоролога: Сводки погоды в 1 касание на базе Gemini, советы по одежде и поездкам, чат с синоптиком и голосовой диктор TTS — с режимом Eco Lite, +3 бонусными кредитами в день, локальным движком за 0 кредитов и поддержкой ключа BYOK.
- Интерактивный 4-слойный радар Доплера: Высокочеткое сканирование RainViewer (осадки, ИК-спутник, потоки ветра и карта улиц).
- Фоновый мониторинг штормов (WorkManager): Регулярная проверка погоды в фоне с отправкой важных push-уведомлений о грозах, штормовом ветре, жаре, заморозках и качестве воздуха (AQI).
- Телеметрия в реальном времени и VOD: Температура, ощущаемая температура, влажность, точка росы, давление, УФ-индекс, фазы Луны и динамические видеосюжеты VOD.
- Обзорный тур, доступность и 20 языков: Интерактивный тур из 4 шагов, управление пультом Android TV, режим PiP, ЭЛТ-эффекты, полная поддержка TalkBack/WCAG и 20 языков."""
    },
    {
        "language_code": "tr-TR",
        "language_name": "Turkish (Turkey)",
        "app_name": "Horizon TV: Hava ve Radar",
        "short_description": "24/7 canlı hava TV, 7 günlük tahmin, Doppler radarı, YZ spiker ve fırtına.",
        "full_description": """Horizon TV, 24 saat kesintisiz yayın yapan hava durumu televizyonunu Android telefonunuza, tabletinize ve Android TV'nize getiriyor. Canlı meteorolojik verileri, 24/7 L-Bar kayan haber bandını, 7 günlük hava tahminini, çok katmanlı Doppler radarını, YZ Baş Meteorolog Masasını ve arka plan fırtına bildirimlerini stüdyo kalitesinde deneyimleyin.

Öne Çıkan Özellikler:
- 24/7 Canlı Yayın ve L-Bar Haber Bandı: Yerel canlı saat, acil uyarı rozetleri, kayan hava durumu bülteni ve 12 saatlik özet şeridiyle ikonik televizyon L-Bar düzeni.
- 7 Günlük Tahmin ve Recharts Grafikleri: Termal aralık çubuklarıyla Open-Meteo 7 günlük en yüksek/düşük sıcaklıklar, genişletilebilir günlük analizler ve 5 günlük / 24 saatlik yağış olasılığı (POP %) eğrileri.
- YZ Meteorolog Masası: Gemini destekli tek dokunuşla canlı hava brifingi, giyim ve ulaşım rehberi, canlı soru-cevap ve TTS sesli spiker—Eco Lite Modu, günlük +3 ek kredi, 0 kredilik cihaz içi yerel motor ve isteğe bağlı BYOK anahtarı desteğiyle.
- Etkileşimli 4 Katmanlı Doppler Radarı: Yağış yansıması, kızılötesi uydu, rüzgar akışları ve sokak haritası içeren yüksek çözünürlüklü RainViewer radarı.
- Arka Plan Şiddetli Hava Bildirimleri (WorkManager): Fırtına, kuvvetli rüzgar, aşırı sıcak, don ve hava kalitesi (AQI) için yüksek öncelikli anlık bildirimler.
- Anlık Telemetri ve VOD Arşivi: Sıcaklık, hissedilen, nem, çiy noktası, basınç, UV indeksi, ay evresi ve dinamik VOD raporları.
- Stüdyo Tanıtım Turu, Erişilebilirlik ve 20 Dil: 4 adımlı etkileşimli başlangıç turu, Android TV kumanda desteği, PiP modu, CRT efektleri, tam TalkBack/WCAG uyumu ve 20 dil."""
    },
    {
        "language_code": "pl-PL",
        "language_name": "Polish (Poland)",
        "app_name": "Horizon TV: Pogoda i Radar",
        "short_description": "Pogoda TV 24/7, prognoza 7-dniowa, radar, briefing AI i alerty burzowe.",
        "full_description": """Horizon TV przenosi całodobową telewizję pogodową 24/7 bezpośrednio na Twój telefon Android, tablet oraz Android TV. Śledź dane meteorologiczne na żywo, pasek informacyjny L-Bar, 7-dniową prognozę, wielowarstwowy radar Dopplerowski, Studio Meteorologa AI oraz powiadomienia push o burzach w tle.

Główne funkcje:
- Transmisja 24/7 i Pasek L-Bar: Kultowy układ telewizyjny L-Bar z lokalnym zegarem, plakietkami ostrzeżeń, przewijanym paskiem wiadomości i prognozą 12-godzinną.
- 7-Dniowa Prognoza i Wykresy Recharts: Maksymalne i minimalne temperatury Open-Meteo na 7 dni z paskami termicznymi, rozwijanymi szczegółami dnia oraz wykresami 5-dniowymi i 24-godzinnymi (POP %).
- Studio Meteorologa AI: Briefing pogodowy 1 kliknięciem z Gemini, poradnik ubioru i dojazdów, czat na żywo oraz lektor głosowy TTS—z trybem Eco Lite, +3 darmowymi kredytami dziennie, lokalnym silnikiem 0 kredytów i opcjonalnym kluczem BYOK.
- Interaktywny 4-Warstwowy Radar Dopplerowski: Skanowanie RainViewer HD z odbiciowościami opadów, satelitą IR, prądami wiatru i mapą ulic.
- Alerty Pogodowe w Tle (WorkManager): Okresowe monitorowanie w tle wysyła priorytetowe powiadomienia o burzach, wichurach, upałach, przymrozkach i jakości powietrza (AQI).
- Telemetria na Żywo i Archiwum VOD: Temperatura, odczuwalna, wilgotność, punkt rosy, ciśnienie, indeks UV, fazy Księżyca i dynamiczne raporty VOD.
- Przewodnik Wprowadzający, Dostępność i 20 Języków: Interaktywny 4-etapowy samouczek, obsługa pilota Android TV, tryb PiP, efekty CRT, pełna dostępność TalkBack/WCAG i 20 języków."""
    },
    {
        "language_code": "nl-NL",
        "language_name": "Dutch (Netherlands)",
        "app_name": "Horizon TV: Weer & Radar",
        "short_description": "24/7 weer-TV, 7-daagse verwachting, Dopplerradar, AI-briefing & noodweer.",
        "full_description": """Horizon TV brengt 24/7 uitgezonden weertelevisie rechtstreeks naar uw Android-telefoon, tablet en Android TV. Ervaar live meteorologische telemetrie, de iconische L-Bar nieuwsticker, 7-daagse weersverwachtingen, interactieve meerlaagse Dopplerradar, de AI-Meteoroloog Desk en achtergrondmeldingen voor noodweer.

Belangrijkste kenmerken:
- 24/7 Live Uitzending & L-Bar Ticker: Iconische tv-indeling met live lokale klok, waarschuwingsbadges, scrollende weerticker en 12-uurs overzicht.
- 7-Daagse Verwachting & Recharts-Trends: Open-Meteo 7-daagse maxima/minima met thermische balken, uitvouwbare dagdetails en 5-daagse / 24-uurs temperatuur- en neerslagcurves (POP %).
- AI-Meteoroloog Studio Desk: Gemini-aangedreven 1-tik live weerbriefings, kleding- & forensenadvies, streaming Q&A en TTS-stemnieuwslezer—met Eco Lite-modus, +3 dagelijkse bonuscredits, 0-credit lokale weermotor en optionele BYOK-sleutel.
- Interactieve 4-Laags Dopplerradar: RainViewer-scan met hoge resolutie voor neerslagreflectiviteit, IR-satelliet, windstromingen en wegenkaart.
- Noodweer Pushmeldingen op de Achtergrond (WorkManager): Periodieke controle stuurt meldingen met hoge prioriteit bij onweer, storm, extreme hitte, vorst en luchtkwaliteit (AQI).
- Live Telemetrie & VOD-Archief: Temperatuur, gevoelstemperatuur, luchtvochtigheid, dauwpunt, luchtdruk, UV-index, maanfase en dynamische VOD-rapportages.
- Studio Rondleiding, Toegankelijkheid & 20 Talen: Interactieve 4-staps introductietour, Android TV-afstandsbediening, PiP-modus, CRT-effecten, volledige TalkBack/WCAG-ondersteuning en 20 talen."""
    },
    {
        "language_code": "th-TH",
        "language_name": "Thai (Thailand)",
        "app_name": "Horizon TV: พยากรณ์อากาศ",
        "short_description": "ทีวีสภาพอากาศสด 24/7 พยากรณ์ 7 วัน เรดาร์ดอปเพลอร์ สรุปด้วย AI และเตือนพายุ",
        "full_description": """Horizon TV นำการถ่ายทอดสดรายการพยากรณ์อากาศตลอด 24 ชั่วโมงมาสู่สมาร์ทโฟน Android แท็บเล็ต และ Android TV ของคุณ สัมผัสข้อมูลอุตุนิยมวิทยาแบบเรียลไทม์ แถบข่าว L-Bar สไตล์สถานีโทรทัศน์ พยากรณ์อากาศล่วงหน้า 7 วัน เรดาร์ดอปเพลอร์หลายชั้น โต๊ะผู้ประกาศข่าวพยากรณ์อากาศ AI และการแจ้งเตือนพายุรุนแรงเบื้องหลัง

คุณสมบัติหลัก:
- ถ่ายทอดสด 24/7 และแถบข่าว L-Bar: เลย์เอาต์ L-Bar สไตล์ทีวีพร้อมนาฬิกาท้องถิ่น ป้ายเตือนภัยด่วน แถบข่าวพยากรณ์อากาศเลื่อนต่อเนื่อง และสรุปรายชั่วโมง 12 ชม.
- พยากรณ์อากาศ 7 วัน & กราฟแนวโน้ม Recharts: อุณหภูมิสูงสุด/ต่ำสุด 7 วันจาก Open-Meteo พร้อมแถบช่วงอุณหภูมิ รายละเอียดรายวันแบบขยายได้ และกราฟโอกาสเกิดฝน (POP %) 5 วัน & 24 ชั่วโมง
- โต๊ะผู้ประกาศพยากรณ์อากาศ AI: สรุปสภาพอากาศสดในแตะเดียวด้วย Gemini คำแนะนำการแต่งกายและการเดินทาง ถาม-ตอบแบบสตรีมมิง และผู้ประกาศเสียง TTS พร้อมโหมด Eco Lite เครดิตฟรีเพิ่ม +3 ต่อวัน ระบบวิเคราะห์ในเครื่อง 0 เครดิต และรองรับคีย์ BYOK
- เรดาร์ดอปเพลอร์โต้ตอบ 4 ชั้น: เรดาร์ความละเอียดสูง RainViewer แสดงกลุ่มฝน ดาวเทียมอินฟราเรด ทิศทางลม และแผนที่ถนน
- แจ้งเตือนสภาพอากาศรุนแรงเบื้องหลัง (WorkManager): ตรวจสอบอัตโนมัติและส่งการแจ้งเตือนความสำคัญสูงเมื่อเกิดพายุฝนฟ้าคะนอง ลมกระโชกแรง อากาศร้อนจัด น้ำค้างแข็ง และคุณภาพอากาศ (AQI)
- ข้อมูลอุตุนิยมวิทยาเรียลไทม์ & คลัง VOD: อุณหภูมิ อุณหภูมิที่รู้สึก ความชื้น จุดน้ำค้าง ความกดอากาศ ดัชนี UV ข้างขึ้นข้างแรม และรายงานวิดีโอ VOD ตามข้อมูลจริง
- ทัวร์แนะนำสตูดิโอ การช่วยการเข้าถึง & 20 ภาษา: ทัวร์เริ่มต้น 4 ขั้นตอน รองรับรีโมท Android TV โหมดภาพซ้อนภาพ (PiP) เอฟเฟกต์จอ CRT รองรับ TalkBack/WCAG เต็มรูปแบบ และแปลครบ 20 ภาษา"""
    },
    {
        "language_code": "uk-UA",
        "language_name": "Ukrainian (Ukraine)",
        "app_name": "Horizon TV: Погода і Радар",
        "short_description": "24/7 ТБ погоди, прогноз на 7 днів, доплерівський радар, ШІ-синоптик і шторми.",
        "full_description": """Horizon TV переносить цілодобове телевізійне мовлення погоди на ваш смартфон Android, планшет та Android TV. Відчуйте метеорологічні дані в реальному часі, фірмову ТБ-панель L-Bar із рухомим рядком, прогноз на 7 днів, багатошаровий доплерівський радар, студію ШІ-метеоролога та фонові push-сповіщення про шторми.

Основні можливості:
- Прямий ефір 24/7 та рухомий рядок L-Bar: Телевізійний L-макет із місцевим годинником, індикаторами тривоги, новинним рядком і прогнозом на 12 годин.
- Прогноз на 7 днів та графіки Recharts: Денні максимуми й мінімуми Open-Meteo на 7 днів із температурними шкалами, детальною розбивкою по днях та графіками на 5 днів і 24 години (POP %).
- Студія ШІ-метеоролога: Зведення погоди в 1 дотик на базі Gemini, поради щодо одягу й поїздок, чат із синоптиком та голосовий диктор TTS — із режимом Eco Lite, +3 бонусними кредитами щодня, локальним рушієм за 0 кредитів та підтримкою ключа BYOK.
- Інтерактивний 4-шаровий радар Доплера: Високочітке сканування RainViewer (опади, ІЧ-супутник, потоки вітру та карта вулиць).
- Фоновий моніторинг штормів (WorkManager): Періодична перевірка погоди у фоні з надсиланням пріоритетних push-сповіщень про грози, штормовий вітер, спеку, заморозки та якість повітря (AQI).
- Телеметрія в реальному часі та VOD: Температура, відчувається як, вологість, точка роси, тиск, УФ-індекс, фази Місяця та динамічні відеосюжети VOD.
- Оглядовий тур, доступність і 20 мов: Інтерактивний тур із 4 кроків, керування пультом Android TV, режим PiP, ЕПТ-ефекти, повна підтримка TalkBack/WCAG та 20 мов."""
    }
]

# Localized Onboarding Strings for all 19 values-* directories
onboarding_translations = {
    "values-es": {
        "onboarding_badge": "TOUR DEL ESTUDIO",
        "onboarding_skip": "Omitir tour",
        "onboarding_back": "Atrás",
        "onboarding_next": "Siguiente",
        "onboarding_finish": "Entrar al Estudio",
        "onboarding_step_indicator": "Paso %1$d de %2$d",
        "onboarding_slide1_title": "TV del Clima en Vivo 24/7 y Barra L",
        "onboarding_slide1_desc": "Viva una red meteorológica 24/7 de nivel de estudio con nuestra icónica barra lateral L-Bar, teletipo continuo de alertas, reloj local en vivo y reproductor Picture-in-Picture (PiP).",
        "onboarding_slide1_highlight1": "Escenario TV en Vivo 24/7 y Modo PiP",
        "onboarding_slide1_highlight2": "Teletipo L-Bar y Franja de 12 Horas",
        "onboarding_slide2_title": "Pronóstico de 7 Días y Radar Doppler",
        "onboarding_slide2_desc": "Rastree tormentas con el radar Doppler multicapa RainViewer, satélite infrarrojo y vectores de viento junto con una perspectiva Open-Meteo de 7 días con barras de rango térmico.",
        "onboarding_slide2_highlight1": "Mapa de Radar Doppler de 4 Capas",
        "onboarding_slide2_highlight2": "Barras Térmicas de 7 Días y Tendencias 24h",
        "onboarding_slide3_title": "Mesa de Meteorólogo IA y Cuota Inteligente",
        "onboarding_slide3_desc": "Genere boletines en vivo con 1 toque, consejos de vestimenta y transporte, y lectura por voz con Gemini IA—con Modo Eco Lite, +3 créditos diarios de gracia, motor local de 0 créditos y llave BYOK.",
        "onboarding_slide3_highlight1": "Boletín IA en Vivo, Locutor de Voz y Chat",
        "onboarding_slide3_highlight2": "Eco Lite, Créditos Extra, Motor Local y BYOK",
        "onboarding_slide4_title": "Alertas Push de Tormenta y Configuración",
        "onboarding_slide4_desc": "Manténgase protegido con el monitoreo en segundo plano de WorkManager y notificaciones push de alta prioridad. Elija sus unidades de temperatura y estilo visual a continuación:",
        "onboarding_slide4_highlight1": "Alertas Push de Clima Severo (WorkManager)",
        "onboarding_slide4_highlight2": "Accesibilidad TalkBack y 20 Idiomas",
        "settings_onboarding_title": "Tour Interactivo de Bienvenida al Estudio",
        "settings_onboarding_subtitle": "Vuelva a ver la guía de 4 pasos sobre TV en Vivo L-Bar, Radar de 7 Días, Mesa IA y Alertas",
        "settings_onboarding_action": "INICIAR TOUR"
    },
    "values-fr": {
        "onboarding_badge": "VISITE DU STUDIO",
        "onboarding_skip": "Passer la visite",
        "onboarding_back": "Retour",
        "onboarding_next": "Suivant",
        "onboarding_finish": "Entrer dans le Studio",
        "onboarding_step_indicator": "Étape %1$d sur %2$d",
        "onboarding_slide1_title": "Météo TV en Direct 24/7 et Bandeau L-Bar",
        "onboarding_slide1_desc": "Découvrez une chaîne météo 24/7 de qualité studio avec notre barre latérale L-Bar emblématique, un téléscripteur d'alertes défilant, une horloge locale et le mode Picture-in-Picture (PiP).",
        "onboarding_slide1_highlight1": "Direct TV 24/7 et Mode PiP",
        "onboarding_slide1_highlight2": "Téléscripteur L-Bar et Bandeau 12h",
        "onboarding_slide2_title": "Prévisions à 7 Jours et Radar Doppler",
        "onboarding_slide2_desc": "Suivez les tempêtes avec le radar Doppler multicouche RainViewer, le satellite IR et les flux de vent ainsi qu'un aperçu Open-Meteo sur 7 jours avec barres thermiques.",
        "onboarding_slide2_highlight1": "Carte Radar Doppler Interactive à 4 Couches",
        "onboarding_slide2_highlight2": "Barres Thermiques 7 Jours et Courbes 24h",
        "onboarding_slide3_title": "Bureau Météo IA et Garde-Fous Gratuits",
        "onboarding_slide3_desc": "Générez des briefings météo en 1 geste, conseils tenue et trajet, et lecture vocale avec Gemini IA—avec mode Eco Lite, +3 crédits quotidiens, moteur local à 0 crédit et clé BYOK.",
        "onboarding_slide3_highlight1": "Briefing IA Direct, Voix Anchor et Q&R",
        "onboarding_slide3_highlight2": "Eco Lite, Crédits Bonus, Moteur Local et BYOK",
        "onboarding_slide4_title": "Alertes Push Tempête et Configuration Studio",
        "onboarding_slide4_desc": "Restez protégé grâce à la surveillance en arrière-plan WorkManager et aux notifications prioritaires. Choisissez vos unités de température et le style visuel ci-dessous :",
        "onboarding_slide4_highlight1": "Alertes Météo Graves en Arrière-Plan",
        "onboarding_slide4_highlight2": "Accessibilité TalkBack et 20 Langues",
        "settings_onboarding_title": "Visite Guidée Interactive du Studio",
        "settings_onboarding_subtitle": "Revoir la présentation en 4 étapes du Direct L-Bar, du Radar 7 Jours, du Bureau IA et des Alertes",
        "settings_onboarding_action": "LANCER LA VISITE"
    },
    "values-de": {
        "onboarding_badge": "STUDIO-EINFÜHRUNGSTOUR",
        "onboarding_skip": "Tour überspringen",
        "onboarding_back": "Zurück",
        "onboarding_next": "Weiter",
        "onboarding_finish": "Studio betreten",
        "onboarding_step_indicator": "Schritt %1$d von %2$d",
        "onboarding_slide1_title": "24/7 Live-Wetter-TV & L-Bar-Ticker",
        "onboarding_slide1_desc": "Erleben Sie ein 24/7-Wetternetzwerk in Studioqualität mit unserer ikonischen L-Bar-Seitenleiste, laufendem Eilmeldungs-Ticker, lokaler Uhr und Bild-in-Bild-Player (PiP).",
        "onboarding_slide1_highlight1": "24/7 Live-TV-Bühne & PiP-Modus",
        "onboarding_slide1_highlight2": "L-Bar-Nachrichtenticker & 12h-Leiste",
        "onboarding_slide2_title": "7-Tage-Vorhersage & Live-Doppler-Radar",
        "onboarding_slide2_desc": "Verfolgen Sie Stürme mit mehrschichtigem RainViewer-Doppler-Radar, IR-Satellit und Windvektoren sowie einer 7-Tage-Open-Meteo-Vorhersage mit Temperaturbalken.",
        "onboarding_slide2_highlight1": "Interaktive 4-Schichten-Doppler-Radarkarte",
        "onboarding_slide2_highlight2": "7-Tage-Wärmebalken & 24-Stunden-Trends",
        "onboarding_slide3_title": "KI-Meteorologen-Desk & Gratis-Schutz",
        "onboarding_slide3_desc": "Erstellen Sie 1-Tap-Wetterbriefings, Kleidungs- & Pendlertipps sowie Sprachansagen mit Gemini KI—mit Eco Lite, +3 täglichen Zusatz-Credits, 0-Credit-Lokal-Engine und BYOK.",
        "onboarding_slide3_highlight1": "Live-KI-Briefing, Sprachmoderator & Q&A",
        "onboarding_slide3_highlight2": "Eco Lite, Zusatz-Credits, Lokal-Engine & BYOK",
        "onboarding_slide4_title": "Unwetter-Push-Warnungen & Studio-Setup",
        "onboarding_slide4_desc": "Bleiben Sie geschützt durch WorkManager-Hintergrundüberwachung und Prioritäts-Push-Warnungen. Wählen Sie unten Ihre Temperatureinheit und den TV-Stil:",
        "onboarding_slide4_highlight1": "WorkManager-Hintergrund-Unwetterwarnungen",
        "onboarding_slide4_highlight2": "Volle TalkBack-Barrierefreiheit & 20 Sprachen",
        "settings_onboarding_title": "Interaktive Studio-Einführungstour",
        "settings_onboarding_subtitle": "4-Schritte-Tour zu Live-TV L-Bar, 7-Tage-Radar, KI-Meteorologen-Desk und Unwetterwarnungen starten",
        "settings_onboarding_action": "TOUR STARTEN"
    },
    "values-it": {
        "onboarding_badge": "TOUR DELLO STUDIO",
        "onboarding_skip": "Salta tour",
        "onboarding_back": "Indietro",
        "onboarding_next": "Avanti",
        "onboarding_finish": "Entra nello Studio",
        "onboarding_step_indicator": "Passaggio %1$d di %2$d",
        "onboarding_slide1_title": "Meteo TV in Diretta 24/7 e Ticker L-Bar",
        "onboarding_slide1_desc": "Vivi una rete meteo 24/7 di livello televisivo con l'iconica barra laterale L-Bar, ticker scorrevole di allerte, orologio locale e lettore Picture-in-Picture (PiP).",
        "onboarding_slide1_highlight1": "Palco Live TV 24/7 e Modalità PiP",
        "onboarding_slide1_highlight2": "Ticker Notizie L-Bar e Fascia 12h",
        "onboarding_slide2_title": "Previsioni a 7 Giorni e Radar Doppler",
        "onboarding_slide2_desc": "Monitora le tempeste con il radar Doppler multistrato RainViewer, satellite IR e vettori del vento insieme alle previsioni Open-Meteo a 7 giorni con barre termiche.",
        "onboarding_slide2_highlight1": "Mappa Radar Doppler a 4 Livelli",
        "onboarding_slide2_highlight2": "Barre Termiche 7 Giorni e Trend 24 Ore",
        "onboarding_slide3_title": "Desk Meteorologo IA e Tutela Quota Gratuita",
        "onboarding_slide3_desc": "Genera briefing meteo in 1 tocco, consigli abbigliamento e pendolari e lettura vocale con Gemini IA—con modalità Eco Lite, +3 crediti extra, motore locale a 0 crediti e BYOK.",
        "onboarding_slide3_highlight1": "Briefing IA Live, Conduttore Vocale e Q&A",
        "onboarding_slide3_highlight2": "Eco Lite, Crediti Extra, Motore Locale e BYOK",
        "onboarding_slide4_title": "Notifiche Push Maltempo e Setup Studio",
        "onboarding_slide4_desc": "Resta protetto con il monitoraggio in background WorkManager e notifiche push ad alta priorità. Scegli le unità di temperatura e lo stile visivo qui sotto:",
        "onboarding_slide4_highlight1": "Allerte Maltempo in Background (WorkManager)",
        "onboarding_slide4_highlight2": "Piena Accessibilità TalkBack e 20 Lingue",
        "settings_onboarding_title": "Tour Guidato Interattivo dello Studio",
        "settings_onboarding_subtitle": "Rivedi il tour in 4 passaggi su Live TV L-Bar, Radar 7 Giorni, Desk IA e Allerte Maltempo",
        "settings_onboarding_action": "AVVIA TOUR"
    },
    "values-pt": {
        "onboarding_badge": "TOUR DO ESTÚDIO",
        "onboarding_skip": "Pular tour",
        "onboarding_back": "Voltar",
        "onboarding_next": "Avançar",
        "onboarding_finish": "Entrar no Estúdio",
        "onboarding_step_indicator": "Etapa %1$d de %2$d",
        "onboarding_slide1_title": "TV de Clima ao Vivo 24/7 e Ticker L-Bar",
        "onboarding_slide1_desc": "Experimente uma rede meteorológica 24/7 com nossa icônica barra lateral L-Bar, letreiro deslizante de alertas, relógio local ao vivo e reprodutor Picture-in-Picture (PiP).",
        "onboarding_slide1_highlight1": "Palco TV ao Vivo 24/7 e Modo PiP",
        "onboarding_slide1_highlight2": "Ticker de Notícias L-Bar e Faixa de 12h",
        "onboarding_slide2_title": "Previsão de 7 Dias e Radar Doppler",
        "onboarding_slide2_desc": "Acompanhe tempestades com radar Doppler multicamadas RainViewer, satélite IR e vetores de vento junto com a previsão Open-Meteo de 7 dias com barras térmicas.",
        "onboarding_slide2_highlight1": "Mapa de Radar Doppler de 4 Camadas",
        "onboarding_slide2_highlight2": "Barras Térmicas de 7 Dias e Tendências 24h",
        "onboarding_slide3_title": "Bancada do Meteorologista IA e Cota Inteligente",
        "onboarding_slide3_desc": "Gere boletins ao vivo em 1 toque, dicas de roupas e deslocamento e locução com Gemini IA—com Modo Eco Lite, +3 créditos diários, motor local de 0 créditos e chave BYOK.",
        "onboarding_slide3_highlight1": "Boletim IA ao Vivo, Âncora de Voz e Q&A",
        "onboarding_slide3_highlight2": "Eco Lite, Créditos Extras, Motor Local e BYOK",
        "onboarding_slide4_title": "Alertas Push de Tempestade e Configuração",
        "onboarding_slide4_desc": "Fique protegido com o monitoramento em segundo plano do WorkManager e notificações push prioritárias. Escolha suas unidades de temperatura e estilo visual abaixo:",
        "onboarding_slide4_highlight1": "Alertas de Tempestade em Segundo Plano",
        "onboarding_slide4_highlight2": "Acessibilidade TalkBack e 20 Idiomas",
        "settings_onboarding_title": "Tour Interativo de Boas-Vindas ao Estúdio",
        "settings_onboarding_subtitle": "Reveja o guia de 4 etapas sobre TV ao Vivo L-Bar, Radar 7 Dias, Bancada IA e Alertas",
        "settings_onboarding_action": "INICIAR TOUR"
    },
    "values-ja": {
        "onboarding_badge": "スタジオ案内ツアー",
        "onboarding_skip": "スキップ",
        "onboarding_back": "戻る",
        "onboarding_next": "次へ",
        "onboarding_finish": "放送スタジオへ入る",
        "onboarding_step_indicator": "ステップ %1$d / %2$d",
        "onboarding_slide1_title": "24/7 ライブ気象放送＆Lバー・ティッカー",
        "onboarding_slide1_desc": "象徴的なLバー・テレメトリサイドバー、スクロール緊急気象ニュースティッカー、現地ライブ時計、ピクチャーインピクチャー（PiP）を備えた24時間気象ネットワークを体験しましょう。",
        "onboarding_slide1_highlight1": "24/7 ライブTVステージ＆PiPモード",
        "onboarding_slide1_highlight2": "放送Lバーニュースティッカー＆12時間予報",
        "onboarding_slide2_title": "7日間週間予報＆ライブドップラーレーダー",
        "onboarding_slide2_desc": "RainViewer多層ドップラーレーダー、赤外線衛星、風向ベクトルで雨雲を追跡し、気温レンジバー付きの7日間Open-Meteo週間予報を確認できます。",
        "onboarding_slide2_highlight1": "4層インタラクティブ・ドップラーレーダー地図",
        "onboarding_slide2_highlight2": "7日間気温レンジバー＆24時間トレンド",
        "onboarding_slide3_title": "AI気象キャスターデスク＆無料枠ガードレール",
        "onboarding_slide3_desc": "Gemini AIによる1タップ生放送ブリーフィング、服装・通勤アドバイス、音声アナウンサー読み上げを提供。Eco Liteモード、毎日+3無料追加クレジット、0クレジット端末内エンジン、BYOKに対応。",
        "onboarding_slide3_highlight1": "ライブAIブリーフィング・音声アナウンサー・Q&A",
        "onboarding_slide3_highlight2": "Eco Lite・追加クレジット・ローカルエンジン・BYOK",
        "onboarding_slide4_title": "気象警報プッシュ通知＆スタジオ設定",
        "onboarding_slide4_desc": "WorkManagerによるバックグラウンド気象監視と高優先度プッシュ通知で安全を確保します。ご希望の温度単位と放送画面スタイルを以下から選択してください：",
        "onboarding_slide4_highlight1": "WorkManagerバックグラウンド気象警報通知",
        "onboarding_slide4_highlight2": "TalkBack完全対応＆20言語ローカライズ",
        "settings_onboarding_title": "インタラクティブ・スタジオ案内ツアー",
        "settings_onboarding_subtitle": "ライブTV Lバー、7日間レーダー、AI気象デスク、気象警報の4ステップガイドを再表示します",
        "settings_onboarding_action": "ツアーを開始"
    },
    "values-ko": {
        "onboarding_badge": "스튜디오 온보딩 투어",
        "onboarding_skip": "건너뛰기",
        "onboarding_back": "이전",
        "onboarding_next": "다음",
        "onboarding_finish": "방송 스튜디오 입장",
        "onboarding_step_indicator": "%2$d단계 중 %1$d단계",
        "onboarding_slide1_title": "24/7 실시간 기상 방송 및 L-Bar 티커",
        "onboarding_slide1_desc": "상징적인 L-Bar 텔레메트리 사이드바, 실시간 스크롤 뉴스 티커, 현지 시계 및 PIP(Picture-in-Picture) 플레이어를 갖춘 24시간 기상 네트워크를 경험하세요.",
        "onboarding_slide1_highlight1": "24/7 라이브 TV 스테이지 및 PIP 모드",
        "onboarding_slide1_highlight2": "방송용 L-Bar 뉴스 티커 및 12시간 스트립",
        "onboarding_slide2_title": "7일 주간 예보 및 실시간 도플러 레이더",
        "onboarding_slide2_desc": "RainViewer 다층 도플러 레이더, 적외선 위성, 바람 벡터로 폭풍을 추적하고 온도 범위 막대가 있는 7일 Open-Meteo 주간 예보를 확인하세요.",
        "onboarding_slide2_highlight1": "4계층 대화형 도플러 레이더 지도",
        "onboarding_slide2_highlight2": "7일 온도 범위 막대 및 24시간 트렌드",
        "onboarding_slide3_title": "AI 기상캐스터 데스크 및 무료 할당량 보호",
        "onboarding_slide3_desc": "Gemini AI 기반 원탭 라이브 브리핑, 옷차림·출퇴근 가이드 및 음성 앵커를 생성하세요. Eco Lite 모드, 매일 +3 보너스 크레딧, 0크레딧 로컬 엔진 및 BYOK 키를 지원합니다.",
        "onboarding_slide3_highlight1": "라이브 AI 브리핑, 음성 앵커 및 실시간 Q&A",
        "onboarding_slide3_highlight2": "Eco Lite, 보너스 크레딧, 로컬 엔진 및 BYOK",
        "onboarding_slide4_title": "기상 특보 푸시 알림 및 스튜디오 설정",
        "onboarding_slide4_desc": "WorkManager 백그라운드 기상 감시와 고우선순위 푸시 알림으로 안전을 지키세요. 아래에서 선호하는 온도 단위와 방송 화면 스타일을 선택하세요:",
        "onboarding_slide4_highlight1": "WorkManager 백그라운드 기상 특보 알림",
        "onboarding_slide4_highlight2": "TalkBack 완벽 지원 및 20개 언어 제공",
        "settings_onboarding_title": "대화형 스튜디오 온보딩 투어",
        "settings_onboarding_subtitle": "라이브 TV L-Bar, 7일 레이더, AI 기상 데스크 및 기상 특보의 4단계 가이드 다시 보기",
        "settings_onboarding_action": "투어 시작"
    },
    "values-zh-rCN": {
        "onboarding_badge": "演播室新手导览",
        "onboarding_skip": "跳过导览",
        "onboarding_back": "上一步",
        "onboarding_next": "下一步",
        "onboarding_finish": "进入广播演播室",
        "onboarding_step_indicator": "第 %1$d / %2$d 步",
        "onboarding_slide1_title": "24/7 全天候直播天气与 L-Bar 资讯条",
        "onboarding_slide1_desc": "体验专业演播室级 24/7 气象电视网络，包含标志性 L-Bar 遥测侧边栏、滚动突发天气字幕条、当地实时时钟与画中画 (PiP) 播放器。",
        "onboarding_slide1_highlight1": "24/7 直播电视舞台与画中画模式",
        "onboarding_slide1_highlight2": "广播 L-Bar 滚动快讯与 12 小时预报条",
        "onboarding_slide2_title": "7 天延伸天气预报与实时多普勒雷达",
        "onboarding_slide2_desc": "通过多层 RainViewer 多普勒雷达、红外卫星云图与风场矢量追踪风暴，并查看带有每日温差条的 7 天 Open-Meteo 预报。",
        "onboarding_slide2_highlight1": "4 图层交互式多普勒雷达地图",
        "onboarding_slide2_highlight2": "7 天温差条与 24 小时趋势曲线",
        "onboarding_slide3_title": "AI 气象主播台与智能免费额度护栏",
        "onboarding_slide3_desc": "由 Gemini AI 驱动的一键直播气象简报、穿衣与通勤建议及语音播报——支持 Eco Lite 模式、每日 +3 免费宽限额度、0 额度本地引擎与 BYOK 密钥。",
        "onboarding_slide3_highlight1": "直播 AI 简报、语音主播与实时问答",
        "onboarding_slide3_highlight2": "Eco Lite、宽限额度、本地引擎与 BYOK",
        "onboarding_slide4_title": "恶劣天气推送预警与演播室设置",
        "onboarding_slide4_desc": "通过 WorkManager 后台天气监测与高优先级推送通知随时掌握风暴动态。请在下方选择您偏好的温度单位与广播视觉风格：",
        "onboarding_slide4_highlight1": "WorkManager 后台恶劣天气警报",
        "onboarding_slide4_highlight2": "完整 TalkBack 无障碍与 20 种语言",
        "settings_onboarding_title": "交互式演播室功能导览",
        "settings_onboarding_subtitle": "重新查看涵盖直播 L-Bar、7 天雷达、AI 气象主播台与风暴预警的 4 步引导",
        "settings_onboarding_action": "启动导览"
    },
    "values-zh-rTW": {
        "onboarding_badge": "攝影棚新手導覽",
        "onboarding_skip": "略過導覽",
        "onboarding_back": "上一步",
        "onboarding_next": "下一步",
        "onboarding_finish": "進入廣播攝影棚",
        "onboarding_step_indicator": "第 %1$d / %2$d 步",
        "onboarding_slide1_title": "24/7 全天候直播天氣與 L-Bar 跑馬燈",
        "onboarding_slide1_desc": "體驗專業攝影棚級 24/7 氣象電視網路，包含經典 L-Bar 遙測側邊欄、滾動突發天氣跑馬燈、當地即時時鐘與子母畫面 (PiP) 播放器。",
        "onboarding_slide1_highlight1": "24/7 直播電視舞台與子母畫面模式",
        "onboarding_slide1_highlight2": "廣播 L-Bar 跑馬燈快訊與 12 小時預報條",
        "onboarding_slide2_title": "7 天延伸天氣預報與即時都卜勒雷達",
        "onboarding_slide2_desc": "透過多層 RainViewer 都卜勒雷達、紅外線衛星雲圖與風場向量追蹤風暴，並查看附有每日溫差條的 7 天 Open-Meteo 預報。",
        "onboarding_slide2_highlight1": "4 圖層互動式都卜勒雷達地圖",
        "onboarding_slide2_highlight2": "7 天溫差條與 24 小時趨勢曲線",
        "onboarding_slide3_title": "AI 氣象主播台與智慧免費額度護欄",
        "onboarding_slide3_desc": "由 Gemini AI 驅動的一鍵直播氣象簡報、穿搭與通勤建議及語音播報——支援 Eco Lite 模式、每日 +3 免費寬限額度、0 額度本機引擎與 BYOK 金鑰。",
        "onboarding_slide3_highlight1": "直播 AI 簡報、語音主播與即時問答",
        "onboarding_slide3_highlight2": "Eco Lite、寬限額度、本機引擎與 BYOK",
        "onboarding_slide4_title": "劇烈天氣推播預警與攝影棚設定",
        "onboarding_slide4_desc": "透過 WorkManager 背景天氣監測與高優先級推播通知隨時掌握風暴動態。請在下方選擇您偏好的溫度單位與廣播視覺風格：",
        "onboarding_slide4_highlight1": "WorkManager 背景劇烈天氣警報",
        "onboarding_slide4_highlight2": "完整 TalkBack 無障礙與 20 種語言",
        "settings_onboarding_title": "互動式攝影棚功能導覽",
        "settings_onboarding_subtitle": "重新查看涵蓋直播 L-Bar、7 天雷達、AI 氣象主播台與風暴預警的 4 步引導",
        "settings_onboarding_action": "啟動導覽"
    },
    "values-vi": {
        "onboarding_badge": "HƯỚNG DẪN STUDIO",
        "onboarding_skip": "Bỏ qua",
        "onboarding_back": "Quay lại",
        "onboarding_next": "Tiếp theo",
        "onboarding_finish": "Vào Studio Phát Sóng",
        "onboarding_step_indicator": "Bước %1$d / %2$d",
        "onboarding_slide1_title": "Truyền Hình Thời Tiết 24/7 & Thanh L-Bar",
        "onboarding_slide1_desc": "Trải nghiệm mạng truyền hình thời tiết 24/7 chuẩn studio với thanh bên L-Bar biểu tượng, dòng tin chạy liên tục, đồng hồ địa phương và trình phát Hình trong Hình (PiP).",
        "onboarding_slide1_highlight1": "Sân Khấu Live TV 24/7 & Chế Độ PiP",
        "onboarding_slide1_highlight2": "Tin Chạy L-Bar & Dải Dự Báo 12 Giờ",
        "onboarding_slide2_title": "Dự Báo 7 Ngày & Radar Doppler Trực Tiếp",
        "onboarding_slide2_desc": "Theo dõi bão với radar Doppler đa lớp RainViewer, vệ tinh hồng ngoại và luồng gió cùng dự báo Open-Meteo 7 ngày với thanh dải nhiệt trực quan.",
        "onboarding_slide2_highlight1": "Bản Đồ Radar Doppler Tương Tác 4 Lớp",
        "onboarding_slide2_highlight2": "Thanh Nhiệt Độ 7 Ngày & Xu Hướng 24 Giờ",
        "onboarding_slide3_title": "Bàn Khí Tượng AI & Bảo Vệ Hạn Mức Miễn Phí",
        "onboarding_slide3_desc": "Tạo bản tin thời tiết 1 chạm, tư vấn trang phục & di chuyển và giọng đọc TTS với Gemini AI—hỗ trợ Eco Lite, +3 lượt miễn phí mỗi ngày, bộ máy nội bộ 0 tốn lượt và khóa BYOK.",
        "onboarding_slide3_highlight1": "Bản Tin AI Trực Tiếp, Giọng Đọc & Hỏi Đáp",
        "onboarding_slide3_highlight2": "Eco Lite, Lượt Tặng, Máy Nội Bộ & BYOK",
        "onboarding_slide4_title": "Cảnh Báo Bão Chạy Nền & Thiết Lập Studio",
        "onboarding_slide4_desc": "Luôn an toàn với dịch vụ WorkManager giám sát thời tiết nguy hiểm chạy nền và thông báo đẩy ưu tiên cao. Chọn đơn vị nhiệt độ và giao diện bên dưới:",
        "onboarding_slide4_highlight1": "Cảnh Báo Bão Chạy Nền (WorkManager)",
        "onboarding_slide4_highlight2": "Trợ Năng TalkBack Đầy Đủ & 20 Ngôn Ngữ",
        "settings_onboarding_title": "Tour Hướng Dẫn Studio Tương Tác",
        "settings_onboarding_subtitle": "Xem lại hướng dẫn 4 bước về Live TV L-Bar, Radar 7 Ngày, Bàn Khí Tượng AI và Cảnh Báo Bão",
        "settings_onboarding_action": "MỞ HƯỚNG DẪN"
    },
    "values-id": {
        "onboarding_badge": "TUR ONBOARDING STUDIO",
        "onboarding_skip": "Lewati Tur",
        "onboarding_back": "Kembali",
        "onboarding_next": "Berikutnya",
        "onboarding_finish": "Masuk ke Studio Siaran",
        "onboarding_step_indicator": "Langkah %1$d dari %2$d",
        "onboarding_slide1_title": "TV Cuaca Langsung 24/7 & Ticker L-Bar",
        "onboarding_slide1_desc": "Nikmati jaringan cuaca 24/7 kelas studio dengan bilah samping telemetri L-Bar ikonik, teks berjalan berita cuaca, jam lokal langsung, dan pemutar Picture-in-Picture (PiP).",
        "onboarding_slide1_highlight1": "Panggung Live TV 24/7 & Mode PiP",
        "onboarding_slide1_highlight2": "Ticker Berita L-Bar & Strip 12 Jam",
        "onboarding_slide2_title": "Prakiraan 7 Hari & Radar Doppler Langsung",
        "onboarding_slide2_desc": "Pantau badai dengan radar Doppler multi-lapis RainViewer, satelit inframerah, dan vektor angin bersama prakiraan 7 hari Open-Meteo dengan bilah rentang suhu.",
        "onboarding_slide2_highlight1": "Peta Radar Doppler Interaktif 4 Lapis",
        "onboarding_slide2_highlight2": "Bilah Suhu 7 Hari & Tren 24 Jam",
        "onboarding_slide3_title": "Meja Meteorolog AI & Pelindung Kuota Gratis",
        "onboarding_slide3_desc": "Buat briefing cuaca 1 ketukan, saran pakaian & perjalanan, serta pembaca suara TTS dengan Gemini AI—dilengkapi Mode Eco Lite, +3 kredit ekstra harian, mesin lokal 0 kredit, dan BYOK.",
        "onboarding_slide3_highlight1": "Briefing AI Langsung, Suara Anchor & Q&A",
        "onboarding_slide3_highlight2": "Eco Lite, Kredit Ekstra, Mesin Lokal & BYOK",
        "onboarding_slide4_title": "Notifikasi Push Badai & Pengaturan Studio",
        "onboarding_slide4_desc": "Tetap terlindungi dengan pemantauan cuaca buruk latar belakang WorkManager dan notifikasi prioritas tinggi. Pilih satuan suhu dan gaya tampilan di bawah ini:",
        "onboarding_slide4_highlight1": "Peringatan Cuaca Buruk Latar Belakang",
        "onboarding_slide4_highlight2": "Aksesibilitas TalkBack Penuh & 20 Bahasa",
        "settings_onboarding_title": "Tur Onboarding Studio Interaktif",
        "settings_onboarding_subtitle": "Putar ulang panduan 4 langkah Live TV L-Bar, Radar 7 Hari, Meja AI, dan Peringatan Cuaca",
        "settings_onboarding_action": "MULAI TUR"
    },
    "values-hi": {
        "onboarding_badge": "स्टूडियो ऑनबोर्डिंग टूर",
        "onboarding_skip": "टूर छोड़ें",
        "onboarding_back": "पीछे",
        "onboarding_next": "अगला",
        "onboarding_finish": "ब्रॉडकास्ट स्टूडियो में प्रवेश करें",
        "onboarding_step_indicator": "चरण %1$d / %2$d",
        "onboarding_slide1_title": "24/7 लाइव मौसम प्रसारण और L-Bar टिकर",
        "onboarding_slide1_desc": "हमारे आइकॉनिक L-Bar टेलीमेट्री साइडबार, स्क्रॉलिंग ब्रेकिंग मौसम टिकर, लाइव स्थानीय घड़ी और पिक्चर-इन-पिक्चर (PiP) प्लेयर के साथ स्टूडियो-ग्रेड 24/7 मौसम नेटवर्क का अनुभव करें।",
        "onboarding_slide1_highlight1": "24/7 लाइव टीवी स्टेज और PiP मोड",
        "onboarding_slide1_highlight2": "ब्रॉडकास्ट L-Bar न्यूज़ टिकर और 12h पट्टी",
        "onboarding_slide2_title": "7-दिवसीय पूर्वानुमान और लाइव डॉपलर रडार",
        "onboarding_slide2_desc": "मल्टी-लेयर RainViewer डॉपलर रडार, इन्फ्रारेड उपग्रह और पवन वेक्टर के साथ तूफानों को ट्रैक करें और थर्मल रेंज बार के साथ 7-दिवसीय Open-Meteo पूर्वानुमान देखें।",
        "onboarding_slide2_highlight1": "4-लेयर इंटरएक्टिव डॉपलर रडार मानचित्र",
        "onboarding_slide2_highlight2": "7-दिवसीय थर्मल बार और 24-घंटे के रुझान",
        "onboarding_slide3_title": "AI मौसम विज्ञानी डेस्क और फ्री-टियर सुरक्षा",
        "onboarding_slide3_desc": "Gemini AI द्वारा संचालित 1-टैप लाइव मौसम ब्रीफिंग, पहनावा और यात्रा सलाह, और वॉयस एंकर सुनें—Eco Lite मोड, +3 दैनिक ग्रेस क्रेडिट, 0-क्रेडिट लोकल इंजन और BYOK सपोर्ट के साथ।",
        "onboarding_slide3_highlight1": "लाइव AI ब्रीफिंग, वॉयस एंकर और प्रश्नोत्तर",
        "onboarding_slide3_highlight2": "Eco Lite, ग्रेस क्रेडिट, लोकल इंजन और BYOK",
        "onboarding_slide4_title": "गंभीर तूफान पुश अलर्ट और स्टूडियो सेटअप",
        "onboarding_slide4_desc": "WorkManager बैकग्राउंड मौसम निगरानी और उच्च-प्राथमिकता पुश नोटिफिकेशन के साथ सुरक्षित रहें। नीचे अपनी पसंदीदा तापमान इकाई और प्रसारण शैली चुनें:",
        "onboarding_slide4_highlight1": "WorkManager बैकग्राउंड गंभीर तूफान अलर्ट",
        "onboarding_slide4_highlight2": "पूर्ण TalkBack एक्सेसिबिलिटी और 20 भाषाएं",
        "settings_onboarding_title": "इंटरएक्टिव स्टूडियो ऑनबोर्डिंग टूर",
        "settings_onboarding_subtitle": "लाइव टीवी L-Bar, 7-दिवसीय रडार, AI मौसम डेस्क और गंभीर अलर्ट का 4-चरणीय टूर फिर से देखें",
        "settings_onboarding_action": "टूर शुरू करें"
    },
    "values-ar": {
        "onboarding_badge": "جولة استوديو البث",
        "onboarding_skip": "تخطي الجولة",
        "onboarding_back": "السابق",
        "onboarding_next": "التالي",
        "onboarding_finish": "دخول استوديو البث",
        "onboarding_step_indicator": "الخطوة %1$d من %2$d",
        "onboarding_slide1_title": "بث مباشر للطقس 24/7 وشريط L-Bar الإخباري",
        "onboarding_slide1_desc": "استمتع بشبكة طقس تلفزيونية على مدار الساعة مع شريط القياس الجانبي L-Bar الشهير، وشريط الأخبار العاجلة المتحرك، والساعة المحلية الحية، ومشغل صورة داخل صورة (PiP).",
        "onboarding_slide1_highlight1": "منصة بث مباشر 24/7 ووضع PiP",
        "onboarding_slide1_highlight2": "شريط أخبار L-Bar وتوقعات 12 ساعة",
        "onboarding_slide2_title": "توقعات 7 أيام ورادار دوبلر المباشر",
        "onboarding_slide2_desc": "تتبع العواصف عبر رادار دوبلر متعدد الطبقات RainViewer والأقمار الصناعية بالأشعة تحت الحمراء ومتجهات الرياح بجانب توقعات Open-Meteo لمدة 7 أيام.",
        "onboarding_slide2_highlight1": "خريطة رادار دوبلر تفاعلية بـ 4 طبقات",
        "onboarding_slide2_highlight2": "أشرطة حرارية لـ 7 أيام واتجاهات 24 ساعة",
        "onboarding_slide3_title": "مكتب خبير الأرصاد بالذكاء الاصطناعي وحماية الحصة",
        "onboarding_slide3_desc": "احصل على موجز طقس مباشر بضغطة واحدة ونصائح الملابس والتنقل وقراءة صوتية عبر Gemini AI—مع وضع Eco Lite و+3 أرصدة يومية ومحرك محلي مجاني ودعم مفتاح BYOK.",
        "onboarding_slide3_highlight1": "موجز ذكاء اصطناعي، مذيع صوتي وأسئلة",
        "onboarding_slide3_highlight2": "Eco Lite، أرصدة إضافية، محرك محلي وBYOK",
        "onboarding_slide4_title": "تنبيهات العواصف الفورية وإعداد الاستوديو",
        "onboarding_slide4_desc": "ابقَ آمناً مع مراقبة الطقس القاسي في الخلفية عبر WorkManager والإشعارات الفورية عالية الأولوية. اختر وحدات الحرارة ونمط العرض المفضل أدناه:",
        "onboarding_slide4_highlight1": "تنبيهات الطقس القاسي في الخلفية (WorkManager)",
        "onboarding_slide4_highlight2": "دعم كامل لقارئ الشاشة TalkBack و20 لغة",
        "settings_onboarding_title": "جولة تعريفية تفاعلية في الاستوديو",
        "settings_onboarding_subtitle": "إعادة عرض الدليل المكون من 4 خطوات للبث المباشر L-Bar والرادار ومكتب الذكاء الاصطناعي والتنبيهات",
        "settings_onboarding_action": "بدء الجولة"
    },
    "values-ru": {
        "onboarding_badge": "ТУР ПО СТУДИИ",
        "onboarding_skip": "Пропустить",
        "onboarding_back": "Назад",
        "onboarding_next": "Далее",
        "onboarding_finish": "Войти в студию",
        "onboarding_step_indicator": "Шаг %1$d из %2$d",
        "onboarding_slide1_title": "24/7 ТВ-эфир погоды и панель L-Bar",
        "onboarding_slide1_desc": "Откройте для себя круглосуточную метеосеть студийного класса с культовой боковой панелью L-Bar, бегущей строкой штормовых сводок, местными часами и режимом «Картинка в картинке» (PiP).",
        "onboarding_slide1_highlight1": "Прямой эфир 24/7 и режим PiP",
        "onboarding_slide1_highlight2": "Бегущая строка L-Bar и лента на 12 ч",
        "onboarding_slide2_title": "Прогноз на 7 дней и радар Доплера",
        "onboarding_slide2_desc": "Отслеживайте штормы с многослойным радаром RainViewer, ИК-спутником и потоками ветра, а также изучайте 7-дневный прогноз Open-Meteo с температурными шкалами.",
        "onboarding_slide2_highlight1": "Интерактивная 4-слойная карта радара",
        "onboarding_slide2_highlight2": "7-дневные шкалы температур и тренды 24 ч",
        "onboarding_slide3_title": "Студия ИИ-синоптика и защита бесплатной квоты",
        "onboarding_slide3_desc": "Создавайте сводки погоды в 1 касание, советы по одежде и маршруту и слушайте голосового диктора с Gemini ИИ — с режимом Eco Lite, +3 бонусными кредитами, локальным движком за 0 кредитов и BYOK.",
        "onboarding_slide3_highlight1": "Сводка ИИ, голосовой диктор и чат",
        "onboarding_slide3_highlight2": "Eco Lite, бонус-кредиты, локальный ИИ и BYOK",
        "onboarding_slide4_title": "Push-уведомления о штормах и настройка",
        "onboarding_slide4_desc": "Будьте под защитой фонового мониторинга WorkManager и приоритетных push-уведомлений. Выберите единицы измерения и визуальный стиль эфира ниже:",
        "onboarding_slide4_highlight1": "Фоновые штормовые оповещения WorkManager",
        "onboarding_slide4_highlight2": "Полная поддержка TalkBack и 20 языков",
        "settings_onboarding_title": "Интерактивный обзорный тур по студии",
        "settings_onboarding_subtitle": "Повторить 4-шаговый гид по эфиру L-Bar, радару на 7 дней, студии ИИ-синоптика и штормовым оповещениям",
        "settings_onboarding_action": "ЗАПУСТИТЬ ТУР"
    },
    "values-tr": {
        "onboarding_badge": "STÜDYO TANITIM TURU",
        "onboarding_skip": "Turu Atla",
        "onboarding_back": "Geri",
        "onboarding_next": "İleri",
        "onboarding_finish": "Yayın Stüdyosuna Gir",
        "onboarding_step_indicator": "Adım %1$d / %2$d",
        "onboarding_slide1_title": "24/7 Canlı Hava Durumu TV ve L-Bar Bandı",
        "onboarding_slide1_desc": "İkonik L-Bar telemetri kenar çubuğu, kayan son dakika hava durumu bandı, canlı yerel saat ve Resim İçinde Resim (PiP) oynatıcısıyla stüdyo kalitesinde 24/7 yayın deneyimi yaşayın.",
        "onboarding_slide1_highlight1": "24/7 Canlı TV Sahnesi ve PiP Modu",
        "onboarding_slide1_highlight2": "L-Bar Haber Bandı ve 12 Saatlik Şerit",
        "onboarding_slide2_title": "7 Günlük Tahmin ve Canlı Doppler Radarı",
        "onboarding_slide2_desc": "Çok katmanlı RainViewer Doppler radarı, kızılötesi uydu ve rüzgar vektörleriyle fırtınaları izleyin; termal aralık çubuklarıyla 7 günlük Open-Meteo görünümünü inceleyin.",
        "onboarding_slide2_highlight1": "4 Katmanlı Etkileşimli Doppler Radar Haritası",
        "onboarding_slide2_highlight2": "7 Günlük Termal Çubuklar ve 24 Saat Trendi",
        "onboarding_slide3_title": "YZ Meteorolog Masası ve Ücretsiz Kota Koruması",
        "onboarding_slide3_desc": "Gemini YZ ile tek dokunuşla canlı hava brifingi, giyim ve ulaşım tavsiyesi ve sesli spiker anlatımı oluşturun—Eco Lite Modu, günlük +3 ek kredi, 0 kredilik yerel motor ve BYOK desteğiyle.",
        "onboarding_slide3_highlight1": "Canlı YZ Brifingi, Sesli Spiker ve Soru-Cevap",
        "onboarding_slide3_highlight2": "Eco Lite, Ek Krediler, Yerel Motor ve BYOK",
        "onboarding_slide4_title": "Şiddetli Fırtına Bildirimleri ve Stüdyo Kurulumu",
        "onboarding_slide4_desc": "WorkManager arka plan hava durumu izleme ve yüksek öncelikli anlık bildirimlerle güvende kalın. Aşağıdan sıcaklık birimlerini ve yayın görünümünü seçin:",
        "onboarding_slide4_highlight1": "WorkManager Arka Plan Fırtına Uyarıları",
        "onboarding_slide4_highlight2": "Tam TalkBack Erişilebilirliği ve 20 Dil",
        "settings_onboarding_title": "Etkileşimli Stüdyo Tanıtım Turu",
        "settings_onboarding_subtitle": "Canlı TV L-Bar, 7 Günlük Radar, YZ Meteorolog Masası ve Uyarılar için 4 adımlı rehberi tekrar oynat",
        "settings_onboarding_action": "TURU BAŞLAT"
    },
    "values-pl": {
        "onboarding_badge": "PRZEWODNIK PO STUDIU",
        "onboarding_skip": "Pomiń",
        "onboarding_back": "Wstecz",
        "onboarding_next": "Dalej",
        "onboarding_finish": "Wejdź do Studia",
        "onboarding_step_indicator": "Krok %1$d z %2$d",
        "onboarding_slide1_title": "Telewizja Pogodowa 24/7 i Pasek L-Bar",
        "onboarding_slide1_desc": "Odkryj całodobową sieć pogodową klasy studyjnej z kultowym panelem bocznym L-Bar, przewijanym paskiem ostrzeżeń, lokalnym zegarem na żywo i trybem Obraz w Obrazie (PiP).",
        "onboarding_slide1_highlight1": "Scena Live TV 24/7 i Tryb PiP",
        "onboarding_slide1_highlight2": "Pasek Informacyjny L-Bar i Prognoza 12h",
        "onboarding_slide2_title": "Prognoza 7-Dniowa i Radar Dopplerowski",
        "onboarding_slide2_desc": "Śledź burze dzięki wielowarstwowemu radarowi RainViewer, satelicie IR i wektorom wiatru oraz sprawdzaj 7-dniową prognozę Open-Meteo z paskami zakresów temperatur.",
        "onboarding_slide2_highlight1": "4-Warstwowa Mapa Radaru Dopplerowskiego",
        "onboarding_slide2_highlight2": "7-Dniowe Paski Termiczne i Trendy 24h",
        "onboarding_slide3_title": "Studio Meteorologa AI i Ochrona Darmowego Limitu",
        "onboarding_slide3_desc": "Generuj briefingi pogodowe 1 kliknięciem, porady dotyczące ubioru i dojazdów oraz odsłuch lektora z Gemini AI—z trybem Eco Lite, +3 bonusowymi kredytami, silnikiem lokalnym 0 kredytów i BYOK.",
        "onboarding_slide3_highlight1": "Briefing AI na Żywo, Lektor i Czat Q&A",
        "onboarding_slide3_highlight2": "Eco Lite, Bonusowe Kredyty, Silnik Lokalny i BYOK",
        "onboarding_slide4_title": "Powiadomienia Push o Burzach i Ustawienia Studia",
        "onboarding_slide4_desc": "Bądź bezpieczny dzięki monitorowaniu pogody w tle WorkManager i priorytetowym powiadomieniom push. Wybierz poniżej jednostki temperatury i styl obrazu:",
        "onboarding_slide4_highlight1": "Alerty Burzowe w Tle (WorkManager)",
        "onboarding_slide4_highlight2": "Pełna Dostępność TalkBack i 20 Języków",
        "settings_onboarding_title": "Interaktywny Przewodnik po Studiu",
        "settings_onboarding_subtitle": "Odtwórz ponownie 4-etapowy przewodnik po Live TV L-Bar, Radarze 7-Dniowym, Studiu AI i Alertach",
        "settings_onboarding_action": "URUCHOM"
    },
    "values-nl": {
        "onboarding_badge": "STUDIO RONDLEIDING",
        "onboarding_skip": "Overslaan",
        "onboarding_back": "Terug",
        "onboarding_next": "Volgende",
        "onboarding_finish": "Open Broadcast Studio",
        "onboarding_step_indicator": "Stap %1$d van %2$d",
        "onboarding_slide1_title": "24/7 Live Weer-TV & L-Bar Nieuwsticker",
        "onboarding_slide1_desc": "Ervaar een 24/7 weernetwerk van studiokwaliteit met onze iconische L-Bar zijbalk, scrollende noodweerticker, live lokale klok en Beeld-in-Beeld (PiP) speler.",
        "onboarding_slide1_highlight1": "24/7 Live TV-Podium & PiP-Modus",
        "onboarding_slide1_highlight2": "Broadcast L-Bar Ticker & 12-Uurs Strip",
        "onboarding_slide2_title": "7-Daagse Verwachting & Live Dopplerradar",
        "onboarding_slide2_desc": "Volg stormen met meerlaagse RainViewer Dopplerradar, IR-satelliet en windvectoren naast een interactieve 7-daagse Open-Meteo verwachting met temperatuurbalken.",
        "onboarding_slide2_highlight1": "4-Laags Interactieve Dopplerradarkaart",
        "onboarding_slide2_highlight2": "7-Daagse Thermische Balken & 24u-Trends",
        "onboarding_slide3_title": "AI-Meteoroloog Desk & Gratis Quota-Bescherming",
        "onboarding_slide3_desc": "Genereer 1-tik live weerbriefings, kleding- & reisadvies en gesproken nieuws met Gemini AI—met Eco Lite-modus, +3 dagelijkse bonuscredits, 0-credit lokale motor en BYOK.",
        "onboarding_slide3_highlight1": "Live AI-Briefing, Stemnieuwslezer & Q&A",
        "onboarding_slide3_highlight2": "Eco Lite, Bonuscredits, Lokale Motor & BYOK",
        "onboarding_slide4_title": "Noodweer Pushmeldingen & Studio-Instellingen",
        "onboarding_slide4_desc": "Blijf beschermd met WorkManager achtergrondmonitoring en pushmeldingen met hoge prioriteit. Kies hieronder uw temperatuureenheid en visuele uitzendstijl:",
        "onboarding_slide4_highlight1": "WorkManager Achtergrond Stormmeldingen",
        "onboarding_slide4_highlight2": "Volledige TalkBack-Toegankelijkheid & 20 Talen",
        "settings_onboarding_title": "Interactieve Studio Introductietour",
        "settings_onboarding_subtitle": "Bekijk opnieuw de 4-staps rondleiding over Live TV L-Bar, 7-Daagse Radar, AI Desk en Noodweer",
        "settings_onboarding_action": "START TOUR"
    },
    "values-th": {
        "onboarding_badge": "ทัวร์แนะนำสตูดิโอ",
        "onboarding_skip": "ข้ามทัวร์",
        "onboarding_back": "ย้อนกลับ",
        "onboarding_next": "ถัดไป",
        "onboarding_finish": "เข้าสู่สตูดิโอออกอากาศ",
        "onboarding_step_indicator": "ขั้นตอนที่ %1$d จาก %2$d",
        "onboarding_slide1_title": "ทีวีพยากรณ์อากาศสด 24/7 และแถบข่าว L-Bar",
        "onboarding_slide1_desc": "สัมผัสเครือข่ายโทรทัศน์สภาพอากาศ 24/7 ระดับสตูดิโอพร้อมแถบข้าง L-Bar อันเป็นเอกลักษณ์ แถบข่าวพยากรณ์อากาศเลื่อนต่อเนื่อง นาฬิกาท้องถิ่น และโหมดภาพซ้อนภาพ (PiP)",
        "onboarding_slide1_highlight1": "เวทีถ่ายทอดสด 24/7 และโหมด PiP",
        "onboarding_slide1_highlight2": "แถบข่าววิ่ง L-Bar และพยากรณ์ 12 ชม.",
        "onboarding_slide2_title": "พยากรณ์ล่วงหน้า 7 วัน & เรดาร์ดอปเพลอร์สด",
        "onboarding_slide2_desc": "ติดตามพายุด้วยเรดาร์ดอปเพลอร์หลายชั้น RainViewer ดาวเทียมอินฟราเรด และทิศทางลม ควบคู่กับพยากรณ์ Open-Meteo 7 วันพร้อมแถบช่วงอุณหภูมิรายวัน",
        "onboarding_slide2_highlight1": "แผนที่เรดาร์ดอปเพลอร์โต้ตอบ 4 ชั้น",
        "onboarding_slide2_highlight2": "แถบช่วงอุณหภูมิ 7 วัน & แนวโน้ม 24 ชม.",
        "onboarding_slide3_title": "โต๊ะผู้ประกาศ AI & ระบบดูแลโควตาฟรีอัจฉริยะ",
        "onboarding_slide3_desc": "สร้างสรุปสภาพอากาศสดในแตะเดียว คำแนะนำการแต่งกายและการเดินทาง และเสียงอ่านผู้ประกาศด้วย Gemini AI พร้อมโหมด Eco Lite เครดิตฟรี +3 ต่อวัน ระบบวิเคราะห์ในเครื่อง 0 เครดิต และคีย์ BYOK",
        "onboarding_slide3_highlight1": "สรุปข่าว AI สด ผู้ประกาศเสียง & ถาม-ตอบ",
        "onboarding_slide3_highlight2": "Eco Lite เครดิตเสริม ระบบในเครื่อง & BYOK",
        "onboarding_slide4_title": "แจ้งเตือนพายุรุนแรง & ตั้งค่าสตูดิโอ",
        "onboarding_slide4_desc": "ปลอดภัยด้วยการตรวจสอบสภาพอากาศเบื้องหลังผ่าน WorkManager และการแจ้งเตือนความสำคัญสูง เลือกหน่วยอุณหภูมิและสไตล์ภาพออกอากาศที่คุณต้องการด้านล่าง:",
        "onboarding_slide4_highlight1": "แจ้งเตือนพายุรุนแรงเบื้องหลัง (WorkManager)",
        "onboarding_slide4_highlight2": "รองรับ TalkBack เต็มรูปแบบ & 20 ภาษา",
        "settings_onboarding_title": "ทัวร์แนะนำการใช้งานสตูดิโอแบบโต้ตอบ",
        "settings_onboarding_subtitle": "ดูคำแนะนำ 4 ขั้นตอนเกี่ยวกับ Live TV L-Bar เรดาร์ 7 วัน โต๊ะผู้ประกาศ AI และการแจ้งเตือนพายุอีกครั้ง",
        "settings_onboarding_action": "เริ่มทัวร์"
    },
    "values-uk": {
        "onboarding_badge": "ТУР СТУДІЄЮ",
        "onboarding_skip": "Пропустити",
        "onboarding_back": "Назад",
        "onboarding_next": "Далі",
        "onboarding_finish": "Увійти до студії",
        "onboarding_step_indicator": "Крок %1$d із %2$d",
        "onboarding_slide1_title": "24/7 ТБ-ефір погоди та панель L-Bar",
        "onboarding_slide1_desc": "Відчуйте цілодобову метеомережу студійного рівня з культовою бічною панеллю L-Bar, рухомим рядком штормових новин, місцевим годинником і режимом «Картинка в картинці» (PiP).",
        "onboarding_slide1_highlight1": "Прямий ефір 24/7 та режим PiP",
        "onboarding_slide1_highlight2": "Рухомий рядок L-Bar і стрічка на 12 год",
        "onboarding_slide2_title": "Прогноз на 7 днів і радар Доплера",
        "onboarding_slide2_desc": "Відстежуйте шторми за допомогою багатошарового радара RainViewer, ІЧ-супутника та потоків вітру разом із 7-денним прогнозом Open-Meteo з температурними шкалами.",
        "onboarding_slide2_highlight1": "Інтерактивна 4-шарова карта радара",
        "onboarding_slide2_highlight2": "7-денні шкали температур і тренди 24 год",
        "onboarding_slide3_title": "Студія ШІ-синоптика та захист безкоштовної квоти",
        "onboarding_slide3_desc": "Створюйте зведення погоди в 1 дотик, поради щодо одягу й маршруту та слухайте голосового диктора з Gemini ШІ — із режимом Eco Lite, +3 бонусними кредитами, локальним рушієм за 0 кредитів і BYOK.",
        "onboarding_slide3_highlight1": "Зведення ШІ, голосовий диктор і чат",
        "onboarding_slide3_highlight2": "Eco Lite, бонус-кредити, локальний ШІ та BYOK",
        "onboarding_slide4_title": "Push-сповіщення про шторми та налаштування",
        "onboarding_slide4_desc": "Заبارпечте захист завдяки фоновому моніторингу WorkManager та пріоритетним push-сповіщенням. Оберіть одиниці вимірювання та візуальний стиль ефіру нижче:",
        "onboarding_slide4_highlight1": "Фонові штормові сповіщення WorkManager",
        "onboarding_slide4_highlight2": "Повна підтримка TalkBack і 20 мов",
        "settings_onboarding_title": "Інтерактивний оглядовий тур студією",
        "settings_onboarding_subtitle": "Переглянути 4-кроковий гід по ефіру L-Bar, радару на 7 днів, студії ШІ-синоптика та штормових сповіщеннях",
        "settings_onboarding_action": "ЗАПУСТИТИ ТУР"
    }
}


def xml_escape(text: str) -> str:
    return (
        text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("'", "\\'")
    )


def update_localized_strings():
    res_root = os.path.join("app", "src", "main", "res")
    for folder, translations in onboarding_translations.items():
        strings_path = os.path.join(res_root, folder, "strings.xml")
        if not os.path.exists(strings_path):
            continue
        with open(strings_path, "r", encoding="utf-8") as f:
            content = f.read()

        # Remove any existing onboarding block if re-running
        marker = "    <!-- Interactive Broadcast Studio Onboarding Tour -->"
        if marker in content:
            content = content.split(marker)[0].rstrip() + "\n</resources>\n"

        lines = [marker]
        for key, val in translations.items():
            lines.append(f'    <string name="{key}">{xml_escape(val)}</string>')
        block = "\n".join(lines) + "\n</resources>\n"

        updated = content.replace("</resources>", block)
        with open(strings_path, "w", encoding="utf-8") as f:
            f.write(updated)
        print(f"Updated localized onboarding strings in {strings_path}")


def generate_csv_files():
    filename = "store_listing.csv"
    public_filename = os.path.join("public", "store_listing.csv")

    with open(filename, mode="w", encoding="utf-8-sig", newline="") as file:
        writer = csv.writer(file, quoting=csv.QUOTE_MINIMAL)
        writer.writerow(["Language Code", "Language Name", "App Name", "Short Description", "Full Description"])

        for entry in store_listings:
            lang_code = entry["language_code"]
            lang_name = entry["language_name"]
            app_name = entry["app_name"]
            short_desc = entry["short_description"]
            full_desc = entry["full_description"]

            app_len = len(app_name)
            short_len = len(short_desc)
            full_len = len(full_desc)

            print(f"[{lang_code}] AppName: {app_len}/30 | ShortDesc: {short_len}/80 | FullDesc: {full_len}/4000")

            assert app_len <= 30, f"Error: App Name for {lang_code} exceeds 30 characters ({app_len})"
            assert short_len <= 80, f"Error: Short Description for {lang_code} exceeds 80 characters ({short_len})"
            assert full_len <= 4000, f"Error: Full Description for {lang_code} exceeds 4000 characters ({full_len})"

            writer.writerow([lang_code, lang_name, app_name, short_desc, full_desc])

    if os.path.exists("public"):
        shutil.copyfile(filename, public_filename)
        print(f"Synchronized {public_filename}")

    print("\nSUCCESS: store_listing.csv and public/store_listing.csv updated with 20 validated languages!")


if __name__ == "__main__":
    update_localized_strings()
    generate_csv_files()
