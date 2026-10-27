# Karta projektu: BusRadar Gdańsk

## 1. Tytuł projektu

BusRadar Gdańsk: punktualność komunikacji miejskiej w czasie rzeczywistym na otwartych danych ZTM w Gdańsku

## 2. Zespół i role

Zespół pracuje w modelu właścicieli obszarów: każdy obszar systemu ma właściciela, który odpowiada za jego projekt, spójność i review kodu, oraz zastępcę. Zadania nie są przypisywane z góry; każdy członek zespołu bierze zadania z tablicy, również z obszarów innych osób. Wspólnym kontraktem między serwisami jest format zdarzeń w Apache Kafka (ADR-001). Szczegóły opisuje dokument „Model pracy zespołu”.

| Uczestnik | Rola | Obszary (właściciel) | Zastępca w obszarach |
|------------------|--------------------|----------------------------------------------|----------------------|
| Jakub Jurkian | Backend Developer (event streaming) / Frontend | **Strumień zdarzeń i czas rzeczywisty:** Apache Kafka (topiki, format zdarzeń, obsługa błędów, dead-letter topic), wykrywanie przejazdów przez przystanki, tracking-service (Redis, SSE). **Frontend:** struktura aplikacji Angular, konwencje, mapa na żywo | Użytkownicy i bezpieczeństwo |
| Kacper Korzekwa | Backend Developer | **Dane i analityka:** stats-service, schemat PostgreSQL (partycje, retencja, Flyway), historia przejazdów, statystyki punktualności, rankingi, typowe opóźnienia | Strumień zdarzeń i czas rzeczywisty; Integracja z ZTM |
| Tomasz Dylik | Team Lead / Backend & Frontend Developer | **Użytkownicy i bezpieczeństwo:** Keycloak, logowanie, autoryzacja w serwisach, trips-service („moje przejazdy”). Prowadzi tablicę zadań i spotkania zespołu | Frontend; Infrastruktura i jakość |
| Marcel Jaroszewicz | DevOps & Backend Developer | **Integracja z ZTM:** ingest-service (pobieranie z API ZTM, deduplikacja, odporność na awarie), import przystanków z GTFS, tryb odtwarzania. **Infrastruktura i jakość:** Docker, CI/CD, serwer 24/7, monitoring, testy E2E | Dane i analityka |

## 3. Cel projektu

Celem projektu jest aplikacja, która na podstawie zbieranych przez całą dobę danych ZTM w Gdańsku pokazuje pasażerowi, jak punktualne są jego codzienne przejazdy: typowe opóźnienie wybranej linii na wybranym przystanku o danej porze i to, jak wypada dzisiejszy kurs na tle normy. Dodatkowo aplikacja udostępnia mapę pojazdów na żywo oraz rankingi najmniej punktualnych linii i przystanków.

Od strony technicznej jest to system przetwarzania strumienia danych oparty na Apache Kafka. Odczyty z API trafiają do topicu `vehicle-positions` raz, a korzystają z nich niezależnie trzy konsumenty: tracking (bieżący stan w Redis, mapa na żywo), detektor przejazdów i zapis surowej historii w PostgreSQL. Awaria jednego konsumenta nie zatrzymuje pozostałych, a po restarcie nadrabia on zaległe zdarzenia.

Opóźnienia pochodzą z pola `delay` w API pozycji pojazdów. Detektor przejazdów sprawdza, czy pojazd jest w promieniu ok. 50 m od przystanku (współrzędne z listy przystanków GTFS), i publikuje zdarzenie przejazdu do topicu `stop-passages`. Z historii tych przejazdów liczone są wszystkie statystyki i „moje przejazdy”, więc nie trzeba odpytywać każdego przystanku osobno. Projekt korzysta wyłącznie z otwartych danych i nie wpływa na pracę komunikacji miejskiej.

## 4. Repozytorium

https://github.com/TomaszDylik/team-project (monorepo: backend, frontend, infrastruktura, dokumentacja)

## 5. Funkcjonalności planowane w semestrze

**Konieczne**

-   Ciągłe pobieranie pozycji pojazdów wraz z bieżącym opóźnieniem z otwartego API ZTM Gdańsk (co ok. 20 s), z deduplikacją odczytów i odpornością na awarie API.

-   Przetwarzanie odczytów przez Apache Kafka: klucz partycji = id pojazdu, osobny topic na odrzucone komunikaty (dead-letter topic).

-   Wykrywanie przejazdów pojazdów przez przystanki i zapis historii w PostgreSQL (partycje dzienne, retencja surowych odczytów 14 dni, agregaty na stałe).

-   Mapa pojazdów na żywo z filtrem linii (Angular, Leaflet, SSE) i oznaczeniem nieaktualnych danych.

-   „Moje przejazdy”: zalogowany użytkownik (Keycloak) zapisuje linię, przystanek i godzinę, a aplikacja pokazuje typowe opóźnienie z ostatnich tygodni (np. „w 80% przypadków do 4 min”) i status dzisiejszego kursu na tle normy.

-   Statystyki punktualności: odsetek kursów punktualnych i średnie opóźnienie per linia, przystanek i godzina.

-   Rankingi najbardziej spóźnionych linii i przystanków w wybranym okresie.

-   Tryb odtwarzania zapisanego dnia: nagrane odczyty są ponownie publikowane do Kafki, więc system działa bez dostępu do API.

-   Wdrożenie na serwer działający całą dobę i zbierający dane od 23.11.2026.

**Opcjonalne**

-   Powiadomienia, gdy dzisiejszy przejazd jest wyraźnie bardziej opóźniony niż zwykle.

-   Widok odjazdów z wybranego przystanku (szacowane odjazdy z API).

-   Mapa miejsc, w których pojazdy najczęściej stoją.

-   Metryki i monitoring (Prometheus, Grafana).

-   Wdrożenie na Kubernetes (k3s), po ustabilizowaniu wdrożenia na Docker Compose.

## 6. Narzędzia i technologie

| Obszar | Narzędzia |
|----------------------|------------------------------------------------------------|
| Backend | Java 21, Spring Boot 4 (Web, Data JPA, Security / OAuth2 Resource Server), Spring for Apache Kafka, PostgreSQL (partycjonowanie tabel), Flyway, Redis |
| Architektura | 4 serwisy: ingest-service, tracking-service (z detektorem przejazdów), stats-service, trips-service; zdarzenia przez Apache Kafka (topiki `vehicle-positions` i `stop-passages`), zapytania przez REST; wspólny moduł z formatem zdarzeń |
| Źródło danych | otwarte dane ZTM w Gdańsku: pozycje pojazdów z opóźnieniem (`gpsPositions`), lista przystanków GTFS (współrzędne), opcjonalnie szacowane odjazdy (`departures`) |
| Frontend | Angular, TypeScript, RxJS, Leaflet (mapa) |
| Uwierzytelnianie | Keycloak (OpenID Connect, Authorization Code + PKCE) |
| Testy | JUnit 5, Mockito, Testcontainers (PostgreSQL, Kafka, Redis), WireMock (zapisane odpowiedzi API), Playwright (E2E) |
| Infrastruktura | Docker, Docker Compose, GitHub Actions (budowanie, testy, obrazy w GitHub Container Registry), serwer VPS |
| Organizacja pracy | model właścicieli obszarów, tablica zadań w GitHub Projects, cotygodniowe spotkania zespołu, code review w pull requestach (CODEOWNERS), decyzje architektoniczne w ADR, Discord |

## 7. Plan pracy i kamienie milowe

| Termin | Kamień milowy |
|----------------------|------------------------------------------------------------|
| do X | Karta, repozytorium i tablica zadań, sprawdzenie licencji danych ZTM, architektura (diagram, ADR-001: format zdarzeń i topiki Kafki), szkielety serwisów, Docker Compose, CI. |
| do X | Walking skeleton: najprostszy przepływ danych (API ZTM → Kafka → Redis → mapa) działający na serwerze. |
| do X | Pobieranie danych przez Kafkę, zapis historii, mapa pojazdów na żywo, logowanie; system uruchomiony na serwerze i zbierający dane. |
| do X | Statystyki punktualności i rankingi, „moje przejazdy”, tryb odtwarzania. |
| do X | Testy integracyjne i E2E, poprawki, wybrane funkcje opcjonalne, dokumentacja (README, OpenAPI). |
| ok. X | Prezentacja końcowa: mapa na żywo, „moje przejazdy” i statystyki z kilku tygodni pomiarów, podsumowanie procesu. |

Zespół spotyka się co tydzień w środę: omawia stan prac, podejmuje decyzje i planuje kolejny tydzień; co dwa tygodnie odbywa się krótka retrospektywa. Szczegóły opisuje dokument „Model pracy zespołu”.
