# CRM-Geofacing-APP

Autonomiczny, lekki moduł lokalizacyjny dedykowany do automatyzacji procesów rejestracji wizyt handlowych w terenie. Aplikacja działa jako usługa w tle (Background Service) zintegrowana z mapami offline i geofencingiem.

Projekt ściśle współpracuje z głównym systemem **CRM-Android-APP** w celu automatycznego wykrywania obecności u kontrahentów i ułatwienia raportowania.

---

## 🚀 Główne Funkcjonalności

### 1. Monitorowanie Lokalizacji & Geofencing
* **Inteligentny Geofencing**: Zaimplementowany algorytm wykrywania obecności w strefie kontrahenta (promień 1 km) o minimalnym wpływie na zużycie energii akumulatora.
* **Dwell Time Tracker**: System mierzy czas ciągłego przebywania w strefie kontrahenta i wyzwala akcję (np. powiadomienie o wizycie) dopiero po nieprzerwanym pobycie trwającym minimum 5 minut.
* **Powiadomienia push**: Po wejściu do strefy użytkownik otrzymuje powiadomienie, którego kliknięcie natychmiast uruchamia panel wizyty.

### 2. Panel Wizyty Handlowej (VisitPanel)
* **Szybkie raportowanie**: Formularz pozwalający na sprawne wprowadzenie notatek oraz ustaleń poczynionych podczas spotkania.
* **Współdzielenie statusu**: Po zapisaniu uwagi są zapisywane w pamięci i automatycznie synchronizowane z centralą CRM.

### 3. Interaktywna Mapa (OpenStreetMap)
* **OSMDroid Integration**: Pełna mapa działająca bez płatnego API Google Maps z naniesionymi pinami kontrahentów.
* **Twoja Lokalizacja**: Dynamiczne centrowanie mapy na pozycji GPS użytkownika i oznaczanie jego obecności na mapie.
* **Filtrowanie na mapie**: Integracja z wyszukiwarką kontrahentów z podziałem na regiony i kody handlowe.

---

## 🔑 Bezpieczeństwo i Architektura (Single Sign-On)

Aplikacja wykorzystuje zaawansowane mechanizmy systemu Android w celu optymalizacji i bezpieczeństwa:

* **Współdzielenie sesji (SSO)**: Dzięki wspólnemu kluczowi podpisu oraz konfiguracji `sharedUserId="com.ossadkowski.crm.shared"`, aplikacja bezpośrednio odczytuje token zalogowanej sesji z pamięci głównej aplikacji CRM. Użytkownik loguje się tylko raz w głównym programie.
* **Background Processing**: Rejestracja pozycji GPS odbywa się przy pomocy `FusedLocationProviderClient` z interwałami dostosowanymi do oszczędzania baterii.
* **Lokalny Cache**: SharedPreferences do zarządzania pobraną z CRM listą kontrahentów oraz zapisanymi notatkami wizyt offline.
