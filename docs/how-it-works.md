# BusRadar Gdańsk — jak działa aplikacja

## W skrócie

BusRadar co ok. 20 sekund pobiera z otwartego API ZTM pozycje i opóźnienia wszystkich autobusów i tramwajów w Gdańsku. Pokazuje je na mapie na żywo, a z zebranych tygodni danych liczy, jak punktualne są poszczególne linie i przystanki. Pasażer zapisuje swoje codzienne przejazdy i widzi, jakiego opóźnienia zwykle może się spodziewać i jak wypada dzisiejszy kurs na tle normy.

To nie symulacja: system przetwarza prawdziwy strumień danych, a sercem projektu jest potok Kafka, który musi radzić sobie z duplikatami, opóźnionymi odczytami i awariami zewnętrznego API.

## Skąd dane

ZTM w Gdańsku publikuje otwarte dane transportowe: pozycje pojazdów na żywo (z opóźnieniem względem rozkładu), szacowane odjazdy z przystanków i rozkład w formacie GTFS. Przed złożeniem karty trzeba było sprawdzić trzy rzeczy, bo od nich zależy zakres:

-   [x] Jak często odświeżają się pozycje pojazdów i czy API ma limity zapytań.

-   [x] Czy API podaje gotowe opóźnienie względem rozkładu. Jeśli tak, statystyki liczymy z niego, bez własnego liczenia z GTFS.

-   [ ] Na jakiej licencji są dane i jak trzeba podać źródło w aplikacji.

**Sprawdzone 12.10.2026 (ok. 23:26):** API pozycji pojazdów zwraca gotowe opóźnienie każdego pojazdu, więc statystyki liczymy z tego jednego strumienia. Z rozkładu GTFS bierzemy tylko listę przystanków z ich współrzędnymi. Szacowane odjazdy z przystanku są opcjonalne, bo wymagałyby osobnego zapytania dla każdego przystanku.

| Endpoint | Co zwraca | Uwagi |
|--------------------------------------------------|----------------------|----------------------|
| `https://ckan2.multimediagdansk.pl/gpsPositions?v=2` | wszystkie pojazdy: `lat`, `lon`, `speed`, `direction`, linia, kurs, `vehicleId`, **`delay` w sekundach**, czas wygenerowania | odświeżane co ok. 20 s; w nocy 80 pojazdów; **główne źródło danych** |
| `https://ckan2.multimediagdansk.pl/departures?stopId=<id>` | odjazdy z przystanku: czas rozkładowy, szacowany, **`delayInSeconds`**, status `REALTIME` | lista przystanków i ich id w rozkładzie GTFS; tylko funkcja opcjonalna |

W odpowiedziach nie było nagłówków limitów zapytań. Pobieranie co 20 s wystarczy, bo częściej dane się nie zmieniają. Licencję i wymagany sposób podania źródła trzeba jeszcze sprawdzić na portalu otwartych danych Gdańska.

## Przepływ danych

Odczyty z API trafiają raz do topicu `vehicle-positions`, a z niego niezależnie korzystają trzy konsumenty: tracking (mapa na żywo), detektor przejazdów i zapis surowej historii. Detektor zamienia surowe pozycje w przejazdy przez przystanki (topic `stop-passages`), z których liczone są statystyki i „moje przejazdy”. trips-service łączy historię przejazdów z bieżącym opóźnieniem z trackingu.

![przepływ danych · od API ZTM do trzech funkcji](img/data-flow.png)

Każdy konsument czyta topic we własnym tempie: gdy serwis statystyk leży, mapa działa dalej, a statystyki nadrabiają po restarcie. Szczegółowy opis przebiegu zawiera dokument „Przebieg systemu”.

## Przykład: jeden tramwaj o poranku

Z czterech pobranych odczytów tramwaju linii 8 do mapy trafiają dwa: jeden jest duplikatem, a jeden przyszedł za późno. Liczby są przykładowe.

| Czas odczytu (z API) | Pobrany | Odczyt | Co robi system |
|----------------|----------------|------------------|------------------------|
| 07:45:00 | 07:45:05 | przy Bramie Wyżynnej, 18 km/h, delay 300 s | nowy: Kafka, Redis, historia; przejazd przez przystanek z opóźnieniem +5 min |
| 07:45:00 | 07:45:15 | ten sam odczyt jeszcze raz | duplikat (ten sam pojazd i czas): pominięty |
| 07:45:20 | 07:45:25 | 50 m dalej, 0 km/h | nowy: na mapie tramwaj stoi |
| 07:45:10 | 07:45:35 | odczyt sprzed poprzedniego | starszy niż ostatni w Redis: mapa go ignoruje, historia zapisuje |

Opóźnienie bierzemy z pola `delay`. Gdy pojazd znajdzie się w promieniu ok. 50 m od przystanku, detektor przejazdów publikuje do topicu `stop-passages` zdarzenie: linia 8, Brama Wyżynna, 07:45, opóźnienie 300 s, czyli +5 minut. stats-service zapisuje je w tabeli `stop_passages`. Za punktualny uznajemy kurs spóźniony najwyżej o 3 minuty; ten próg zespół ustala sam i opisuje w README.

## Statystyki

Po tygodniu zbierania danych aplikacja odpowiada, które linie są najmniej punktualne. Liczby poniżej są przykładowe, żeby pokazać kształt wyniku.

| Linia | Przejazdy w tygodniu | Punktualne (do 3 min) | Średnie opóźnienie |
|-------------|-------------|-------------|-------------|
| 122 | 960 | 64% | 3,8 min |
| 8 | 1840 | 71% | 2,9 min |
| 199 | 1210 | 82% | 1,6 min |
| 3 | 1525 | 88% | 1,1 min |

Cały ranking to jedno zapytanie z funkcją okna (dokładnie to, o co pytają na rozmowach z SQL):

```sql
SELECT line,
       COUNT(*) AS passages,
       100.0 * COUNT(*) FILTER (WHERE delay_sec <= 180) / COUNT(*) AS punctual_pct,
       AVG(delay_sec) / 60.0 AS avg_delay_min,
       RANK() OVER (ORDER BY AVG(delay_sec) DESC) AS worst_rank
FROM stop_passages
WHERE passed_at >= now() - interval '7 days'
GROUP BY line;
```

To samo zapytanie pogrupowane dodatkowo po godzinie pokazuje, że linia spóźnia się głównie w szczycie, a nie przez cały dzień.

## Moje przejazdy

Pasażer zapisuje przejazd, np. „122, Brama Wyżynna, dni robocze, 7:30”. Aplikacja pokazuje mu dwie rzeczy:

-   **Typowe opóźnienie** z ostatnich 4 tygodni: „zwykle 2–5 min, w 80% przypadków do 4 min”.

-   **Status na dziś**: „dziś +7 min, gorzej niż zwykle”, na podstawie bieżącego opóźnienia pojazdu z tracking-service.

Typowe opóźnienie to percentyle z tej samej tabeli przejazdów:

```sql
SELECT percentile_cont(0.5) WITHIN GROUP (ORDER BY delay_sec) / 60.0 AS median_min,
       percentile_cont(0.8) WITHIN GROUP (ORDER BY delay_sec) / 60.0 AS p80_min
FROM stop_passages
WHERE line = '122'
  AND stop_id = :stopId
  AND extract(isodow FROM passed_at) <= 5
  AND passed_at::time BETWEEN '07:15' AND '07:45'
  AND passed_at >= now() - interval '28 days';
```

trips-service przechowuje zapisane przejazdy użytkownika, a po typowe opóźnienie i bieżący status pyta przez REST stats-service i tracking-service.

## Ile danych i jak je przechowywać

Przy założeniu 400 pojazdów odczytywanych co 20 sekund system dostaje 20 odczytów na sekundę, czyli do ok. 1,7 mln wierszy dziennie. W nocy jeździ ok. 80 pojazdów, więc w praktyce będzie mniej.

-   Surowe odczyty leżą w tabeli partycjonowanej po dniu. Partycje starsze niż 14 dni usuwamy w całości, co jest szybkie i nie obciąża bazy.

-   Przejazdy przez przystanki (`stop_passages`) i agregaty (per linia, przystanek i godzina) zostają na stałe, bo są dużo mniejsze. Z nich liczymy statystyki i „moje przejazdy”.

-   Wybrane dni eksportujemy do pliku na potrzeby trybu odtwarzania i testów.

## Gdy coś pójdzie nie tak

Awaria API lub serwisu nie psuje danych; najwyżej na chwilę pokazujemy starsze pozycje z wyraźnym oznaczeniem.

| Sytuacja | Co robi system | Mechanizm |
|----------------------|----------------------|----------------------|
| API ZTM nie odpowiada | mapa pokazuje ostatnie pozycje z dopiskiem „dane sprzed 2 min” | ponawianie z rosnącym odstępem, circuit breaker |
| Ten sam odczyt przychodzi dwa razy | zapisany raz | unikalny klucz (id pojazdu, czas odczytu) |
| Odczyty przychodzą nie po kolei | mapa nie cofa pojazdu, historia ma wszystko | Redis trzyma tylko najnowszy odczyt pojazdu |
| Serwis statystyk leży przez godzinę | po restarcie nadrabia zaległości | Kafka przechowuje zdarzenia, konsument czyta od zapisanego offsetu |
| API zmienia format danych | błędne odczyty nie blokują reszty | walidacja, ponowienia i osobny topic na odrzucone komunikaty (dead-letter topic) |
| Prezentacja bez internetu | demo działa na nagranym dniu | tryb odtwarzania |

Zdarzenia jednego pojazdu mają klucz partycji = id pojazdu, więc trafiają do konsumenta w kolejności, w jakiej zostały wysłane.

## Kto co trzyma

Każdy element należy do obszaru, którego właściciel go projektuje i robi review zmian. Zadania z każdego obszaru może brać każdy członek zespołu (dokument „Model pracy zespołu”). Wspólną ścieżką jest topic Kafki z odczytami.

| Element | Dane i zadania | Obszar | Właściciel |
|----------------|--------------------------|----------------|----------------|
| ingest-service | pobieranie z API ZTM, wstępna deduplikacja, publikacja do Kafki, import przystanków z GTFS | Integracja z ZTM | Marcel Jaroszewicz |
| tryb odtwarzania | nagrywanie dnia do pliku i ponowna publikacja do Kafki | Integracja z ZTM | Marcel Jaroszewicz |
| potok Kafka | topiki, format zdarzeń (ADR-001), obsługa błędów i dead-letter topic | Strumień zdarzeń i czas rzeczywisty | Jakub Jurkian |
| tracking-service | ostatnie pozycje w Redis, strumień SSE do mapy, bieżące opóźnienie pojazdu, detektor przejazdów (`stop-passages`) | Strumień zdarzeń i czas rzeczywisty | Jakub Jurkian |
| stats-service | surowa historia (14 dni), przejazdy przez przystanki, agregaty, rankingi, typowe opóźnienia | Dane i analityka | Kacper Korzekwa |
| trips-service | zapisane przejazdy użytkownika, łączenie typowego opóźnienia ze statusem na dziś | Użytkownicy i bezpieczeństwo | Tomasz Dylik |
| Keycloak | konta, logowanie, autoryzacja w serwisach | Użytkownicy i bezpieczeństwo | Tomasz Dylik |
| serwer i CI/CD | Docker, wdrożenie 24/7, monitoring, testy E2E | Infrastruktura i jakość | Marcel Jaroszewicz |
| Angular | struktura aplikacji, mapa na żywo, widoki statystyk i „moich przejazdów” | Frontend | Jakub Jurkian |
